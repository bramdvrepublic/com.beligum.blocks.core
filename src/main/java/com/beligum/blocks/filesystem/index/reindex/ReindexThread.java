package com.beligum.blocks.filesystem.index.reindex;

import com.beligum.base.resources.DefaultResourceFilter;
import com.beligum.base.resources.ifaces.ResourceFilter;
import com.beligum.base.resources.ifaces.ResourceIterator;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfClassNode;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.pages.PageRepository;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.github.dexecutor.core.DefaultDexecutor;
import com.github.dexecutor.core.DexecutorConfig;
import com.github.dexecutor.core.ExecutionConfig;
import com.github.dexecutor.core.support.ThreadPoolUtil;
import com.github.dexecutor.core.task.Task;
import com.github.dexecutor.core.task.TaskProvider;
import com.google.common.util.concurrent.UncheckedExecutionException;
import net.sf.ehcache.concurrent.Sync;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is a single thread that will be responsible for a reindexation job.
 * <p>
 * It's organized like this:
 * <p>
 * - do a full multithreaded scan of the filesystem, taking the supplied filters of the constructor into account
 * and create a database of files (pages) that need to be reindexed while calculating and storing all extra metadata
 * that will be needed to actually reindex the files (like page type, etc).
 * <p>
 * - after building that database, build a dependency graph on all encountered page types (following their dependencies down the line)
 * and prioritize the types that needs to be reindexed first (because others depend on them).
 * <p>
 * - execute the reindexing of the types in the prioritized dependency graph, reading in all pages from the database for that type and
 * do it in a multithreaded manner using the dexecutor framework.
 * <p>
 * Created by bram on 15/04/17.
 */
public class ReindexThread extends Thread
{
    //-----CONSTANTS-----
    public interface Listener
    {
        void reindexingStarted();

        void reindexingEnded();
    }

    //this is the folder that will hold all temp files for all reindexing taksks
    private static final String TEMP_FOLDER_NAME = "reindex";
    private static final String PAGE_TABLE_NAME = "page";
    private static final String PAGE_COLUMN_ID_NAME = "id";
    private static final String PAGE_COLUMN_TYPE_NAME = "rdfClassCurie";
    private static final String PAGE_COLUMN_URI_NAME = "absPageUri";
    private static final String PAGE_COLUMN_STAMP_NAME = "stamp";

    private static final int MAX_TASKS_PER_TX = 100;

    //-----VARIABLES-----
    private final List<String> folders;
    private final String filter;
    private final int depth;
    private final Listener listener;
    private long startStamp;
    private boolean cancelThread;
    private Path dbFile;
    private String dbConnectionUrl;
    private ResourceRepository pageRepository;

    //-----CONSTRUCTORS-----
    public ReindexThread(final List<String> folders, final String filter, final int depth, final Listener listener) throws IOException
    {
        this.folders = folders;
        this.filter = filter;
        this.depth = depth;
        this.listener = listener;
        this.startStamp = System.currentTimeMillis();
        //reset a possibly active global cancellation
        this.cancelThread = false;

        //build a SQLite database in the temp folder that will hold all files to reindex, ordered by type
        this.dbFile = R.configuration().getContextConfig().getLocalTempDir().resolve(TEMP_FOLDER_NAME).resolve("db_" + this.startStamp + ".db");
        //make sure the parent exists
        Files.createDirectories(this.dbFile.getParent());
        this.dbConnectionUrl = "jdbc:sqlite:" + this.dbFile.toUri();
    }

    @Override
    public void run()
    {
        ExecutorService executorService = null;

        try {
            Logger.info("Launching a new reindexation task.");
            if (this.listener != null) {
                this.listener.reindexingStarted();
            }

            //Note: this is not very kosher, but it works
            this.pageRepository = new PageRepository();

            //iterate over all files and save their details to the temp database
            Logger.info("Iterating the file system to build a local database.");
            long numPages = this.buildDatabase();

            //build and execute the dependency graph, based on the the different rdfClasses of the pages in the database
            Logger.info("Using the generated database to reindex all " + numPages + " pages on this website.");
            executorService = this.buildDependencyGraph();
        }
        catch (Throwable e) {
            Logger.error("Caught exception while executing the reindexation of all pages of this website", e);

            cancelThread = true;
        }
        finally {

            try {
                if (executorService != null) {
                    executorService.shutdown();
                    executorService.awaitTermination(1, TimeUnit.HOURS);
                }
            }
            catch (Exception e) {
                Logger.error("Error while shutting down long-running transaction of page reindexation", e);
            }

            try {
                Files.deleteIfExists(this.dbFile);
            }
            catch (Exception e) {
                Logger.error("Error while deleting the temp reindex database after page reindexation; " + this.dbFile, e);
            }
            finally {
                this.dbFile = null;
            }

            Logger.info("Reindexing " + (cancelThread ? "cancelled" : "completed") + " in " +
                        DurationFormatUtils.formatDuration(System.currentTimeMillis() - startStamp, "H:mm:ss") + " time");

            if (this.listener != null) {
                this.listener.reindexingEnded();
            }
        }
    }

    public long getStartStamp()
    {
        return startStamp;
    }
    public synchronized void cancel()
    {
        this.cancelThread = true;
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private long buildDatabase()
    {
        boolean keepRunning = true;
        ExecutorService taskExecutor = Executors.newFixedThreadPool(ThreadPoolUtil.ioIntesivePoolSize());
        Connection dbConnection = null;

        //keep track of how many pages we encounter
        long pageCounter = 0l;

        try {
            //we can't put the in the try-resources block because we need to wait till the executor finishes
            dbConnection = DriverManager.getConnection(this.dbConnectionUrl);

            //create the table
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE " + PAGE_TABLE_NAME + " " +
                                   "(" + PAGE_COLUMN_ID_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                                   " " + PAGE_COLUMN_URI_NAME + " TEXT NOT NULL," +
                                   " " + PAGE_COLUMN_TYPE_NAME + " TEXT NOT NULL," +
                                   //needed for millisecond precision
                                   " " + PAGE_COLUMN_STAMP_NAME + " TIMESTAMP NOT NULL DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))" +
                                   ")");
            }

            //iterate all configured folders and start up an iterator for every one
            for (String folder : this.folders) {

                Logger.info("Entering folder " + folder);

                ResourceFilter pathFilter = null;
                if (!StringUtils.isEmpty(this.filter)) {
                    pathFilter = new DefaultResourceFilter(this.filter);
                }
                ResourceIterator pageIterator = this.pageRepository.getAll(true, URI.create(folder), pathFilter, this.depth);

                //note: read-only because we won't be changing the page, only the index
                while (pageIterator.hasNext() && keepRunning) {

                    taskExecutor.submit(new InsertTask(dbConnection, pageIterator.next().unwrap(Page.class), pageCounter++));

                    //this is a chance to cut-short the IO iteration
                    keepRunning = keepRunning && !this.cancelThread;
                    if (!keepRunning) {
                        Logger.info("Stopped creating database because it was cut short");
                    }
                }

                //also break the folder loop if we have been cancelled
                if (!keepRunning) {
                    break;
                }
            }
        }
        catch (Throwable e) {
            Logger.error("Error while creating database", e);
        }
        finally {
            try {
                taskExecutor.shutdown();
                taskExecutor.awaitTermination(1, TimeUnit.HOURS);
            }
            catch (Exception e) {
                Logger.error("Error while shutting down the database executor service", e);
            }

            if (dbConnection != null) {
                try {
                    dbConnection.close();
                }
                catch (Exception e) {
                    Logger.error("Error while closing SQL connection for " + this.dbConnectionUrl, e);
                }
            }
        }

        return pageCounter;
    }
    private ExecutorService buildDependencyGraph() throws SQLException
    {
        //this is the service that will execute the reindexation of the different rdfClasses
        ExecutorService rdfClassExecutor = Executors.newFixedThreadPool(ThreadPoolUtil.ioIntesivePoolSize());

        // Dexecutor is a small framework for (asynchronously) executing tasks that depend on each other (and detect cycles).
        // Here, we use it to create a dependency graph of RDF classes that depend on each other and process the
        // 'deepest' dependencies first, because earlier (more shallow) classes will need their dependencies to
        // be present when they get indexed.
        DexecutorConfig<RdfClassNode, Void> config = new DexecutorConfig<>(rdfClassExecutor, new ReindexTaskProvider());

        //build an executor from the config
        DefaultDexecutor<RdfClassNode, Void> executor = new DefaultDexecutor<>(config);

        //This is a subset of all classes that are publicly accessible. Note that this is not the same as all the classes
        // known to the system (eg. the (internal) City won't end up here because it's 'inherited' from an external ontology).
        //It's the list of classes an end-user can select as the 'type' of a page, meaning no other classes can end up being saved
        // on disk, so if we iterate files, all encountered classes should be in this set.
        //We'll sort them based on internal dependency and first index the ones that won't depend
        // on others down the line.
        Set<RdfClass> indexClasses = new HashSet<>();

        //get the different classes from the database
        try (Connection dbConnection = DriverManager.getConnection(this.dbConnectionUrl);
             Statement stmt = dbConnection.createStatement()) {

            ResultSet resultSet = stmt.executeQuery("SELECT DISTINCT " + PAGE_COLUMN_TYPE_NAME + " FROM " + PAGE_TABLE_NAME + ";");
            while (resultSet.next()) {
                indexClasses.add(RdfFactory.getClassForResourceType(URI.create(resultSet.getString(PAGE_COLUMN_TYPE_NAME))));
            }
        }

        //build the dependency graph, split up by rdfClass
        for (RdfClass rdfClass : indexClasses) {
            RdfClassNode node = RdfClassNode.instance(rdfClass);
            Set<RdfProperty> props = rdfClass.getProperties();

            boolean addedDep = false;
            if (props != null) {
                for (RdfProperty p : props) {
                    if (p.getDataType().getType().equals(RdfClass.Type.CLASS)) {
                        RdfClassNode dep = RdfClassNode.instance(p.getDataType());

                        //link the two together
                        node.addDependency(dep);

                        //this basically means: the indexing of p.getDataType() should finish before the indexing of rdfClass
                        executor.addDependency(dep, node);

                        addedDep = true;
                    }
                }
            }

            //if the class has no dependency on other classes, make sure it gets indexed
            if (!addedDep) {
                executor.addIndependent(node);
            }
        }

        //for debugging: prints out the executor graph
        //                    StringBuilder builder = new StringBuilder();
        //                    executor.print(new LevelOrderTraversar<>(), new StringTraversarAction<>(builder));
        //                    Logger.info(builder.toString());

        //boot the reindexing, rdfClass by rdfClass, parallellizing where possible
        //arg means: signal the execution should end if an exception is thrown in one of the tasks
        executor.execute(ExecutionConfig.TERMINATING);

        return rdfClassExecutor;
    }

    //-----INNER CLASSES-----
    private class InsertTask implements Runnable
    {
        //-----CONSTANTS-----

        //-----VARIABLES-----
        private Connection dbConnection;
        private Page page;
        private long pageCounter;

        //-----CONSTRUCTORS-----
        public InsertTask(Connection dbConnection, Page page, long pageCounter)
        {
            this.dbConnection = dbConnection;
            this.page = page;
            this.pageCounter = pageCounter;
        }

        //-----PUBLIC METHODS-----
        @Override
        public void run()
        {
            //one last check in the launched thread to make sure we can continue
            if (!cancelThread) {

                try (Statement statement = dbConnection.createStatement()) {

                    //note that this will read and analyze the html from disk, but it's slightly optimized to only read the necessary first line,
                    //so it should be quite fast.
                    //Watch out: the analyzer will read the normalized file, but we assume it might be broken or missing until we reindexed the page,
                    //           so we force an analysis of the original html.
                    statement.executeUpdate("INSERT INTO " + PAGE_TABLE_NAME + "(" + PAGE_COLUMN_URI_NAME + ", " + PAGE_COLUMN_TYPE_NAME + ")" +
                                            " VALUES ('" + page.getPublicAbsoluteAddress() + "', '" + page.createAnalyzer(true).getHtmlTypeof().value + "');");
                }
                catch (Throwable e) {
                    cancelThread = true;
                    Logger.error("Error while executing database insert for " + page.getPublicAbsoluteAddress(), e);
                }
            }
        }

        //-----PROTECTED METHODS-----

        //-----PRIVATE METHODS-----

    }

    private class ReindexTaskProvider implements TaskProvider<RdfClassNode, Void>
    {
        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        public ReindexTaskProvider()
        {
        }

        //-----PUBLIC METHODS-----
        @Override
        public Task<RdfClassNode, Void> provideTask(RdfClassNode rdfClassNode)
        {
            return new ReindexRdfClassTask(rdfClassNode);
        }

        //-----PROTECTED METHODS-----

        //-----PRIVATE METHODS-----

    }

    /**
     * This task is responsible for loading all the pages of a particular rdfClass from the DB,
     * and reindexing them all.
     */
    private class ReindexRdfClassTask extends Task<RdfClassNode, Void> implements TX.Listener
    {
        //-----CONSTANTS-----

        //-----VARIABLES-----
        private final RdfClass rdfClass;

        //-----CONSTRUCTORS-----
        public ReindexRdfClassTask(RdfClassNode rdfClassNode)
        {
            this.rdfClass = rdfClassNode.getRdfClass();
        }

        //-----PUBLIC METHODS-----
        @Override
        public Void execute()
        {
            if (!cancelThread) {
                //this is the executor that will parallellize the reindexation of all members in this rdfClass
                ExecutorService reindexExecutor = Executors.newFixedThreadPool(ThreadPoolUtil.ioIntesivePoolSize());

                Connection dbConnection = null;
                TX transaction = null;

                try {
                    //instance a transaction that's connected to this thread
                    transaction = StorageFactory.createCurrentThreadTx(this, Sync.ONE_DAY);

                    //We'll group the two index connections, connected to the new transaction, together into one option
                    // that will get passed to the reindex() method to re-use our general transaction
                    //Note that the connections get released when the transaction is released (see below)
                    ResourceRepository.IndexOption indexConnectionsOption = new PageRepository.PageIndexConnectionOption(StorageFactory.getMainPageIndexer().connect(transaction),
                                                                                                                         StorageFactory.getTriplestoreIndexer().connect(transaction));

                    dbConnection = DriverManager.getConnection(dbConnectionUrl);

                    try (Statement stmt = dbConnection.createStatement()) {

                        ResultSet resultSet = stmt.executeQuery("SELECT " + PAGE_COLUMN_URI_NAME + " FROM " + PAGE_TABLE_NAME +
                                                                " WHERE " + PAGE_COLUMN_TYPE_NAME + "='" + rdfClass.getCurieName() + "';");

                        int i = 0;
                        while (resultSet.next()) {
                            //                            //augment the counter and restart the transaction if needed
                            //                            if (++i >= MAX_TASKS_PER_TX) {
                            //                                this.endTransaction();
                            //                                this.startTransaction();
                            //                                i = 0;
                            //                            }

                            if (cancelThread) {
                                break;
                            }

                            reindexExecutor.submit(new ReindexTask(URI.create(resultSet.getString(PAGE_COLUMN_URI_NAME)), indexConnectionsOption));
                        }
                    }
                }
                catch (Throwable e) {
                    try {
                        transaction.setRollbackOnly();
                    }
                    catch (Exception e1) {
                        Logger.error("Internal error while rolling back reindexation transaction for RDF class " + rdfClass + "; this shouldn't happen.", e);
                    }
                    finally {
                        throw new UncheckedExecutionException("Error while reindexing rdfClass " + rdfClass, e);
                    }
                }
                finally {
                    try {
                        if (reindexExecutor != null) {
                            reindexExecutor.shutdown();
                            reindexExecutor.awaitTermination(1, TimeUnit.HOURS);
                        }
                    }
                    catch (Exception e) {
                        Logger.error("Error while shutting down reindexation service for class " + rdfClass, e);
                    }

                    if (dbConnection != null) {
                        try {
                            dbConnection.close();
                        }
                        catch (Exception e) {
                            Logger.error("Error while closing SQL connection for RDF class " + this.rdfClass, e);
                        }
                    }

                    if (transaction != null) {
                        try {
                            transaction.close();
                        }
                        catch (Throwable e) {
                            Logger.error("Error while closing long-running transaction of page reindexation for RDF class " + this.rdfClass, e);
                        }
                    }
                    else {
                        Logger.error("Can't close transaction because it's null for RDF class " + this.rdfClass);
                    }
                }
            }

            return null;
        }
        @Override
        public void transactionTimedOut(TX transaction)
        {
            if (transaction != null) {
                try {
                    //doing this will cut short all future reindexation (no need to continue if all will fail at the end anyway)
                    Logger.error("Closing reindexation transaction because of a timeout event");
                    transaction.close(true);
                }
                catch (Exception e) {
                    Logger.error("Error while closing transaction after a timeout event", e);
                }
            }
        }
        @Override
        public void transactionStatusChanged(TX transaction, int oldStatus, int newStatus)
        {
            //We're not interested in every single status change
            //Logger.info("TX change from " + Decoder.decodeStatus(oldStatus) + " to " + Decoder.decodeStatus(newStatus) + " for TX " + transaction.hashCode());
        }

        //-----PROTECTED METHODS-----

        //-----PRIVATE METHODS-----

    }

    private class ReindexTask implements Runnable
    {
        //-----CONSTANTS-----

        //-----VARIABLES-----
        private final URI pageUri;
        private final ResourceRepository.IndexOption indexConnectionsOption;

        //-----CONSTRUCTORS-----
        public ReindexTask(URI pageUri, ResourceRepository.IndexOption indexConnectionsOption)
        {
            this.pageUri = pageUri;
            this.indexConnectionsOption = indexConnectionsOption;
        }

        //-----PUBLIC METHODS-----
        @Override
        public void run()
        {
            if (!cancelThread) {
                try {
                    //Logger.info("Reindexing " + pageUri);

                    //request the page directly, so we don't go through the resource cache
                    Page page = pageRepository.get(pageRepository.request(pageUri, null));

                    //effectively reindex the page
                    pageRepository.reindex(page, this.indexConnectionsOption);
                }
                catch (Throwable e) {
                    Logger.error("Error while reindexing " + pageUri, e);
                }
            }
        }

        //-----PROTECTED METHODS-----

        //-----PRIVATE METHODS-----

    }
}

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
import java.util.concurrent.*;

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

    //-----VARIABLES-----
    private final List<String> folders;
    private final String filter;
    private final int depth;
    private final Listener listener;
    private long startStamp;
    private boolean cancelThread;
    private Path dbFile;
    private boolean resumed;
    private String dbConnectionUrl;
    private Connection dbConnection;
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

        //build a SQLite database in the temp folder that will hold all files to reindex, ordered by type,
        //Note: we don't add the start stamp to the file name anymore, so we can resume a broken reindex session
        this.dbFile = R.configuration().getContextConfig().getLocalTempDir().resolve(TEMP_FOLDER_NAME).resolve("db_reindex.db");
        //an existing file means we're resuming an old session
        this.resumed = Files.exists(this.dbFile);
        //make sure the parent exists
        Files.createDirectories(this.dbFile.getParent());
        this.dbConnectionUrl = "jdbc:sqlite:" + this.dbFile.toUri();
    }

    @Override
    public void run()
    {
        ExecutorService executorService = null;

        try {
            if (this.resumed) {
                Logger.info("Resuming a reindexation task that was started on " + Files.getLastModifiedTime(this.dbFile));
            }
            else {
                Logger.info("Launching a new reindexation task.");
            }
            if (this.listener != null) {
                this.listener.reindexingStarted();
            }

            //Note: this is not very kosher, but it works
            this.pageRepository = new PageRepository();

            //we'll have one big connection during the entire session
            this.dbConnection = DriverManager.getConnection(this.dbConnectionUrl);

            //check if the page table exists and create it if not
            this.createPageTable();

            //iterate over all files and save their details to the temp database
            long pagesToResume = this.getDatabaseSize();
            if (pagesToResume == 0) {
                long startStamp = System.currentTimeMillis();
                Logger.info("Iterating the file system of all pages to build a local database.");
                long numPages = this.buildDatabase();
                Logger.info("Done building a local database of " + numPages + " pages to reindex in " +
                            DurationFormatUtils.formatDuration(System.currentTimeMillis() - startStamp, "H:mm:ss") + " time");
            }
            else {
                Logger.info("Not iterating the file system because I found an existing database with " + pagesToResume + " pages in it to resume at " + this.dbFile);
            }

            //build and execute the dependency graph, based on the the different rdfClasses of the pages in the database
            Logger.info("Using the generated database to reindex all pages in the generated database.");
            executorService = this.executeDatabaseTasks();
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
                long pageNum = this.getDatabaseSize();

                //all db work is done, we can safely close it now
                this.dbConnection.close();

                if (pageNum == 0) {
                    Logger.info("Cleaning up the database file because it's empty.");
                    Files.deleteIfExists(this.dbFile);
                }
                else {
                    Logger.info("Not deleting the database file because " + pageNum + " pages were left behind.");
                }
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
    private long getDatabaseSize() throws SQLException
    {
        long retVal = 0;

        if (Files.exists(this.dbFile)) {
            try (Statement stmt = dbConnection.createStatement()) {
                retVal = stmt.executeQuery("SELECT COUNT(*) FROM " + PAGE_TABLE_NAME + ";").getLong(1);
            }
        }

        return retVal;
    }
    private void createPageTable() throws SQLException
    {
        //see http://somesimplethings.blogspot.be/2010/03/derby-create-table-if-not-exists.html
        ResultSet rs = dbConnection.getMetaData().getTables(null, null, PAGE_TABLE_NAME, null);
        if (!rs.next()) {
            //create the table
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE " + PAGE_TABLE_NAME + " " +
                                   "(" + PAGE_COLUMN_ID_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                                   " " + PAGE_COLUMN_URI_NAME + " TEXT NOT NULL," +
                                   " " + PAGE_COLUMN_TYPE_NAME + " TEXT NOT NULL," +
                                   //needed for millisecond precision
                                   " " + PAGE_COLUMN_STAMP_NAME + " TIMESTAMP NOT NULL DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))" +
                                   ");");
            }
        }
    }
    private long buildDatabase()
    {
        //the amount of pages after which the transaction is flushed
        final int SQL_FLUSH_NUM = 5000;
        //the amount of pages after which a report is printed
        final int REPORT_NUM = 1000;

        boolean keepRunning = true;

        ThreadPoolExecutor taskExecutor = getBlockingExecutor(ThreadPoolUtil.poolSize(0.3), 10000);

        PreparedStatement preparedStatement = null;

        //keep track of how many pages we encounter
        long pageCounter = 0l;
        long[] processCounter = new long[] { 0l };
        long startStamp = System.currentTimeMillis();

        try {

            //When a connection is created, it is in auto-commit mode.
            // This means that each individual SQL statement is treated as a transaction and is automatically committed right after it is executed.
            //The way to allow two or more statements to be grouped into a transaction is to disable the auto-commit mode.
            dbConnection.setAutoCommit(false);

            //we can't make this a resource-try-catch because it can't be closed until all async tasks are done
            preparedStatement = dbConnection.prepareStatement("INSERT INTO " + PAGE_TABLE_NAME + "(" + PAGE_COLUMN_URI_NAME + ", " + PAGE_COLUMN_TYPE_NAME + ") VALUES (?, ?);");

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

                    //this is a chance to cut-short the IO iteration
                    keepRunning = keepRunning && !this.cancelThread;
                    if (!keepRunning) {
                        Logger.info("Stopped creating database because it was cancelled");
                    }
                    else {
                        Page page = pageIterator.next().unwrap(Page.class);

                        pageCounter++;

                        //we'll launch an async task because the creator of the page analyzer is quite intensive
                        taskExecutor.submit(new InsertTask(dbConnection, preparedStatement, page, pageCounter, processCounter));

                        if (pageCounter % SQL_FLUSH_NUM == 0) {
                            Logger.info("Flushing all SQL inserts to disk.");
                            synchronized (preparedStatement) {
                                preparedStatement.executeBatch();
                                dbConnection.commit();
                            }
                        }

                        if (pageCounter % REPORT_NUM == 0) {
                            double secsPassed = (System.currentTimeMillis() - startStamp) / 1000.0;
                            Logger.info("Report: iterating pages to build the database of files to reindex:\n" +
                                        "\tCurrently at page " + pageCounter + ": " + page + "\n" +
                                        "\tThe executor service has " + taskExecutor.getQueue().size() + " tasks in it's queue.\n" +
                                        "\tMean FS iteration speed is " + (int) (pageCounter / secsPassed) + " pages/sec.\n" +
                                        "\tMean processing speed is " + (int) (processCounter[0] / secsPassed) + " pages/sec."
                            );
                        }
                    }
                }

                //also break the folder loop if we have been cancelled
                if (!keepRunning) {
                    break;
                }
            }
        }
        catch (Throwable e) {
            cancelThread = true;
            Logger.error("Error while creating database", e);
        }
        finally {

            final long timeout = 1;
            final TimeUnit unit = TimeUnit.HOURS;
            Logger.info("Done launching tasks to fill the database of files to reindex;" +
                        " waiting for all remaining tasks (" + taskExecutor.getQueue().size() + ") to terminate" +
                        " (for max " + timeout + " " + unit + ")");

            try {
                taskExecutor.shutdown();
                taskExecutor.awaitTermination(timeout, unit);
            }
            catch (Exception e) {
                Logger.error("Error while shutting down the database executor service", e);
            }

            //commit any pending updates
            try {
                synchronized (preparedStatement) {
                    preparedStatement.executeBatch();
                    dbConnection.commit();
                }
            }
            catch (SQLException e) {
                Logger.error("Error while flushing final SQL transaction for " + this.dbConnectionUrl, e);
            }

            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                }
                catch (SQLException e) {
                    Logger.error("Error while closing SQL prepared statement for " + this.dbConnectionUrl, e);
                }
            }

            if (dbConnection != null) {
                try {
                    //revert back to normal
                    dbConnection.setAutoCommit(true);
                }
                catch (Exception e) {
                    Logger.error("Error while resetting SQL connection for " + this.dbConnectionUrl, e);
                }
            }
        }

        return pageCounter;
    }
    private ExecutorService executeDatabaseTasks() throws SQLException
    {
        //this is the service that will execute the reindexation of the different rdfClasses
        ExecutorService rdfClassExecutor = getBlockingExecutor(ThreadPoolUtil.poolSize(0.3), 1000);

        // Dexecutor is a small framework for (asynchronously) executing tasks that depend on each other (and detect cycles).
        // Here, we use it to create a dependency graph of RDF classes that depend on each other and process the
        // 'deepest' dependencies first, because earlier (more shallow) classes will need their dependencies to
        // be present when they get indexed.
        DexecutorConfig<RdfClassNode, Void> config = new DexecutorConfig<>(rdfClassExecutor, new ReindexTaskProvider());

        //build an executor from the config
        DefaultDexecutor<RdfClassNode, Void> executor = new DefaultDexecutor<>(config);

        //get the different classes from the database
        try (Statement stmt = dbConnection.createStatement()) {

            //This is a subset of all classes that are publicly accessible. Note that this is not the same as all the classes
            // known to the system (eg. the (internal) City won't end up here because it's 'inherited' from an external ontology).
            //It's the list of classes an end-user can select as the 'type' of a page, meaning no other classes can end up being saved
            // on disk, so if we iterate files, all encountered classes should be in this set.
            //We'll sort them based on internal dependency and first index the ones that won't depend
            // on others down the line.
            ResultSet resultSet = stmt.executeQuery("SELECT DISTINCT " + PAGE_COLUMN_TYPE_NAME + " FROM " + PAGE_TABLE_NAME + ";");

            //this will keep track of classes that don't have any dependency to add them after the loop
            Set<RdfClassNode> lonelyClasses = new HashSet<>();
            Set<RdfClassNode> deletedClasses = new HashSet<>();

            while (resultSet.next()) {
                //Note: the type is the literal html string in the typeof="" attribute of a RDFa HTML page,
                //so it needs parsing
                RdfClass rdfClass = RdfFactory.getClassForResourceType(URI.create(resultSet.getString(PAGE_COLUMN_TYPE_NAME)));

                //                Logger.info("Checking deps of class " + rdfClass);

                RdfClassNode node = RdfClassNode.instance(rdfClass);
                //don't add it if a previous dependency added it already
                if (!deletedClasses.contains(node)) {
                    lonelyClasses.add(node);
                }

                Set<RdfProperty> props = rdfClass.getProperties();
                if (props != null) {
                    for (RdfProperty p : props) {
                        //if the datatype of the property is a true class, it's a valid dependency
                        if (p.getDataType().getType().equals(RdfClass.Type.CLASS)) {
                            RdfClassNode dep = RdfClassNode.instance(p.getDataType());

                            //link the two together
                            //                            Logger.info("Adding a dependency: " + p.getDataType() + " should be evaluated before " + rdfClass);
                            node.addDependency(dep);

                            //wipe it from the to-do list because we've added it to the executor now
                            lonelyClasses.remove(dep);
                            deletedClasses.add(dep);

                            //this basically means: the indexing of p.getDataType() should finish before the indexing of rdfClass
                            //or more formally: arg1 should be evaluated before arg2
                            executor.addDependency(dep, node);
                        }
                    }
                }
            }

            //these classes have no dependency on other classes, make sure they get indexed
            for (RdfClassNode node : lonelyClasses) {
                //                Logger.info("Adding an independent dependency for " + node.getRdfClass());
                executor.addIndependent(node);
            }
        }

        //for debugging: prints out the executor graph
        //        StringBuilder builder = new StringBuilder("\n--------------------\n");
        //        executor.print(new LevelOrderTraversar<>(), new StringTraversarAction<>(builder));
        //        Logger.info(builder.toString()+"\n--------------------\n");

        //boot the reindexing, rdfClass by rdfClass, parallellizing where possible
        //arg means: signal the execution should end if an exception is thrown in one of the tasks
        executor.execute(ExecutionConfig.TERMINATING);

        return rdfClassExecutor;
    }
    private ThreadPoolExecutor getBlockingExecutor(int nThreads, int queueSize)
    {
        //we rewrote this one to have a limited and blocking queue instead of a (standard) unbounded queue,
        //because the queue can grow very large
        //see http://stackoverflow.com/questions/2247734/executorservice-standard-way-to-avoid-to-task-queue-getting-too-full
        //return (ThreadPoolExecutor) Executors.newFixedThreadPool(ThreadPoolUtil.ioIntesivePoolSize());
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      //same as Executors.newFixedThreadPool(), not the same as the SO article, hope that's ok.
                                      //Note: should be because coreThreads is the same as maxThreads, right?
                                      0L, TimeUnit.MILLISECONDS,
                                      new ArrayBlockingQueue<>(queueSize, true), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    //-----INNER CLASSES-----
    private class InsertTask implements Runnable
    {
        //-----CONSTANTS-----

        //-----VARIABLES-----
        private Connection dbConnection;
        private PreparedStatement preparedStatement;
        private Page page;
        private long pageCounter;
        private long[] processCounter;

        //-----CONSTRUCTORS-----
        public InsertTask(Connection dbConnection, PreparedStatement preparedStatement, Page page, long pageCounter, long[] processCounter)
        {
            this.dbConnection = dbConnection;
            this.preparedStatement = preparedStatement;
            this.page = page;
            this.pageCounter = pageCounter;
            this.processCounter = processCounter;
        }

        //-----PUBLIC METHODS-----
        @Override
        public void run()
        {
            //one last check in the launched thread to make sure we can continue
            if (!cancelThread) {
                //note that this will read and analyze the html from disk, but it's slightly optimized to only read the necessary first line,
                //so it should be quite fast.
                //Watch out: the analyzer will read the normalized file, but we assume it might be broken or missing until we reindexed the page,
                //           so we force an analysis of the original html.
                try {
                    synchronized (this.preparedStatement) {
                        this.preparedStatement.setString(1, page.getPublicAbsoluteAddress().toString());
                        //Note: this one is quite costly (reading in and anayzing the page HTML)
                        this.preparedStatement.setString(2, page.createAnalyzer(true).getHtmlTypeof().value);
                        //this.preparedStatement.executeUpdate();
                        this.preparedStatement.addBatch();
                    }

                    //                    try (Statement statement = this.dbConnection.createStatement()) {
                    //                        statement.executeUpdate("INSERT INTO " + PAGE_TABLE_NAME + "(" + PAGE_COLUMN_URI_NAME + ", " + PAGE_COLUMN_TYPE_NAME + ")" +
                    //                                                " VALUES ('" + page.getPublicAbsoluteAddress() + "', '" + page.createAnalyzer(true).getHtmlTypeof().value + "');");
                    //                    }
                }
                catch (Throwable e) {
                    cancelThread = true;
                    Logger.error("Error while executing database insert for " + page.getPublicAbsoluteAddress(), e);
                }
                finally {
                    this.processCounter[0]++;
                }
            }
        }

        //-----PROTECTED METHODS-----

        //-----PRIVATE METHODS-----

    }

    /**
     * An implementation that will provide the reindex runnables
     */
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
        private static final int MAX_PAGES_PER_TX = 1000;
        private static final long EXECUTOR_FINISH_TIMEOUT = 1;
        private final TimeUnit EXECUTOR_FINISH_TIMEOUT_UNIT = TimeUnit.HOURS;

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
                ExecutorService reindexExecutor = getBlockingExecutor(ThreadPoolUtil.poolSize(0.1), 1000);

                TX transaction = null;
                long startStamp = System.currentTimeMillis();
                long pageCounter = 0;
                long maxPages = 0;
                boolean success = false;

                try {
                    //instance a transaction that's connected to this thread
                    transaction = StorageFactory.createCurrentThreadTx(this, Sync.ONE_DAY);

                    //We'll group the two index connections, connected to the new transaction, together into one option
                    // that will get passed to the reindex() method to re-use our general transaction
                    //Note that the connections get released when the transaction is released (see below)
                    //Also note this means we'll have a transaction per rdf class
                    ResourceRepository.IndexOption indexConnectionsOption = new PageRepository.PageIndexConnectionOption(StorageFactory.getMainPageIndexer().connect(transaction),
                                                                                                                         StorageFactory.getTriplestoreIndexer().connect(transaction));

                    try (Statement stmt = dbConnection.createStatement()) {

                        //this is the number of results we can expect
                        maxPages = stmt.executeQuery("SELECT COUNT(*) FROM " + PAGE_TABLE_NAME +
                                                     " WHERE " + PAGE_COLUMN_TYPE_NAME + "='" + rdfClass.getCurieName() + "';")
                                       .getLong(1);

                        Logger.info("Launching reindexation of RDF class " + this.rdfClass + " with " + maxPages + " members.");

                        //iterate all URIs of pages in this class
                        ResultSet resultSet = stmt.executeQuery("SELECT " + PAGE_COLUMN_URI_NAME + " FROM " + PAGE_TABLE_NAME +
                                                                " WHERE " + PAGE_COLUMN_TYPE_NAME + "='" + rdfClass.getCurieName() + "';");
                        long txBatchCounter = 0;
                        boolean aborted = false;
                        while (resultSet.next()) {

                            if (cancelThread) {
                                aborted = true;
                                break;
                            }

                            if (txBatchCounter > MAX_PAGES_PER_TX) {
                                Logger.info("Max transaction limit reached for class " + this.rdfClass + " at " + pageCounter + " pages; finishing, committing and booting a new one");

                                //we need to
                                reindexExecutor.shutdown();
                                reindexExecutor.awaitTermination(EXECUTOR_FINISH_TIMEOUT, EXECUTOR_FINISH_TIMEOUT_UNIT);

                                //close the active transaction
                                StorageFactory.releaseCurrentThreadTx(false);
                                txBatchCounter = 0;

                                //start a new transaction
                                transaction = StorageFactory.createCurrentThreadTx(this, Sync.ONE_DAY);
                                indexConnectionsOption = new PageRepository.PageIndexConnectionOption(StorageFactory.getMainPageIndexer().connect(transaction),
                                                                                                      StorageFactory.getTriplestoreIndexer().connect(transaction));
                            }

                            reindexExecutor.submit(new ReindexTask(URI.create(resultSet.getString(PAGE_COLUMN_URI_NAME)), indexConnectionsOption));

                            pageCounter++;
                            txBatchCounter++;
                        }

                        success = !aborted;
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
                    //                    Logger.info("Done launching reindexation of " + pageCounter + " pages of RDF class " + this.rdfClass +
                    //                                " waiting for all remaining tasks to terminate" +
                    //                                " (for max " + timeout + " " + unit + ")");
                    boolean finishError = false;

                    try {
                        if (reindexExecutor != null) {
                            reindexExecutor.shutdown();
                            reindexExecutor.awaitTermination(EXECUTOR_FINISH_TIMEOUT, EXECUTOR_FINISH_TIMEOUT_UNIT);
                        }
                    }
                    catch (Throwable e) {
                        finishError = true;
                        Logger.error("Error while shutting down the database executor service", e);
                    }
                    finally {
                        long timeDiff = System.currentTimeMillis() - startStamp;
                        Logger.info((success ? "Finished" : "Aborted") + " reindexation of RDF class " + this.rdfClass + "\n" +
                                    "\tNumber of pages: " + pageCounter + "\n" +
                                    "\tTotal time: " + DurationFormatUtils.formatDuration(timeDiff, "H:mm:ss") + "\n" +
                                    "\tMean reindexation time: " + (int) (pageCounter / (timeDiff / 1000.0)) + " pages/sec\n");
                    }

                    if (transaction != null) {
                        try {
                            StorageFactory.releaseCurrentThreadTx(false);
                        }
                        catch (Throwable e) {
                            finishError = true;
                            Logger.error("Error while closing long-running transaction of page reindexation for RDF class " + this.rdfClass, e);
                        }
                    }
                    else {
                        Logger.error("Can't close transaction because it's null for RDF class " + this.rdfClass);
                    }

                    if (dbConnection != null) {

                        //If everything worked well, we'll open another connection and wipe all classes
                        if (success && !finishError) {
                            try (Statement stmt = dbConnection.createStatement()) {
                                long deletedPages = stmt.executeUpdate("DELETE FROM " + PAGE_TABLE_NAME +
                                                                       " WHERE " + PAGE_COLUMN_TYPE_NAME + "='" + rdfClass.getCurieName() + "';");
                                if (deletedPages != pageCounter) {
                                    Logger.error("Hmm, the number of deleted (" + deletedPages + ") and processed (" + pageCounter + ") pages don't seem to match for RDF class " + this.rdfClass);
                                }
                            }
                            catch (Exception e) {
                                Logger.error("Error while deleting the reindex tasks from the database after successfully processing all entries of RDF class " + this.rdfClass, e);
                            }
                        }
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
                    //let's signal we should end the processing as soon one error occurs
                    cancelThread = true;
                    Logger.error("Error while reindexing " + pageUri, e);
                }
            }
        }

        //-----PROTECTED METHODS-----

        //-----PRIVATE METHODS-----

    }
}

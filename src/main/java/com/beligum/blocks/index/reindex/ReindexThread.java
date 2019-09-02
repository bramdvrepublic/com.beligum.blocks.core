/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.index.reindex;

import com.beligum.base.resources.DefaultResourceFilter;
import com.beligum.base.resources.ifaces.ResourceFilter;
import com.beligum.base.resources.ifaces.ResourceIterator;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.Request;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfClassNode;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.tx.TX;
import com.beligum.blocks.filesystem.pages.PageRepository;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.github.dexecutor.core.DefaultDexecutor;
import com.github.dexecutor.core.DexecutorConfig;
import com.github.dexecutor.core.ExecutionConfig;
import com.github.dexecutor.core.support.ThreadPoolUtil;
import com.github.dexecutor.core.task.Task;
import com.github.dexecutor.core.task.TaskProvider;
import net.sf.ehcache.concurrent.Sync;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class ReindexThread extends Thread implements LongRunningThread
{
    //-----CONSTANTS-----
    //this is the folder that will hold all temp files for all reindexing tasks
    public static final String TEMP_REINDEX_FOLDER_NAME = "reindex";
    private static final String SQL_TABLE_NAME = "resource";
    private static final String SQL_COLUMN_ID_NAME = "id";
    private static final String SQL_COLUMN_TYPE_NAME = "rdfClassCurie";
    private static final String SQL_COLUMN_URI_NAME = "absUri";
    private static final String SQL_COLUMN_STAMP_NAME = "stamp";
    private static final String TASK_PARAM_DELIM = ";";

    private static final int MAX_NUM_THREADS = Runtime.getRuntime().availableProcessors();

    //-----VARIABLES-----
    private final List<String> folders;
    private final Set<RdfClass> classes;
    private final String filter;
    private final int depth;
    private Class<? extends ReindexTask> reindexTaskClass;
    private final Map<String, String> params;
    private Integer fixedNumThreads;
    private final Listener listener;
    private long startStamp;
    private AtomicBoolean cancelThread;
    private Path dbFile;
    private boolean resumed;
    private String dbConnectionUrl;
    private Connection dbConnection;
    private ResourceRepository repository;

    //-----CONSTRUCTORS-----
    public ReindexThread(final List<String> folders, Set<RdfClass> classes, final String filter, final int depth, ResourceRepository repository, final Class<? extends ReindexTask> reindexTaskClass,
                         final List<String> params, final Integer fixedNumThreads, final Listener listener) throws IOException
    {
        this.folders = folders;
        this.classes = classes;
        this.filter = filter;
        this.depth = depth;
        this.repository = repository;
        this.reindexTaskClass = reindexTaskClass;
        this.params = new HashMap<>();
        if (params != null) {
            for (String p : params) {
                String[] keyVal = p.split(TASK_PARAM_DELIM);
                if (keyVal.length != 2) {
                    throw new IOException("Encountered invalid task parameter; it should have a key/value delimited by '" + TASK_PARAM_DELIM + "' and formatted like this: 'key" + TASK_PARAM_DELIM +
                                          "value'; " + p);
                }
                else {
                    //let's not trim or do any cleanup
                    this.params.put(keyVal[0], keyVal[1]);
                }
            }
        }

        this.fixedNumThreads = fixedNumThreads;
        if (this.fixedNumThreads != null) {
            if (this.fixedNumThreads > MAX_NUM_THREADS) {
                Logger.warn("Requested " + this.fixedNumThreads + " threads, but the maximum is set to " + MAX_NUM_THREADS + ", limiting.");
                this.fixedNumThreads = MAX_NUM_THREADS;
            }

            Logger.info("Capping reindex execution threads to " + this.fixedNumThreads);
        }
        this.listener = listener;
        this.startStamp = System.currentTimeMillis();
        //reset a possibly active global cancellation
        this.cancelThread = new AtomicBoolean(false);

        //build a SQLite database in the temp folder that will hold all files to reindex, ordered by type,
        //Note: we don't add the start stamp to the file name anymore, so we can resume a broken reindex session
        this.dbFile = R.configuration().getContextConfig().getLocalTempDir().resolve(TEMP_REINDEX_FOLDER_NAME).resolve("db_reindex.db");
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

        boolean keepDatabase = false;
        try {
            if (this.resumed) {
                Logger.info("Resuming a reindexation task of " + this.reindexTaskClass.getSimpleName() + " that was started on " + Files.getLastModifiedTime(this.dbFile));
            }
            else {
                Logger.info("Launching a new reindexation task of " + this.reindexTaskClass.getSimpleName() + ".");
            }
            if (this.listener != null) {
                this.listener.longRunningThreadStarted();
            }

            //we'll have one big connection during the entire session
            //Note: solution below is much faster
            //this.dbConnection = DriverManager.getConnection(this.dbConnectionUrl);
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(this.dbConnectionUrl);
            dataSource.setJournalMode("WAL");
            dataSource.getConfig().setBusyTimeout("10000");
            this.dbConnection = dataSource.getConnection();

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

            //once we get here, the entire FS is iterated and we'll keep the database
            keepDatabase = true;

            //build and execute the dependency graph, based on the the different rdfClasses of the pages in the database
            Logger.info("Using the generated database to reindex all pages in the generated database.");
            executorService = this.executeDatabaseTasks();
        }
        catch (Throwable e) {
            Logger.error("Caught exception while executing the reindexation of all pages of this website", e);

            cancelThread.set(true);
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

            long pageNum = -1;
            try {
                pageNum = this.getDatabaseSize();

                //all db work is done, we can safely close it now
                this.dbConnection.close();

                if (pageNum == 0 || !keepDatabase) {
                    Logger.info("Deleting the database file because " + (pageNum == 0 ? "it's empty." : "we didn't iterate all files."));
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

                //boot the task class once more to signal we're all done
                try {
                    this.reindexTaskClass.newInstance().finished(cancelThread.get(), pageNum);
                }
                catch (Exception e) {
                    Logger.error("Error while signalling the task class we're all done", e);
                }
            }

            Logger.info("Reindexing " + (cancelThread.get() ? "cancelled" : "completed") + " in " +
                        DurationFormatUtils.formatDuration(System.currentTimeMillis() - startStamp, "H:mm:ss") + " time");

            if (this.listener != null) {
                this.listener.longRunningThreadEnded();
            }
        }
    }
    @Override
    public long getStartStamp()
    {
        return startStamp;
    }
    @Override
    public synchronized void cancel()
    {
        this.cancelThread.set(true);
        Logger.info("Reindexing cancellation requested, please wait for the last tasks to complete...");
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private long getDatabaseSize() throws SQLException
    {
        long retVal = 0;

        if (Files.exists(this.dbFile)) {
            try (Statement stmt = dbConnection.createStatement()) {
                retVal = stmt.executeQuery("SELECT COUNT(*) FROM " + SQL_TABLE_NAME + ";").getLong(1);
            }
        }

        return retVal;
    }
    private void createPageTable() throws SQLException
    {
        //see http://somesimplethings.blogspot.be/2010/03/derby-create-table-if-not-exists.html
        ResultSet rs = dbConnection.getMetaData().getTables(null, null, SQL_TABLE_NAME, null);
        if (!rs.next()) {
            //create the table
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE " + SQL_TABLE_NAME + " " +
                                   "(" + SQL_COLUMN_ID_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                                   " " + SQL_COLUMN_URI_NAME + " TEXT NOT NULL," +
                                   " " + SQL_COLUMN_TYPE_NAME + " TEXT NOT NULL," +
                                   //needed for millisecond precision
                                   " " + SQL_COLUMN_STAMP_NAME + " TIMESTAMP NOT NULL DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))" +
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

        ThreadPoolExecutor taskExecutor = createBlockingExecutor(ThreadPoolUtil.poolSize(0.5), 1000);

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
            preparedStatement = dbConnection.prepareStatement("INSERT INTO " + SQL_TABLE_NAME + "(" + SQL_COLUMN_URI_NAME + ", " + SQL_COLUMN_TYPE_NAME + ") VALUES (?, ?);");

            //iterate all configured folders and start up an iterator for every one
            for (String folder : this.folders) {

                Logger.info("Entering folder " + folder);

                ResourceFilter pathFilter = null;
                if (!StringUtils.isEmpty(this.filter)) {
                    pathFilter = new DefaultResourceFilter(this.filter);
                }
                ResourceIterator pageIterator = this.repository.getAll(true, URI.create(folder), pathFilter, this.depth);

                //note: read-only because we won't be changing the page, only the index
                while (pageIterator.hasNext() && keepRunning) {

                    //this is a chance to cut-short the IO iteration
                    keepRunning = keepRunning && !this.cancelThread.get();
                    if (!keepRunning) {
                        Logger.info("Stopped creating database because it was cancelled");
                    }
                    else {
                        Page page = pageIterator.next().unwrap(Page.class);

                        pageCounter++;

                        //we'll launch an async task because the creator of the page analyzer is quite intensive
                        taskExecutor.submit(new InsertTask(dbConnection, preparedStatement, page, pageCounter, processCounter));

                        if (pageCounter % SQL_FLUSH_NUM == 0) {
                            Logger.info("Reached SQL statement threshold of " + SQL_FLUSH_NUM + "; flushing all SQL inserts to disk.");
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
            cancelThread.set(true);
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
    private ExecutorService executeDatabaseTasks() throws SQLException, IOException
    {
        //count the number of classes in the db to create the executor
        int numClasses;
        try (Statement stmt = dbConnection.createStatement()) {
            numClasses = stmt.executeQuery("SELECT COUNT(DISTINCT " + SQL_COLUMN_TYPE_NAME + ") FROM " + SQL_TABLE_NAME + ";")
                             .getInt(1);
        }

        //this is the service that will execute the reindexation of the different rdfClasses
        ExecutorService rdfClassExecutor = createBlockingExecutor(ThreadPoolUtil.poolSize(0.3), numClasses);

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
            ResultSet resultSet = stmt.executeQuery("SELECT DISTINCT " + SQL_COLUMN_TYPE_NAME + " FROM " + SQL_TABLE_NAME + ";");

            //this will keep track of classes that don't have any dependency to add them after the loop
            Set<RdfClassNode> allClasses = new HashSet<>();
            Set<RdfClassNode> lonelyClasses = new HashSet<>();
            Set<RdfClassNode> addedClasses = new HashSet<>();

            while (resultSet.next()) {
                //Note: the type is the literal html string in the typeof="" attribute of a RDFa HTML page,
                //so it needs parsing
                URI rdfClassUri = URI.create(resultSet.getString(SQL_COLUMN_TYPE_NAME));
                RdfClass rdfClass = RdfFactory.getClass(rdfClassUri);
                //this is bad. It means we'll encounter a NPE later on because we don't have dependency information,
                //so let's quit now with some additional debug information
                if (rdfClass == null) {
                    throw new IOException("Can't seem to find the RDF class for URI " + rdfClassUri);
                }

                //see if we need to process this class; if the list is empty (no specific class to reindex selected) or it's in there.
                boolean selectClass = true;
                if (this.classes != null && !this.classes.isEmpty()) {
                    selectClass = this.classes.contains(rdfClass);
                }

                RdfClassNode node = RdfClassNode.instance(rdfClass);
                allClasses.add(node);

                if (selectClass) {
                    //                Logger.info("Checking deps of class " + rdfClass);

                    //don't add it if a previous dependency added it already
                    if (!addedClasses.contains(node)) {
                        lonelyClasses.add(node);
                    }

                    Iterable<RdfProperty> props = rdfClass.getProperties();
                    if (props != null) {
                        for (RdfProperty p : props) {

                            //this happens sometimes when we introduce a static RDF variable bug, but the NPE log doesn't help us much
                            if (p.getDataType() == null) {
                                Logger.error("Encountered a null-valued datatype for RDF property " + p + " of class " + rdfClass + "; this is just debug info, it will lead to a crash...");
                            }

                            //if the datatype of the property is a true class, it's a valid dependency
                            if (p.getDataType().getType().equals(RdfClass.Type.CLASS)) {
                                RdfClassNode dep = RdfClassNode.instance(p.getDataType());

                                //link the two together
                                //                            Logger.info("Adding a dependency: " + p.getDataType() + " should be evaluated before " + rdfClass);
                                node.addDependency(dep);

                                //wipe both sides from the to-do list because we've added it to the executor now
                                lonelyClasses.remove(dep);
                                lonelyClasses.remove(node);
                                addedClasses.add(dep);
                                addedClasses.add(node);

                                //this basically means: the indexing of p.getDataType() should finish before the indexing of rdfClass
                                //or more formally: arg1 should be evaluated before arg2
                                executor.addDependency(dep, node);
                            }
                        }
                    }
                }
                else {
                    Logger.info("Skipping first-level processing of this class because we have an explicit class list and it's not selected; " + rdfClass);
                }
            }

            //these classes have no dependency on other classes, make sure they get indexed
            for (RdfClassNode node : lonelyClasses) {
                //                Logger.info("Adding an independent dependency for " + node.getRdfClass());
                executor.addIndependent(node);
            }

            for (RdfClassNode rdfClass : allClasses) {
                //if this class was never used, delete it from the database cause it's won't be processed because of an explicit type selection
                if (!addedClasses.contains(rdfClass) && !lonelyClasses.contains(rdfClass)) {
                    stmt.executeUpdate("DELETE FROM " + SQL_TABLE_NAME +
                                       " WHERE " + SQL_COLUMN_TYPE_NAME + "='" + rdfClass.getRdfClass().getCurie() + "';");
                }
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
    private ThreadPoolExecutor createBlockingExecutor(int nThreads, int queueSize)
    {
        //we rewrote this one to have a limited and blocking queue instead of a (standard) unbounded queue,
        //because the queue can grow very large
        //see http://stackoverflow.com/questions/2247734/executorservice-standard-way-to-avoid-to-task-queue-getting-too-full
        //return (ThreadPoolExecutor) Executors.newFixedThreadPool(ThreadPoolUtil.ioIntesivePoolSize());

        //        return new ThreadPoolExecutor(nThreads, nThreads,
        //                                      //same as Executors.newFixedThreadPool(), not the same as the SO article, hope that's ok.
        //                                      //Note: should be because coreThreads is the same as maxThreads, right?
        //                                      0L, TimeUnit.MILLISECONDS,
        //                                      new ArrayBlockingQueue<>(queueSize, true), new ThreadPoolExecutor.CallerRunsPolicy());

        //the above code doesn't block when the queue is full, but runs the code in the thread of the submitter instead,
        //the implementation below works as expected.
        return new BoundedExecutor(this.fixedNumThreads == null ? nThreads : this.fixedNumThreads, queueSize);
    }

    //-----INNER CLASSES-----

    /**
     * This is an executor that blocks on submission of tasks (instead of using an unbounded queue or instead of executing the task in the calling thread)
     * <p>
     * See http://stackoverflow.com/questions/2001086/how-to-make-threadpoolexecutors-submit-method-block-if-it-is-saturated
     * and read the comments for the difference with a new ThreadPoolExecutor.CallerRunsPolicy()
     * and read the comments of the accepted post for more detail
     */
    private class BoundedExecutor extends ThreadPoolExecutor
    {
        private final Semaphore semaphore;

        public BoundedExecutor(int nThreads, int queueSize)
        {
            super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

            //read the comments of the SO post for details on this
            this.semaphore = new Semaphore(nThreads + queueSize);
        }

        @Override
        public Future<?> submit(final Runnable command) throws RejectedExecutionException
        {
            Future<?> retVal = null;

            try {
                semaphore.acquire();

                super.submit(command);
            }
            catch (Throwable e) {

                Logger.error("Error while submitting task to bounded executor", e);
                cancelThread.set(true);

                throw new RejectedExecutionException(e);
            }
            finally {
                semaphore.release();
            }

            return retVal;
        }
    }

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
            if (!cancelThread.get()) {
                //note that this will read and analyze the html from disk, but it's slightly optimized to only read the necessary first line,
                //so it should be quite fast.
                //Watch out: the analyzer will read the normalized file, but we assume it might be broken or missing until we reindexed the page,
                //           so we force an analysis of the original html.
                try {
                    synchronized (this.preparedStatement) {
                        //this is the URI that will be used to lookup the page (using the regular lookup methods)
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
                    cancelThread.set(true);
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
        //if this grows large (eg 1000), it's a source of "Too many open files" exceptions
        private static final int MAX_PAGES_PER_TX = 100;
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
            if (!cancelThread.get()) {
                //this is the executor that will parallellize the reindexation of all members in this rdfClass
                //Note: we can't make the queue size too large or we'll run into a "Too many open files" exception
                int nThreads = ThreadPoolUtil.poolSize(0.9);
                int queueSize = 100;
                ExecutorService reindexExecutor = createBlockingExecutor(nThreads, queueSize);

                //TX transaction = null;
                Request requestContext = null;
                long startStamp = System.currentTimeMillis();
                long taskCounter = 0;
                long[] reindexCounter = new long[] { 0l };
                long maxPages = 0;
                boolean success = false;
                List<String> deleteQueries = Collections.synchronizedList(new LinkedList<>());

                try {
                    //instance a fake request context that's connected to this thread
                    // note: we'll do in-between flushes, so 1 hour should be sufficient
                    requestContext = R.requestManager().createSimulatedRequest(Sync.ONE_HOUR);

                    //We'll group the two index connections, connected to the new transaction, together into one option
                    // that will get passed to the reindex() method to re-use our general transaction
                    //Note that the connections get released when the transaction is released (see below)
                    //Also note this means we'll have a transaction per rdf class
                    ResourceRepository.IndexOption indexConnectionsOption = new PageRepository.IndexConnectionOption(StorageFactory.getJsonIndexer().connect(true),
                                                                                                                     StorageFactory.getSparqlIndexer().connect(true));

                    try (Statement stmt = dbConnection.createStatement()) {

                        //this is the number of results we can expect
                        maxPages = stmt.executeQuery("SELECT COUNT(*) FROM " + SQL_TABLE_NAME +
                                                     " WHERE " + SQL_COLUMN_TYPE_NAME + "='" + rdfClass.getCurie() + "';")
                                       .getLong(1);

                        Logger.info("Launching reindexation of RDF class " + this.rdfClass + " with " + maxPages + " members.");

                        //iterate all URIs of pages in this class
                        ResultSet resultSet = stmt.executeQuery("SELECT " + SQL_COLUMN_ID_NAME + ", " + SQL_COLUMN_URI_NAME + " FROM " + SQL_TABLE_NAME +
                                                                " WHERE " + SQL_COLUMN_TYPE_NAME + "='" + rdfClass.getCurie() + "';");
                        long txBatchCounter = 0;
                        boolean aborted = false;
                        while (resultSet.next()) {

                            if (cancelThread.get()) {
                                aborted = true;
                                reindexExecutor.shutdownNow();
                                break;
                            }

                            if (txBatchCounter > MAX_PAGES_PER_TX) {

                                long timeDiffMillis = System.currentTimeMillis() - startStamp;
                                float pctDone = taskCounter / (float) maxPages;
                                float pctLeft = 1.0f - pctDone;
                                long estimatedMillisLeft = (long) (timeDiffMillis / pctDone * pctLeft);

                                Logger.info("Max transaction limit reached, flushing.\n" +
                                            "Statistics: \n" +
                                            "  - class: " + this.rdfClass + "\n" +
                                            "  - progress: " + taskCounter + "/" + maxPages + " (" + (int) (pctDone * 100) + "%)\n" +
                                            "  - avg. speed: " + (int) (taskCounter / (timeDiffMillis / 1000.0)) + " pages/sec\n" +
                                            "  - time running: " + DurationFormatUtils.formatDuration(timeDiffMillis, "H:mm:ss") + "\n" +
                                            "  - est. time left: " + DurationFormatUtils.formatDuration(estimatedMillisLeft, "H:mm:ss") +
                                            "");

                                reindexExecutor.shutdown();
                                reindexExecutor.awaitTermination(EXECUTOR_FINISH_TIMEOUT, EXECUTOR_FINISH_TIMEOUT_UNIT);
                                //we need to recreate it or the next submit will complain the executor is terminated.
                                reindexExecutor = createBlockingExecutor(nThreads, queueSize);

                                //commit and close the active transaction
                                requestContext.destroy(false);
                                requestContext = null;
                                txBatchCounter = 0;

                                //if all went well, we'll flush the successful entries from the db
                                try (Statement flushStmt = dbConnection.createStatement()) {
                                    //From javadoc: It is imperative that the user manually synchronize on the returned list when iterating over it.
                                    synchronized (deleteQueries) {
                                        for (String sql : deleteQueries) {
                                            flushStmt.addBatch(sql);
                                        }
                                    }
                                    flushStmt.executeBatch();
                                }

                                //start a new transaction
                                requestContext = R.requestManager().createSimulatedRequest(Sync.ONE_HOUR);
                                indexConnectionsOption = new PageRepository.IndexConnectionOption(StorageFactory.getJsonIndexer().connect(true),
                                                                                                  StorageFactory.getSparqlIndexer().connect(true));
                            }

                            //Logger.info("Submitting reindexation of " + publicUri);
                            ReindexTask reindexTask = reindexTaskClass.newInstance();
                            reindexTask.create(URI.create(resultSet.getString(SQL_COLUMN_URI_NAME)), repository, indexConnectionsOption, params, deleteQueries, SQL_TABLE_NAME, SQL_COLUMN_ID_NAME,
                                               resultSet.getLong(SQL_COLUMN_ID_NAME), reindexCounter, cancelThread);
                            reindexExecutor.submit(reindexTask);

                            taskCounter++;
                            txBatchCounter++;
                        }

                        success = !aborted;
                    }
                }
                catch (Throwable e) {
                    throw new RuntimeException("Error while reindexing rdfClass " + rdfClass, e);
                }
                finally {
                    //                    Logger.info("Done launching reindexation of " + pageCounter + " pages of RDF class " + this.rdfClass +
                    //                                " waiting for all remaining tasks to terminate" +
                    //                                " (for max " + timeout + " " + unit + ")");
                    boolean finishError = false;

                    try {
                        if (reindexExecutor != null) {
                            if (success) {
                                reindexExecutor.shutdown();
                            }
                            else {
                                reindexExecutor.shutdownNow();
                            }

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
                                    "\tNumber of pages: " + taskCounter + "\n" +
                                    "\tReindexed pages: " + reindexCounter[0] + "\n" +
                                    "\tTotal time: " + DurationFormatUtils.formatDuration(timeDiff, "H:mm:ss") + "\n" +
                                    "\tMean submission time: " + (int) (taskCounter / (timeDiff / 1000.0)) + " pages/sec\n" +
                                    "\tMean reindexation time: " + (int) (reindexCounter[0] / (timeDiff / 1000.0)) + " pages/sec\n");
                    }

                    if (requestContext != null) {
                        try {
                            requestContext.destroy(!success);
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

                        //If everything worked well, we'll open another connection and execute all remaining statements
                        if (success && !finishError) {

                            try (Statement stmt = dbConnection.createStatement()) {
                                for (String sql : deleteQueries) {
                                    stmt.addBatch(sql);
                                }
                                stmt.executeBatch();
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
}

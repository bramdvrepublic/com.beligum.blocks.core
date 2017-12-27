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

package com.beligum.blocks.filesystem.index.reindex;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.pages.PageRepository;
import net.sf.ehcache.concurrent.Sync;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Options;
import org.sqlite.JDBC;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.beligum.blocks.filesystem.index.reindex.PageExportTask.SQL_TABLE_NAME;

/**
 * Created by bram on 11/05/17.
 */
public class PageImportThread extends Thread implements LongRunningThread, TX.Listener
{
    //-----CONSTANTS-----
    private static final int MAX_PAGES_PER_TX = 100;

    //-----VARIABLES-----
    private long startStamp;
    private AtomicBoolean cancelThread;
    private Path dbFile;
    private Connection dbConnection;
    private final Listener listener;

    //-----CONSTRUCTORS-----
    public PageImportThread(final String dbFilePath, final Listener listener) throws IOException
    {
        this.startStamp = System.currentTimeMillis();
        this.cancelThread = new AtomicBoolean(false);
        this.listener = listener;

        if (StringUtils.isEmpty(dbFilePath)) {
            throw new IOException("No import database file supplied, can't continue.");
        }
        else {
            dbFile = Paths.get(dbFilePath);
            if (!Files.exists(dbFile)) {
                throw new IOException("Can't find import database file, can't continue; " + dbFile);
            }
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public long getStartStamp()
    {
        return this.startStamp;
    }
    @Override
    public void run()
    {
        TX transaction = null;
        boolean success = false;
        List<String> deleteQueries = Collections.synchronizedList(new LinkedList<>());

        try {
            if (this.listener != null) {
                this.listener.longRunningThreadStarted();
            }

            this.dbConnection = DriverManager.getConnection(JDBC.PREFIX + this.dbFile.toUri());
            this.checkPageTable();

            long txBatchCounter = 0;
            PageRepository pageRepository = new PageRepository();
            FileContext fileContext = StorageFactory.getPageStoreFileSystem();
            transaction = StorageFactory.createCurrentThreadTx(this, Sync.ONE_DAY);
            try (Statement stmt = this.dbConnection.createStatement()) {
                ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + SQL_TABLE_NAME + ";");
                while (resultSet.next() && !cancelThread.get()) {

                    if (txBatchCounter > MAX_PAGES_PER_TX) {

                        Logger.info("Flushing transaction...");

                        //close the active transaction
                        StorageFactory.releaseCurrentThreadTx(false);
                        transaction = null;
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
                        transaction = StorageFactory.createCurrentThreadTx(this, Sync.ONE_DAY);
                    }

                    String uriStr = resultSet.getString(PageExportTask.SQL_COLUMN_URI_NAME);
                    String path = resultSet.getString(PageExportTask.SQL_COLUMN_PATH_NAME);

                    Logger.info("Importing "+uriStr);

                    try (OutputStream os = fileContext.create(new org.apache.hadoop.fs.Path(path), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
                        os.write(resultSet.getBytes(PageExportTask.SQL_COLUMN_CONTENT_NAME));
                    }

//                    Source source = new NewPageSource(URI.create(uriStr), new String(resultSet.getBytes(PageExportTask.SQL_COLUMN_CONTENT_NAME), Charsets.UTF_8));
//                    pageRepository.save(source, null, null);

                    deleteQueries.add("DELETE FROM " + SQL_TABLE_NAME +
                                      " WHERE " + PageExportTask.SQL_COLUMN_URI_NAME + "='" + uriStr + "';");
                    txBatchCounter++;
                }
            }

            success = true;
        }
        catch (Throwable e) {
            Logger.error("Caught error while importing pages from backup database " + this.dbFile, e);
            if (transaction != null) {
                try {
                    transaction.setRollbackOnly();
                }
                catch (Exception e1) {
                    Logger.error("Internal error while rolling back page import transaction for " + this.dbFile + "; this shouldn't happen.", e);
                }
                finally {
                    throw new RuntimeException("Error while importing pages from " + this.dbFile, e);
                }
            }
            cancelThread.set(true);
        }
        finally {

            if (transaction != null) {
                try {
                    StorageFactory.releaseCurrentThreadTx(false);
                }
                catch (Throwable e) {
                    Logger.error("Error while closing long-running transaction of page import for" + this.dbFile, e);
                }
            }
            else {
                Logger.error("Can't close transaction because it's null for " + this.dbFile);
            }

            if (this.dbConnection != null) {

                if (success && !cancelThread.get()) {
                    try (Statement stmt = dbConnection.createStatement()) {
                        for (String sql : deleteQueries) {
                            stmt.addBatch(sql);
                        }
                        stmt.executeBatch();
                    }
                    catch (Exception e) {
                        Logger.error("Error while deleting the reindex tasks from the database after successfully processing all entries; " + this.dbFile, e);
                    }
                }

                try {
                    Logger.info("Closing connection with import DB; " + dbFile);
                    this.dbConnection.close();
                    this.dbConnection = null;
                }
                catch (SQLException e) {
                    Logger.error("Error while closing down database connection with " + dbFile, e);
                }
            }

            if (this.listener != null) {
                this.listener.longRunningThreadEnded();
            }
        }

        Logger.info("Importing page database " + (cancelThread.get() ? "cancelled" : "completed") + " in " +
                    DurationFormatUtils.formatDuration(System.currentTimeMillis() - startStamp, "H:mm:ss") + " time");

    }
    @Override
    public void cancel()
    {
        this.cancelThread.set(true);
    }
    @Override
    public void transactionTimedOut(TX transaction)
    {
        if (transaction != null) {
            try {
                //doing this will cut short all future reindexation (no need to continue if all will fail at the end anyway)
                Logger.error("Closing page import transaction because of a timeout event");
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
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void checkPageTable() throws SQLException, IOException
    {
        try (ResultSet rs = this.dbConnection.getMetaData().getTables(null, null, PageExportTask.SQL_TABLE_NAME, null)) {
            if (!rs.next()) {
                throw new IOException("Couldn't find the main table to import from; " + dbFile);
            }
        }
    }
}

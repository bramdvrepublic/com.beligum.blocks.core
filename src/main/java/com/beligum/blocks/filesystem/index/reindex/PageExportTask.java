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

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.hdfs.impl.FileSystems;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 11/05/17.
 */
public class PageExportTask extends ReindexTask
{
    //-----CONSTANTS-----
    private static final String EXPORT_DIR_PARAM = "exportDir";
    public static final String SQL_TABLE_NAME = "resource";
    public static final String SQL_COLUMN_URI_NAME = "uri";
    public static final String SQL_COLUMN_PATH_NAME = "path";
    public static final String SQL_COLUMN_CONTENT_NAME = "content";

    //-----VARIABLES-----
    private static Path dbFile;
    private static Configuration exportFileContextConfig;
    private static FileContext exportFileContext;
    private static Connection dbConnection;
    private static long counter;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    protected void init(Map<String, String> params) throws IOException
    {
        try {
            String pathParam = params.get(EXPORT_DIR_PARAM);
            if (!StringUtils.isEmpty(pathParam)) {
                dbFile = Paths.get(pathParam);
            }
            if (dbFile == null) {
                dbFile = R.configuration().getContextConfig().getLocalTempDir().resolve(ReindexThread.TEMP_REINDEX_FOLDER_NAME).resolve("export.db");
                Logger.info("No export file parameter (" + EXPORT_DIR_PARAM + ") set, exporting to " + dbFile);
            }

            //we always start from scratch
            Files.deleteIfExists(dbFile);
            Files.createDirectories(dbFile.getParent());

            URI exportFsUri = UriBuilder.fromUri(dbFile.toUri()).scheme(FileSystems.SQL.getScheme()).build();
            exportFileContextConfig = HdfsUtils.createHdfsConfig(exportFsUri, null, new HashMap<>());
            exportFileContext = StorageFactory.createFileContext(exportFileContextConfig);

            //            dbConnection = DriverManager.getConnection(JDBC.PREFIX + dbFile.toUri());
            //            this.createPageTable();

            counter = 0;
        }
        catch (Exception e) {
            throw new IOException("Error while initializing the export database; " + dbFile, e);
        }

    }
    @Override
    protected void runTaskFor(Resource resource, ResourceRepository.IndexOption indexConnectionsOption) throws IOException
    {
        try {
            Page page = resource.unwrap(Page.class);
            if (page == null) {
                throw new IOException("Unable to fix this resource, it's not a valid Page; " + resource);
            }

            org.apache.hadoop.fs.Path originalFile = page.getLocalStoragePath();
            if (!page.getFileContext().util().exists(originalFile)) {
                throw new IOException("Original HTML file for this page is missing, can't fix it; " + page.getPublicAbsoluteAddress());
            }

            //Logger.info("Exporting " + page.getLocalStoragePath());
            if (!HdfsUtils.copy(page.getFileContext(), page.getLocalStoragePath(), exportFileContext, page.getLocalStoragePath(), false, true, exportFileContextConfig)) {
                throw new IOException("Error while exporting file; " + page.getUri());
            }

            counter++;

            //            if (!page.getFileContext().util().copy(page.getLocalStoragePath(), snapshotMetaFolder)) {
            //
            //            }

            //            try (PreparedStatement stmt = dbConnection.prepareStatement("INSERT INTO" +
            //                                                                        " " + SQL_TABLE_NAME +
            //                                                                        "(" +
            //                                                                        SQL_COLUMN_URI_NAME + ", " +
            //                                                                        SQL_COLUMN_PATH_NAME + ", " +
            //                                                                        SQL_COLUMN_CONTENT_NAME +
            //                                                                        ")" +
            //                                                                        " VALUES (?, ?, ?)")) {
            //
            //                stmt.setString(1, page.getUri().toString());
            //                stmt.setString(2, page.getLocalStoragePath().toString());
            //
            //                //read everything to a blob buffer
            //                Blob blob = new BlobImpl();
            //                try (InputStream in = page.getFileContext().open(page.getLocalStoragePath());
            //                     OutputStream out = blob.setBinaryStream(1)) {
            //                    IOUtils.copy(in, out);
            //                }
            //                stmt.setBytes(3, blob == null || blob.length() == 0 ? new byte[0] : blob.getBytes(1, (int) blob.length()));
            //
            //                stmt.executeUpdate();
            //            }
        }
        catch (Exception e) {
            throw new IOException("Error while exporting resource " + resource.getUri() + " to " + dbFile, e);
        }
    }
    @Override
    protected void cleanup(boolean success)
    {
        Logger.info("Exported " + counter + " pages to " + dbFile);

//        if (exportFileContext != null) {
//            if (exportFileContext.getDefaultFileSystem() instanceof AutoCloseable) {
//                try {
//                    ((AutoCloseable)exportFileContext.getDefaultFileSystem()).close();
//                }
//                catch (Exception e) {
//                    Logger.error("Error while closing down filesystem with " + dbFile, e);
//                }
//            }
//        }

        if (dbConnection != null) {
            try {
                Logger.info("Closing connection with export DB; " + dbFile);
                dbConnection.close();
                dbConnection = null;
            }
            catch (SQLException e) {
                Logger.error("Error while closing down database connection with " + dbFile, e);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void createPageTable() throws SQLException
    {
        ResultSet rs = dbConnection.getMetaData().getTables(null, null, SQL_TABLE_NAME, null);
        if (!rs.next()) {
            //create the table
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE " + SQL_TABLE_NAME + " " +
                                   "(" +
                                   " " + SQL_COLUMN_URI_NAME + " TEXT PRIMARY KEY NOT NULL," +
                                   " " + SQL_COLUMN_PATH_NAME + " TEXT NOT NULL," +
                                   " " + SQL_COLUMN_CONTENT_NAME + " BLOB NOT NULL" +
                                   ")");
            }
        }
    }
}

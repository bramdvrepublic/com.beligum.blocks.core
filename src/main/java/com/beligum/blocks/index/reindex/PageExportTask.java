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

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.hdfs.impl.FileSystems;
import com.beligum.blocks.filesystem.hdfs.impl.SqlFS;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 11/05/17.
 */
public class PageExportTask extends ReindexTask
{
    //-----CONSTANTS-----
    private static final String EXPORT_DIR_PARAM = "exportDir";

    //-----VARIABLES-----
    private static Path dbFile;
    private static Configuration exportFileContextConfig;
    private static FileContext exportFileContext;
    private static long counter;

    private static org.apache.hadoop.fs.Path lastPath;

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
                dbFile = R.configuration().getContextConfig().getLocalTempDir().resolve(ReindexThread.TEMP_REINDEX_FOLDER_NAME).resolve("export." + SqlFS.DEFAULT_FILENAME_EXT);
                Logger.info("No export file parameter (" + EXPORT_DIR_PARAM + ") set, exporting to " + dbFile);
            }

            //we want to export to a file, not a folder
            if (!dbFile.toString().endsWith("." + SqlFS.DEFAULT_FILENAME_EXT)) {
                dbFile = Paths.get(dbFile.toString() + "." + SqlFS.DEFAULT_FILENAME_EXT);
            }

            //we always start from scratch
            Files.deleteIfExists(dbFile);
            Files.createDirectories(dbFile.getParent());

            URI exportFsUri = UriBuilder.fromUri(dbFile.toUri()).scheme(FileSystems.SQL.getScheme()).build();
            exportFileContextConfig = HdfsUtils.createHdfsConfig(exportFsUri, null, new HashMap<>());
            //we won't be needing transaction support, we want full speed
            exportFileContextConfig.setBoolean(SqlFS.ENABLE_TX_SUPPORT_CONFIG, false);
            exportFileContext = StorageFactory.createFileContext(exportFileContextConfig);

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
                throw new IOException("Unable to export this resource, it's not a valid Page; " + resource);
            }

            org.apache.hadoop.fs.Path originalFile = page.getLocalStoragePath();
            if (!page.getFileContext().util().exists(originalFile)) {
                throw new IOException("Original HTML file for this page is missing, can't export it; " + page.getPublicAbsoluteAddress());
            }

            //Logger.info("Exporting " + page.getLocalStoragePath());
            if (!HdfsUtils.copy(page.getFileContext(), originalFile, exportFileContext, originalFile, false, true, exportFileContextConfig)) {
                throw new IOException("Error while exporting original file of page; " + page.getUri());
            }

            //now do the same for the dot folder
            org.apache.hadoop.fs.Path dotFolder = page.getDotFolder();
            if (page.getFileContext().util().exists(dotFolder)) {
                if (!HdfsUtils.copy(page.getFileContext(), dotFolder, exportFileContext, dotFolder, false, true, exportFileContextConfig)) {
                    throw new IOException("Error while exporting dot folder of page; " + page.getUri());
                }
            }

            lastPath = originalFile;

            counter++;
        }
        catch (Exception e) {
            throw new IOException("Error while exporting resource " + resource.getUri() + " to " + dbFile, e);
        }
    }
    @Override
    protected void cleanup(boolean success)
    {
        Logger.info("Exported " + counter + " pages to " + dbFile + ", last one was " + lastPath);

        //By default, the SqlFs filesystem will keep a number of database connections open,
        //even after all work is done (although, later, we switched to the MiniConnectionPoolManagerWithTimeout
        // implementation that closes idle connections after one minute).
        //This forces the SqlFs to call dispose() on the ConnectionPool, forcing a close down of all idle connection
        //if our task is done.
        if (exportFileContext != null) {
            if (exportFileContext.getDefaultFileSystem() instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) exportFileContext.getDefaultFileSystem()).close();
                }
                catch (Exception e) {
                    Logger.error("Error while closing down filesystem with " + dbFile, e);
                }
            }
        }

        //note: they're static, so we should reset them
        dbFile = null;
        exportFileContextConfig = null;
        exportFileContext = null;
        counter = 0;
        lastPath = null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

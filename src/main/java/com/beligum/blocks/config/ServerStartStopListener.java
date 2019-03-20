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

package com.beligum.blocks.config;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceProducer;
import ch.qos.logback.classic.Level;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.ServerLifecycleListener;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.endpoints.PageAdminEndpoint;
import com.beligum.blocks.filesystem.index.ifaces.Indexer;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.TemplateCache;
import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.LoggerProvider;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileContext;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.Container;

import javax.transaction.TransactionManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Created by bram on 11/10/14.
 */
public class ServerStartStopListener implements ServerLifecycleListener
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public int getStartupPriority()
    {
        return ServerLifecycleListener.PRIORITY_CORE;
    }
    @Override
    public void onServerStarted(Server server, Container container)
    {
        if (Settings.instance().hasBlocksCoreConfig()) {

            //let all .html files pass through our HtmlParser
            R.resourceManager().register(new HtmlParser());

            //the Jericho logger is quite verbose with setRollbackOnly messages when it comes to parsing our Velocity-annotated html tags,
            //so let's disable it's logger in any other level than debug mode
            if (R.configuration().getLogConfig().getLogLevel().isGreaterOrEqual(Level.INFO)) {
                Config.LoggerProvider = LoggerProvider.DISABLED;
            }

            //we might as well pre-load the templates here
            TemplateCache.instance();

            //boot up all the static RDF fields
            RdfFactory.assertInitialized();
        }
    }
    @Override
    public int getShutdownPriority()
    {
        return ServerLifecycleListener.PRIORITY_CORE;
    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        if (Settings.instance().hasBlocksCoreConfig()) {

            //start all possible running async tasks
            PageAdminEndpoint.endAllAsyncTasksNow();

            //don't boot it up if it's not there
            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
                try {
                    StorageFactory.getXADiskTransactionManager().shutdown();
                }
                catch (IOException e) {
                    Logger.error("Exception caught while shutting down XADisk", e);
                }
            }

            Iterator<Indexer> indexIter = StorageFactory.getIndexerRegistry().iterator();
            while (indexIter.hasNext()) {
                Indexer indexer = indexIter.next();
                try {
                    indexer.shutdown();
                }
                catch (IOException e) {
                    Logger.error("Exception caught while shutting down indexer; " + indexer, e);
                }
                indexIter.remove();
            }

            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGEVIEW_FS)) {
                try {
                    FileContext pageViewFs = StorageFactory.getPageViewFileSystem();
                    if (pageViewFs.getDefaultFileSystem() instanceof AutoCloseable) {
                        ((AutoCloseable) pageViewFs.getDefaultFileSystem()).close();
                    }
                }
                catch (Exception e) {
                    Logger.error("Error while shutting down page store filesystem", e);
                }
            }

            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGESTORE_FS)) {
                try {
                    FileContext pageStoreFs = StorageFactory.getPageStoreFileSystem();
                    if (pageStoreFs.getDefaultFileSystem() instanceof AutoCloseable) {
                        ((AutoCloseable) pageStoreFs.getDefaultFileSystem()).close();
                    }
                }
                catch (Exception e) {
                    Logger.error("Error while shutting down page store filesystem", e);
                }
            }

            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRANSACTION_MANAGER)) {
                try {
                    TransactionManager transactionManager = StorageFactory.getTransactionManager();
                    if (transactionManager instanceof BitronixTransactionManager) {
                        BitronixTransactionManager bitronixTransactionManager = (BitronixTransactionManager) transactionManager;

                        //Since manually registered resource pools are left untouched by BitronixTransactionManager.shutdown(),
                        // we need to do this manually
                        XAResourceProducer customProducer = StorageFactory.getBitronixResourceProducer();
                        if (customProducer!=null) {
                            //hope this order is ok
                            customProducer.close();
                            ResourceRegistrar.unregister(customProducer);
                        }

                        bitronixTransactionManager.shutdown();
                    }
                    else {
                        //TODO no public close method on a transaction manager??
                    }
                }
                catch (IOException e) {
                    Logger.error("Error while shutting down transaction manager", e);
                }
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.core.config;

import com.beligum.blocks.core.dbs.BlocksUrlDispatcher;
import com.beligum.blocks.core.mongo.MongoDatabase;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.dynamic.DynamicBlockHandler;
import com.beligum.blocks.core.mongocache.TemplateCache;
import com.beligum.core.framework.base.ifaces.ServerLifecycleListener;
import com.beligum.core.framework.utils.Logger;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.Container;

/**
 * Created by bas on 24.02.15.
 * Class initializing all things necessary at server start up and closing all things necessary at server stop
 */
public class BlocksLifecycleListener implements ServerLifecycleListener
{
    @Override
    public void onServerStarted(Server server, Container container)
    {
        //initialize the Redis-singleton on server start-up
//        RedisDatabase.getInstance();



        //initialize the dynamic block handler before the templates are parsed, so all dynamic blocks are known beforehand
        DynamicBlockHandler.getInstance();
        try {
            BlocksConfig.getInstance().setDatabase(new MongoDatabase());
        } catch (Exception e) {
            Logger.error(e);
        }
//        //initialize template-cache
        try {
            BlocksConfig.getInstance().setTemplateCache(new TemplateCache());
            BlocksConfig.getInstance().getTemplateCache().reset();
        } catch (Exception e) {
            Logger.error(e);
        }

//        //initialize url-id-mapping
        BlocksUrlDispatcher urlDispatcher = null;
        try {
            urlDispatcher = BlocksConfig.getInstance().getDatabase().fetchSiteMap();
        } catch (Exception e) {
            urlDispatcher = BlocksConfig.getInstance().getDatabase().createUrlDispatcher();
        }
        BlocksConfig.getInstance().setUrlDispatcher(urlDispatcher);
//        try {
//            XMLUrlIdMapper.getInstance();
//        }
//        catch (Exception e) {
//            Logger.error("Could not initialize url mapping.", e);
//        }
    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        //close the Redis-singleton on server-end (this destroys the Jedis-pool)
        //TODO: when does server pass here?
        RedisDatabase.getInstance().close();
    }
}

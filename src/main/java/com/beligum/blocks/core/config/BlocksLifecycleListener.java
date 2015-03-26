package com.beligum.blocks.core.config;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.mongo.MongoBlocksFactory;
import com.beligum.blocks.core.mongo.MongoDatabase;
import com.beligum.blocks.core.dbs.redis.RedisDatabase;
import com.beligum.blocks.core.dynamic.DynamicBlockHandler;
import com.beligum.blocks.core.caching.TemplateCache;
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
        Blocks.setFactory(new MongoBlocksFactory());

        try {
            Blocks.setDatabase(new MongoDatabase());
        } catch (Exception e) {
            Logger.error(e);
        }
//        //initialize template-cache
        try {
            Blocks.setTemplateCache(new TemplateCache());
        } catch (Exception e) {
            Logger.error(e);
        }


    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        //close the Redis-singleton on server-end (this destroys the Jedis-pool)
        //TODO: when does server pass here?
        RedisDatabase.getInstance().close();
    }
}

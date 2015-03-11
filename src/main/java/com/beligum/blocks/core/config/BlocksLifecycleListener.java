package com.beligum.blocks.core.config;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.BleuprintsCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.dynamic.DynamicBlockHandler;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.core.framework.base.ifaces.ServerLifecycleListener;
import com.beligum.core.framework.utils.Logger;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletContextEvent;

/**
 * Created by bas on 24.02.15.
 * Class initializing all things necessary at server start up and closing all things necessary at server stop
 */
public class BlocksLifecycleListener implements ServerLifecycleListener
{
    @Override
    public void onServerStarted(Server server, ServletContextEvent event)
    {
        //initialize the Redis-singleton on server start-up
        RedisDatabase.getInstance();

        //initialize the dynamic block handler before the templates are parsed, so all dynamic blocks are known beforehand
        DynamicBlockHandler.getInstance();

        //initialize template-cache
        try {
            BleuprintsCache.getInstance();
            PageTemplateCache.getInstance();
        }
        catch (CacheException e){
            Logger.error("Could not initialize cache.", e);
        }

        //initialize url-id-mapping
        try {
            XMLUrlIdMapper.getInstance();
        }
        catch (Exception e) {
            Logger.error("Could not initialize url mapping.", e);
        }
    }
    @Override
    public void onServerStopped(Server server, ServletContextEvent event)
    {
        //close the Redis-singleton on server-end (this destroys the Jedis-pool)
        //TODO: when does server pass here?
        RedisDatabase.getInstance().close();
    }
}

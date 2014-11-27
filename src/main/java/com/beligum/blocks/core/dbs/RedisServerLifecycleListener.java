package com.beligum.blocks.core.dbs;


import com.beligum.core.framework.base.ifaces.ServerLifecycleListener;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletContextEvent;

/**
 * Created by bas on 29.10.14.
 */
public class RedisServerLifecycleListener implements ServerLifecycleListener
{
    //_____________________IMPLEMENTATION OF ServerLifecycleListener___________//
    @Override
    public void onServerStarted(Server server, ServletContextEvent event)
    {
        //initialize the Redis-singleton on server start-up
        // Redis.getInstance();
    }
    @Override
    public void onServerStopped(Server server, ServletContextEvent event)
    {
        //close the Redis-singleton on server-end (this destroys the Jedis-pool)
        //TODO: when does server pass here?
        // Redis.getInstance().close();
    }
}

package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.core.framework.base.ifaces.ServerLifecycleListener;
import com.beligum.core.framework.utils.Logger;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletContextEvent;

/**
 * Created by bas on 17.02.15.
 */
public class CacheServerLifecycleListener implements ServerLifecycleListener
{
    @Override
    public void onServerStarted(Server server, ServletContextEvent event)
    {
        try {
            EntityTemplateClassCache.getInstance();
            PageTemplateCache.getInstance();
        }
        catch (CacheException e){
            Logger.error("Could not initialize cache.", e);
        }
    }
    @Override
    public void onServerStopped(Server server, ServletContextEvent event)
    {

    }
}

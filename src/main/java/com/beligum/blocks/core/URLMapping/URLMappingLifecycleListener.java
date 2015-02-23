package com.beligum.blocks.core.URLMapping;

import com.beligum.core.framework.base.ifaces.ServerLifecycleListener;
import com.beligum.core.framework.utils.Logger;
import org.eclipse.jetty.server.Server;
import org.xml.sax.SAXException;

import javax.servlet.ServletContextEvent;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Created by bas on 23.02.15.
 */
public class URLMappingLifecycleListener implements ServerLifecycleListener
{
    @Override
    public void onServerStarted(Server server, ServletContextEvent event)
    {
        try {
            XMLMapper.getInstance();
        }
        catch (Exception e) {
            Logger.error("Could not initialize url mapping.", e);
        }
    }
    @Override
    public void onServerStopped(Server server, ServletContextEvent event)
    {

    }
}

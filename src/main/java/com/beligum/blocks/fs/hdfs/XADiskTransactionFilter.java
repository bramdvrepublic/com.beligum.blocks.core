package com.beligum.blocks.fs.hdfs;

import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import org.xadisk.bridge.proxies.interfaces.Session;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Created by bram on 4/20/15.
 */
@Provider
@Priority(Priorities.USER)
public class XADiskTransactionFilter implements ContainerResponseFilter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    //TODO check if this works in case of exceptions
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException
    {
        Session tx = (Session) R.cacheManager().getRequestCache().get(CacheKeys.XADISK_REQUEST_TRANSACTION);
        if (tx != null) {
            try {
                if (responseContext.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                    tx.rollback();
                }
                else {
                    tx.commit();
                }
            }
            catch (Exception e) {
                throw new IOException("Exception caught while processing the file system transaction; this is bad", e);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

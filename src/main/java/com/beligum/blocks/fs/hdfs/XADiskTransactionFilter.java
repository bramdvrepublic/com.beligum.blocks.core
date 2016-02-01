package com.beligum.blocks.fs.hdfs;

import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;

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
        XADiskRequestCacheEntry tx = (XADiskRequestCacheEntry) R.cacheManager().getRequestCache().get(CacheKeys.XADISK_REQUEST_TRANSACTION);
        if (tx != null) {
            try {
                if (tx.xaSession!=null) {
                    if (responseContext.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                        tx.xaSession.rollback();
                    }
                    else {
                        tx.xaSession.commit();
                    }
                }
            }
            catch (Exception e) {
                throw new IOException("Exception caught while processing the file system transaction; this is probably bad", e);
            }
            finally {
                //make sure we only do this once
                R.cacheManager().getRequestCache().remove(CacheKeys.XADISK_REQUEST_TRANSACTION);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

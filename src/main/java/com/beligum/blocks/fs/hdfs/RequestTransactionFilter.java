package com.beligum.blocks.fs.hdfs;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.StorageFactory;

import javax.annotation.Priority;
import javax.transaction.Status;
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
public class RequestTransactionFilter implements ContainerResponseFilter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * For now, this is a bit of a hack.
     * Sometimes we want to call multiple transactional request-operations during a single request (eg. during re-indexing).
     * The transaction system complains when doing this, so calling this method after every 'request-operation', we simulate a real request,
     * but it certainly is not the best approach...
     */
    public static void executeManualRequestCleanup() throws IOException
    {
        doFilter(null, null);
    }
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException
    {
        doFilter(requestContext, responseContext);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static void doFilter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException
    {
        RequestTX tx = (RequestTX) R.cacheManager().getRequestCache().get(CacheKeys.REQUEST_TRANSACTION);
        if (tx != null) {
            try {
                if ((responseContext!=null && responseContext.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) || tx.getStatus() != Status.STATUS_ACTIVE) {
                    tx.rollback();
                }
                else {
                    //this is the general case: try to commit and (try to) rollback on error
                    try {
                        tx.commit();
                    }
                    catch (Exception e) {
                        try {
                            Logger.warn("Caught exception while committing a file system transaction, trying to rollback...", e);
                            tx.rollback();
                        }
                        catch (Exception e1) {
                            //don't wait for the next reboot before trying to revert to a clean state; try it now
                            //note that the reboot method is implemented so that it doesn't throw (another) exception, so we can rely on it's return value quite safely
                            if (!StorageFactory.rebootPageStoreTransactionManager()) {
                                throw new IOException(
                                                "Exception caught while processing a file system transaction and the reboot because of a faulty rollback failed too; this is VERY bad and I don't really know what to do. You should investigate this!",
                                                e1);
                            }
                            else {
                                //we can't just swallow the exception; something's wrong and we should report it to the user
                                throw new IOException(
                                                "I was unable to commit a file system transaction and even the resulting rollback failed, but I did manage to reboot the filesystem. I'm adding the exception below;",
                                                e1);
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                throw new IOException("Exception caught while processing a file system transaction; this is bad", e);
            }
            finally {
                try {
                    tx.close();
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while closing a file system transaction; this is bad", e);
                }
                finally {
                    //make sure we only do this once
                    R.cacheManager().getRequestCache().remove(CacheKeys.REQUEST_TRANSACTION);
                }
            }
        }
    }
}

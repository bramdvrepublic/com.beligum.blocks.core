package com.beligum.blocks.fs.hdfs;

import com.beligum.blocks.config.StorageFactory;

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
public class RequestTransactionFilter implements ContainerResponseFilter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException
    {
        StorageFactory.releaseCurrentRequestTx(responseContext != null && responseContext.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

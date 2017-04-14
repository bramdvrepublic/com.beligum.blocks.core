package com.beligum.blocks.config;

import javax.annotation.Priority;
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
// Note that we want this to be executed as late as possible, hence the very low value.
// If this somehow wouldn't be sufficient (still too early), the only alternative would be to create a plugin system in the JAXRSApplicationEventListener class
@Priority(100)
public class ReleaseFilter implements ContainerResponseFilter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException
    {
        //there's probably something wrong if the responseContext is null, so also roll back in case that happens
        StorageFactory.releaseCurrentRequestTx(responseContext == null || responseContext.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

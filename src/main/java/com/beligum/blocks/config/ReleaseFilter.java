/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
// If this somehow wouldn't be sufficient (still too early), the only alternative would be to instance a plugin system in the JAXRSApplicationEventListener class
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

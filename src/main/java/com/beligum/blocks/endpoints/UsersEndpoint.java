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

package com.beligum.blocks.endpoints;

import com.beligum.base.auth.endpoints.AbstractUsersEndpoint;
import com.beligum.base.auth.models.DefaultSubject;
import com.beligum.base.models.Subject;
import com.beligum.base.routing.ifaces.ReverseRoute;
import gen.com.beligum.blocks.endpoints.ApplicationEndpointRoutes;

import javax.ws.rs.Path;

/**
 * Created by bram on 12/1/15.
 */
@Path(UsersEndpoint.PATH)
public class UsersEndpoint extends AbstractUsersEndpoint
{
    //-----CONSTANTS-----
    protected static final String PATH = "users";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    protected Class<? extends Subject> getSubjectClass()
    {
        return DefaultSubject.class;
    }
    @Override
    protected String getPrefixPath()
    {
        return PATH;
    }
    @Override
    protected ReverseRoute getLoginRedirect()
    {
        return ApplicationEndpointRoutes.getPage("");
    }
    @Override
    protected ReverseRoute getLogoutRedirect()
    {
        return ApplicationEndpointRoutes.getPage("");
    }
    @Override
    protected ReverseRoute getEmailCallbackRedirect()
    {
        return ApplicationEndpointRoutes.getPage("");
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

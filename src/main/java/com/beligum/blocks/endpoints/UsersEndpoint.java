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
import com.beligum.base.models.Person;
import com.beligum.base.models.Principal;
import com.beligum.base.models.Subject;
import com.beligum.base.routing.ifaces.ReverseRoute;
import gen.com.beligum.blocks.endpoints.ApplicationEndpointRoutes;
import gen.com.beligum.blocks.endpoints.UsersEndpointRoutes;

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
    public UsersEndpoint()
    {
        //this is needed for the securityManager
    }

    //-----PUBLIC METHODS-----
    @Override
    public Class<? extends Subject> getSubjectClass()
    {
        return DefaultSubject.class;
    }
    @Override
    public String getPrefixPath()
    {
        return PATH;
    }
    @Override
    public ReverseRoute getLoginRedirect()
    {
        return ApplicationEndpointRoutes.getPage("");
    }
    @Override
    public ReverseRoute getLogoutRedirect()
    {
        return ApplicationEndpointRoutes.getPage("");
    }
    @Override
    public ReverseRoute getEmailCallbackRedirect()
    {
        return ApplicationEndpointRoutes.getPage("");
    }
    @Override
    public ReverseRoute getUserRedirect(long userId)
    {
        //TODO this isn't right (a reverse route of this class?) and doesn't compile (at least sometimes it doesn't)
        // We should implement the possibility for endpoints to have superclasses and adjust the reverse routes generator.
        //return UsersEndpointRoutes.getUser(userId);
        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

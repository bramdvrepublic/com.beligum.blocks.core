package com.beligum.blocks.endpoints;

import com.beligum.base.auth.endpoints.AbstractUsersEndpoint;
import com.beligum.base.auth.models.DefaultSubject;
import com.beligum.base.models.Subject;
import com.beligum.base.routing.ifaces.ReverseRoute;
import gen.com.beligum.blocks.endpoints.ApplicationEndpointRoutes;
import gen.com.beligum.blocks.endpoints.UsersEndpointRoutes;

import javax.ws.rs.Path;

/**
 * Created by bram on 12/1/15.
 */
@Path("users")
public class UsersEndpoint extends AbstractUsersEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    protected Class<? extends Subject> getSubjectClass()
    {
        return DefaultSubject.class;
    }
    @Override
    protected ReverseRoute getLoginEndpoint()
    {
        return UsersEndpointRoutes.getLogin();
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

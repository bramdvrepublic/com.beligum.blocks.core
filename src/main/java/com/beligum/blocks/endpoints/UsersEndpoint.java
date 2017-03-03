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

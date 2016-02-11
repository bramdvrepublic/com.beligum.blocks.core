package com.beligum.blocks.routing;

import com.beligum.blocks.routing.ifaces.Router;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by wouter on 1/06/15.
 */
public abstract class AbstractRouter implements Router
{
    protected Route route;

    public AbstractRouter(Route route)
    {
        this.route = route;
    }

    @Override
    public Response response() throws IOException
    {
        Response retVal;
        // We give an extra chance to find another active page for another language on the same url
        if (!this.route.exists() || this.route.getWebPath().isNotFound()) {
            this.route.getAlternateLocalPath();
        }

        if (!this.route.exists() || this.route.getWebPath().isNotFound()) {
            // If page does not exist, throw error for normal user and allow admin to create a new page
            retVal = newPage();
        }
        // Return ok. Show PageImpl
        else if (this.route.getWebPath().isPage()) {
            retVal = showPage();
        }
        else if (this.route.getWebPath().isRedirect()) {
            retVal = redirect();
        }
        else {
            throw new BadRequestException();
        }

        return retVal;

    }

    public abstract Response newPage() throws IOException;

    public abstract Response showPage() throws IOException;

    public abstract Response redirect();
}

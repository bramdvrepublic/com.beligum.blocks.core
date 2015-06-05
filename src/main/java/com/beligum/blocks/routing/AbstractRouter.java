package com.beligum.blocks.routing;

import com.beligum.blocks.routing.ifaces.Router;
import com.beligum.blocks.security.Permissions;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

/**
 * Created by wouter on 1/06/15.
 */
public abstract class AbstractRouter implements Router
{
    protected Route route;

    public AbstractRouter(Route route) {
        this.route = route;
    }

    @Override
    public Response response() {
        Response retVal;

        if (!this.route.exists() || this.route.getNode().isNotFound()) {
            // If page does not exist, throw error for normal user and allow admin to create a new page
           retVal = newPage();
        }
        // Return ok. Show Page
        else if (this.route.getNode().isPage()) {
            retVal = showPage();
        } else if (this.route.getNode().isRedirect()) {
            retVal = redirect();
        } else {
            throw new BadRequestException();
        }

        return retVal;

    }

    public abstract Response newPage();

    public abstract Response showPage();

    public abstract Response redirect();
}

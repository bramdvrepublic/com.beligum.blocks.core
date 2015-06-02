package com.beligum.blocks.routing;

import com.beligum.blocks.routing.ifaces.Route;
import com.beligum.blocks.routing.ifaces.Router;
import com.beligum.blocks.security.Permissions;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

/**
 * Created by wouter on 1/06/15.
 *
 * Returns a response based on a RouteObject
 *
 */
public class HtmlRouter extends AbstractRouter
{

    public HtmlRouter(Route route) {
        super(route);
    }

    public Response newPage() {

        if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            throw new NotFoundException();
        } else {
            if (!route.exists()) {
                route.create();
            }


        }

        return Response.ok().build();
    }

    public Response showPage() {
        return Response.ok().build();
    }

    public Response redirect() {
        return null;
    }
}

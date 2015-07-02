package com.beligum.blocks.routing.ifaces;

import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by wouter on 1/06/15.
 */
public interface Router
{
    public Response response() throws IOException;
}

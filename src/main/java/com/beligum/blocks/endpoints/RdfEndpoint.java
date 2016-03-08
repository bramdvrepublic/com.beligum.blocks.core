package com.beligum.blocks.endpoints;

import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import com.beligum.blocks.security.Permissions;
import org.apache.lucene.search.BooleanClause;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Created by bram on 2/25/16.
 */
@Path("/blocks/admin/rdf")
public class RdfEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @GET
    @Path("/properties/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getProperties() throws IOException
    {
        return Response.ok(RdfFactory.getProperties()).build();
    }

    @GET
    @Path("/classes/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getClasses() throws IOException
    {
        return Response.ok(RdfFactory.getClasses()).build();
    }

    @GET
    @Path("/resources/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getResources(@QueryParam("resourceTypeCurie") URI resourceTypeCurie, @QueryParam("query") String query) throws IOException
    {
        PageIndexer mainPageIndexer = StorageFactory.getMainPageIndexer();

        PageIndexConnection.FieldQuery[] queries = new PageIndexConnection.FieldQuery[]{ new PageIndexConnection.FieldQuery(PageIndexEntry.Field.typeOf, resourceTypeCurie.toString(), BooleanClause.Occur.FILTER,
                                                                                                                            PageIndexConnection.FieldQuery.Type.EXACT),
                                                                                         /*new PageIndexConnection.FieldQuery(IndexEntry.Field.tokenisedId, query, BooleanClause.Occur.MUST,
                                                                                                                            PageIndexConnection.FieldQuery.Type.WILDCARD),*/
                                                                                         new PageIndexConnection.FieldQuery(PageIndexEntry.Field.title, query, BooleanClause.Occur.MUST,
                                                                                                                            PageIndexConnection.FieldQuery.Type.WILDCARD) };

        List<PageIndexEntry> result = mainPageIndexer.connect().search(queries, 10);

        return Response.ok(result).build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
}

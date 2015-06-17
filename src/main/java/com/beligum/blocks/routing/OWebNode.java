package com.beligum.blocks.routing;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.routing.ifaces.WebNode;
import com.beligum.blocks.routing.ifaces.WebPath;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public class OWebNode implements WebNode
{

    // The vertex we are wrapping
    private Vertex vertex;

    public OWebNode(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public Integer getStatusCode()
    {
        return (Integer)vertex.getProperty(OBlocksDatabase.WEB_NODE_STATUS_FIELD);
    }
    @Override
    public URI getPageUrl()
    {
        return UriBuilder.fromUri((String)vertex.getProperty(OBlocksDatabase.WEB_NODE_PAGE_FIELD)).build();
    }

    @Override
    public void setPageOk(URI pageUrl)
    {
        if (pageUrl == null) {
            setPageNotFound();
        } else {
            vertex.setProperty(OBlocksDatabase.WEB_NODE_PAGE_FIELD, pageUrl);
            this.setStatusCode(WebNode.OK);
        }
    }

    @Override
    public void setPageRedirect(URI pageUrl)
    {
        if (pageUrl == null) {
            setPageNotFound();
        } else {
            vertex.setProperty(OBlocksDatabase.WEB_NODE_PAGE_FIELD, pageUrl);
            this.setStatusCode(WebNode.REDIRECT);
        }
    }

    @Override
    public void setPageNotFound()
    {
        vertex.setProperty(OBlocksDatabase.WEB_NODE_PAGE_FIELD, null);
        this.setStatusCode(WebNode.NOT_FOUND);
    }

    /*
    * Get the path starting from this node with a name in a language
    *
    * @param name   the name of the path
    * @param locale The locale of the path
    * */
    @Override
    public WebPath getChildPath(String name, Locale locale)
    {
        WebPath retVal = null;
        if (locale.equals(Locale.ROOT)) {
            locale = BlocksConfig.instance().getDefaultLanguage();
        }
        String field = OBlocksDatabase.getLocalizedNameField(locale);
        Iterable<Edge> edges =  this.vertex.query().direction(Direction.OUT).labels(OBlocksDatabase.PATH_CLASS_NAME).has(field, name).edges();
        if(edges.iterator().hasNext()) {
            retVal = new OWebPath(edges.iterator().next());
        }
        return retVal;
    }

    @Override
    public WebPath getParentPath(String name, Locale locale)
    {
        WebPath retVal = null;
        Iterable<Edge> edges =  this.vertex.query().direction(Direction.IN).edges();
        if(edges.iterator().hasNext()) {
            retVal = new OWebPath(edges.iterator().next());
        }
        return retVal;
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    @Override
    public boolean isNotFound() {
        return this.getStatusCode().equals(404);
    }

    @Override
    public boolean isRedirect() {
        return this.getStatusCode().equals(303);
    }

    @Override
    public boolean isPage() {
        return this.getStatusCode().equals(200);
    }

    // ---- PRIVATE METHODS -------

    protected void setStatusCode(Integer code) {
        this.vertex.setProperty(OBlocksDatabase.WEB_NODE_STATUS_FIELD, code);
    }
}

package com.beligum.blocks.routing.nodes;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.routing.ORouteController;
import com.beligum.blocks.routing.ifaces.nodes.WebNode;
import com.beligum.blocks.routing.ifaces.nodes.WebPath;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

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
        return (Integer)vertex.getProperty(ORouteController.STATUS_FIELD);
    }
    @Override
    public String getPageUrl()
    {
        return vertex.getProperty(ORouteController.PAGE_FIELD);
    }
    @Override
    public void setStatusCode(Integer statusCode)
    {
        vertex.setProperty(ORouteController.STATUS_FIELD, statusCode);
    }
    @Override
    public void setPageUrl(String pageUrl)
    {
        vertex.setProperty(ORouteController.PAGE_FIELD, pageUrl);
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
        String field = ORouteController.getLocalizedNameField(locale);
        Iterable<Edge> edges =  this.vertex.query().direction(Direction.OUT).labels(ORouteController.PATH_CLASS_NAME).has(field, name).edges();
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
}

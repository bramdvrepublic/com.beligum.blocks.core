package com.beligum.blocks.routing;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.routing.ifaces.WebNode;
import com.beligum.blocks.routing.ifaces.WebPath;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public class OWebPath extends OrientEdge implements WebPath
{




    // The Orient Edge we are wrapping
    private Edge edge;

    public OWebPath(Edge edge) {
        this.edge = edge;
    }

    /*
    * Returns the name of a path in a certain locale
    * If no name is found for this locale we use the default name defined under Locale.ROOT
    * */
    @Override
    public String getName(Locale locale)
    {
        if (locale == null) {
            locale = Locale.ROOT;
        }
        String fieldName = OBlocksDatabase.getLocalizedNameField(locale);
        String retVal = this.edge.getProperty(fieldName);
        if (retVal == null) {
            retVal = this.edge.getProperty(OBlocksDatabase.getLocalizedNameField(Locale.ROOT));
        }
        return retVal;
    }

    /*
    * Sets the name of a path in a certain Locale
    * */
    @Override
    public void setName(String name, Locale locale)
    {

        String fieldName = OBlocksDatabase.getLocalizedNameField(locale);
        this.edge.setProperty(fieldName, name);

        // Make sure there is always a default fieldname
        if (getName(Locale.ROOT) == null || locale.equals(BlocksConfig.instance().getDefaultLanguage())) {
            this.edge.setProperty(OBlocksDatabase.getLocalizedNameField(Locale.ROOT), name);
        }

    }

    /*
    * Get the node where this path starts
    * */
    @Override
    public WebNode getParentWebNode()
    {
        WebNode retVal = null;
        Vertex v = this.edge.getVertex(Direction.OUT);
        if (v != null) {
            retVal = new OWebNode(v);
        }
        return retVal;
    }

    /*
    * Get the node where this path ends
    * */
    @Override
    public WebNode getChildWebNode()
    {
        WebNode retVal = null;
        Vertex v = this.edge.getVertex(Direction.IN);
        if (v != null) {
            retVal = new OWebNode(v);
        }
        return retVal;
    }


    @Override
    public void setCreatedAt(Calendar date)
    {
        this.edge.setProperty(OBlocksDatabase.RESOURCE_CREATED_AT, date.getTime());
    }
    @Override
    public Calendar getCreatedAt()
    {
        Calendar retVal = null;
        Date date = this.edge.getProperty(OBlocksDatabase.RESOURCE_CREATED_AT);
        if (date != null) {
            retVal = Calendar.getInstance();
            retVal.setTime(date);
        }
        return retVal;

    }

    @Override
    public void setCreatedBy(String user)
    {
        this.edge.setProperty(OBlocksDatabase.RESOURCE_CREATED_BY, user);
    }

    @Override
    public String getCreatedBy()
    {
        return this.edge.getProperty(OBlocksDatabase.RESOURCE_CREATED_BY);
    }

    @Override
    public void setUpdatedAt(Calendar date)
    {
        this.edge.setProperty(OBlocksDatabase.RESOURCE_UPDATED_AT, date.getTime());
    }

    @Override
    public Calendar getUpdatedAt()
    {
        Calendar retVal = null;
        Date date = this.edge.getProperty(OBlocksDatabase.RESOURCE_UPDATED_AT);
        if (date != null) {
            retVal = Calendar.getInstance();
            retVal.setTime(date);
        }
        return retVal;

    }

    @Override
    public void setUpdatedBy(String user)
    {
        this.edge.setProperty(OBlocksDatabase.RESOURCE_UPDATED_BY, user);
    }

    @Override
    public String getUpdatedBy()
    {
        return this.edge.getProperty(OBlocksDatabase.RESOURCE_UPDATED_BY);
    }


}

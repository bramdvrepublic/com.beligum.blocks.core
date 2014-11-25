package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractViewable;
import com.beligum.blocks.core.models.ifaces.Storable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public class Row extends AbstractViewable
{
    /**the version of the application this row is supposed to interact with*/
    protected String applicationVersion;
    /**the creator of this row*/
    protected String creator;
    /**boolean whether or not this elements template can be changed by the client, it cannot be changed after initialization*/
    protected final boolean isFinal;

    /**
     * Constructor
     * @param id the id to this row (is of the form "[site]/[pageName]#[rowId]")
     * @param template the template of this row
     * @param allChildren the children of this row
     * @param isFinal boolean whether or not the template of this element can be changed by the client
     */
    public Row(RedisID id, String template, Set<Entity> allChildren, boolean isFinal)
    {
        super(id, template, allChildren);
        this.isFinal = isFinal;
        //TODO BAS: this version should be fetched from pom.xml and added to the row.java as a field
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
    }

    /**
     * @param id the id to this row (is of the form "[site]/[pageName]#[rowId]")
     * @param template the template of this row
     * @param isFinal boolean whether or not the template of this element can be changed by the client
     * @param allChildren the children of this row
     * @param applicationVersion the version of the application this row was saved under
     * @param creator the creator of this row
     */
    public Row(RedisID id, String template, Set<Entity> allChildren, boolean isFinal, String applicationVersion, String creator){
        super(id, template, allChildren);
        this.isFinal = isFinal;
        this.applicationVersion = applicationVersion;
        this.creator = creator;
    }

    /**
     * @return boolean whether or not this rows template can be changed by the client
     */
    public boolean isFinal()
    {
        return isFinal;
    }


}

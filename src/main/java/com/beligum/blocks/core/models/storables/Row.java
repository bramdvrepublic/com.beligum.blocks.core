package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractViewable;
import com.beligum.blocks.core.models.ifaces.Storable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public class Row extends AbstractViewable implements Storable
{
    /**the version of the application this row is supposed to interact with*/
    private String applicationVersion;
    /**the creator of this row*/
    private String creator;
    /**boolean whether or not this elements template can be changed by the client, it cannot be changed after initialization*/
    protected final boolean isFinal;

    /**
     * Constructor
     * @param id the id to this row (is of the form "[site]/[pageName]#[rowId]")
     * @param template the template of this row
     * @param directChildren the direct children of this row
     * @param isFinal boolean whether or not the template of this element can be changed by the client
     */
    public Row(RedisID id, String template, Set<Row> directChildren, boolean isFinal)
    {
        super(id, template, directChildren);
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
     * @param directChildren the direct children of this row
     * @param applicationVersion the version of the application this row was saved under
     * @param creator the creator of this row
     */
    public Row(RedisID id, String template, Set<Row> directChildren, boolean isFinal, String applicationVersion, String creator){
        super(id, template, directChildren);
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

    /**
     * @return the name of the variable of this viewable in the template holding this viewable
     */
    public String getTemplateVariableName()
    {
        return this.getHtmlId();
    }

    /**
     *
     * @return the unique id of this element in the html-tree (html-file) it belongs to
     */
    public String getHtmlId()
    {
        return this.getId().getHtmlId();
    }

    //_______________IMPLEMENTATION OF STORABLE____________________//
    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }
    @Override
    public String getCreator()
    {
        return this.creator;
    }
    @Override
    public long getVersion()
    {
        return this.getId().getVersion();
    }
    @Override
    public RedisID getId()
    {
        return (RedisID) super.getId();
    }
    @Override
    public String getUnversionedId(){
        return this.getId().getUnversionedId();
    }
    @Override
    public String getVersionedId(){
        return this.getId().getVersionedId();
    }
    @Override
    public Map<String, String> toHash(){
        Map<String, String> hash = new HashMap<>();
        hash.put(DatabaseConstants.TEMPLATE, this.getTemplate());
        hash.put(DatabaseConstants.APP_VERSION, this.applicationVersion);
        hash.put(DatabaseConstants.CREATOR, this.creator);
        hash.put(DatabaseConstants.ELEMENT_CLASS_TYPE, this.getClass().getSimpleName());
        return hash;
    }

    //___________OVERRIDE OF OBJECT_____________//

    /**
     * Two rows are equal when their template, meta-data (page-class, creator and application-version), site-domain and unversioned element-id (everything after the '#') are equal
     * (thus equal through object-state, not object-address).
     * @param obj
     * @return true if two rows are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof Row) {
            if(obj == this){
                return true;
            }
            else {
                Row rowObj = (Row) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(template, rowObj.template)
                                                           .append(this.getHtmlId(), rowObj.getHtmlId())
                                                           .append(this.getId().getAuthority(), rowObj.getId().getAuthority())
                                                           .append(this.creator, rowObj.creator)
                                                           .append(this.applicationVersion, rowObj.applicationVersion);
                return significantFieldsSet.isEquals();
            }
        }
        else{
            return false;
        }
    }

    /**
     * Two rows have the same hashCode when their template, meta-data (page-class, creator and application-version), site-domain and unversioned element-id (everything after the '#') are equal
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        //7 and 31 are two randomly chosen prime numbers, needed for building hashcodes, ideally, these are different for each class
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(7, 31);
        significantFieldsSet = significantFieldsSet.append(template)
                                                   .append(this.getHtmlId())
                                                   .append(this.getId().getAuthority())
                                                   .append(this.creator)
                                                   .append(this.applicationVersion);
        return significantFieldsSet.toHashCode();
    }
}

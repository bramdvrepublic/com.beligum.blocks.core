package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.IdentifiableObject;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;

/**
 * Created by bas on 05.11.14.
 */
public abstract class AbstractTemplate extends IdentifiableObject implements Storable
{
    /**string representing the html-template of this element, once the template has been set, it cannot be changed*/
    protected String template;
    /**the version of the application this row is supposed to interact with*/
    protected String applicationVersion;
    /**the creator of this row*/
    protected String creator;

    /**
     * Constructor taking a unique id.
     * @param id id for this template
     * @param template the template-string which represents the content of this viewable
     */
    protected AbstractTemplate(RedisID id, String template)
    {
        super(id);
        this.template = template;
        //TODO: this version should be fetched from pom.xml
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
    }

    /**
     *
     * @return the template of this viewable
     */
    public String getTemplate()
    {
        return template;
    }

    //________________IMPLEMENTATION OF STORABLE_____________
    /**
     * Override of the getId-method of IdentifiableObject. Here a RedisID is returned, which has more functionalities.
     * @return the id of this storable
     */
    @Override
    public RedisID getId()
    {
        return (RedisID) super.getId();
    }
    /**
     * @return the version of this storable, which is the time it was created in milliseconds
     */
    @Override
    public long getVersion()
    {
        return this.getId().getVersion();
    }
    /**
     * @return the id of this storable with it's version attached ("[storableId]:[version]")
     */
    @Override
    public String getVersionedId(){
        return this.getId().getVersionedId();
    }
    /**
     * @return the id of this storable without a version attached ("[storableId]")
     */
    @Override
    public String getUnversionedId(){
        return this.getId().getUnversionedId();
    }
    /**
     * @return the id of the hash representing this storable ("[storableId]:[version]:hash")
     */
    @Override
    public String getHashId(){
        return this.getId().getHashId();
    }
    /**
     * @return the version of the application this storable is supposed ot interact with
     */
    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }
    /**
     * @return the creator of this storable
     */
    @Override
    public String getCreator()
    {
        return this.creator;
    }
    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     *
     * @return a map representing the key-value structure of this element to be saved to db
     */
    @Override
    public Map<String, String> toHash(){
        Map<String, String> hash = new HashMap<>();
        hash.put(DatabaseConstants.TEMPLATE, this.getTemplate());
        hash.put(DatabaseConstants.APP_VERSION, this.applicationVersion);
        hash.put(DatabaseConstants.CREATOR, this.creator);
        return hash;
    }

    //________________OVERRIDE OF OBJECT_______________//

    /**
     * Two templates have the same hashCode when their template-content, url and meta-data are equal.
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        //7 and 31 are two randomly chosen prime numbers, needed for building hashcodes, ideally, these are different for each class
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(7, 31);
        significantFieldsSet = significantFieldsSet.append(template)
                        .append(this.getUnversionedId())
                        .append(this.creator)
                        .append(this.applicationVersion);
        return significantFieldsSet.toHashCode();
    }

    /**
     * Two templates are equal when their template-content, url and meta-data are equal
     * (thus equal through object-state, not object-address).
     * @param obj
     * @return true if two templates are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof AbstractTemplate) {
            if(obj == this){
                return true;
            }
            else {
                AbstractTemplate abstractTemplateObj = (AbstractTemplate) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(template, abstractTemplateObj.template)
                                .append(this.getUnversionedId(), abstractTemplateObj.getUnversionedId())
                                .append(this.creator, abstractTemplateObj.creator)
                                .append(this.applicationVersion, abstractTemplateObj.applicationVersion);
                return significantFieldsSet.isEquals();
            }
        }
        else{
            return false;
        }
    }

    @Override
    public String toString()
    {
        return this.getTemplate();
    }
}

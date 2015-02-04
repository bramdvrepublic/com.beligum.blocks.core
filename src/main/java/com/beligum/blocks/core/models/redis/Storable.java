package com.beligum.blocks.core.models.redis;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.SerializationException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.utils.Utils;

import java.util.Map;

/**
 * Created by bas on 04.02.15.
 */
public class Storable extends Identifiable
{
    /**the version of the application this template is supposed to interact with*/
    protected String applicationVersion;
    /**the creator of this template*/
    protected String createdBy;
    /**the updater of this template*/
    protected String updatedBy;
    /**the moment of creation of this template*/
    protected String createdAt;
    /**the moment of last update of this template*/
    protected String updatedAt;
    /**deletion flag*/
    protected Boolean deleted = false;

    public Storable(RedisID id){
        super(id);
        //TODO BAS!: are applicationversions remembered like this?
        this.applicationVersion = BlocksConfig.getProjectVersion();
    }

    //________________IMPLEMENTATION OF STORABLE_____________
    /**
     * Override of the getId-method of IdentifiableObject. Here a RedisID is returned, which has more functionalities.
     * @return the id of this storable
     */
    public RedisID getId()
    {
        return (RedisID) super.getId();
    }
    /**
     * @return the version of this storable, which is the time it was created in milliseconds
     */
    public long getVersion()
    {
        return this.getId().getVersion();
    }
    /**
     * @return the id of this storable with it's version attached ("[storableId]:[version]")
     */
    public String getVersionedId(){
        return this.getId().getVersionedId();
    }
    /**
     * @return the id of this storable without a version attached ("[storableId]")
     */
    public String getUnversionedId(){
        return this.getId().getUnversionedId();
    }
    /**
     * @return the version of the application this storable is supposed ot interact with
     */
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }
    public void setApplicationVersion(String applicationVersion)
    {
        this.applicationVersion = applicationVersion;
    }
    /**
     * @return the creator of this storable
     */
    public String getCreatedBy()
    {
        return this.createdBy;
    }
    public void setCreatedBy(String created_by)
    {
        this.createdBy = created_by;
    }
    /**
     * @return the updater of this storable
     */
    public String getUpdatedBy()
    {
        return this.updatedBy;
    }
    public void setUpdatedBy(String updated_by)
    {
        this.updatedBy = updated_by;
    }
    /**
     * @return the moment of creation of this storable
     */
    public String getCreatedAt()
    {
        return createdAt;
    }
    public void setCreatedAt(String createdAt)
    {
        this.createdAt = createdAt;
    }
    /**
     * @return the moment of last update of this storable
     */
    public String getUpdatedAt()
    {
        return updatedAt;
    }
    public void setUpdatedAt(String updatedAt)
    {
        this.updatedAt = updatedAt;
    }
    /**
     * @return deletion flag
     */
    public Boolean getDeleted()
    {
        return deleted;
    }
    public void setDeleted(Boolean deleted)
    {
        this.deleted = deleted;
    }
    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     *
     * @return a map representing the key-value structure of this element to be saved to db
     */
    public Map<String, String> toHash() throws SerializationException
    {
        return Utils.toHash(this);
    }
}

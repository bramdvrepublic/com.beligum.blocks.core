package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.exceptions.SerializationException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.utils.Utils;
import com.beligum.core.framework.security.Authentication;
import com.beligum.core.framework.security.Principal;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.joda.time.LocalDateTime;

import java.util.Map;

/**
 * Created by wouter on 17/03/15.
 */
public class META
{
    protected Long documentVersion;
    /**the version of the application this template is supposed to interact with*/
    protected String applicationVersion;
    /**the creator of this template, it is the username of a Shiro principal*/
    protected String createdBy;
    /**the updater of this template, it is the username of a Shiro principal*/
    protected String updatedBy;
    /**the moment of creation of this template, formatted as f.i. 2015-02-17T13:38:21.170*/
    protected String createdAt;
    /**the moment of last update of this template, formatted as f.i. 2015-02-17T13:38:21.170*/
    protected String updatedAt;
    /**deletion flag*/
    protected Boolean deleted = false;


    public META() {
        this.documentVersion = 0L;
        this.applicationVersion = BlocksConfig.getProjectVersion();
    }

    //________________IMPLEMENTATION OF STORABLE_____________


    /**
     * @return the version of the application this storable is supposed ot interact with
     */
    public Long getDocumentVersion()
    {
        return this.documentVersion;
    }
    public void setDocumentVersion(Long documentVersion)
    {
        this.documentVersion = documentVersion;
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

    /**
     *
     * @return the current local time, in a format each {@link META} can understand
     */
    public static String getCurrentTime(){
        return LocalDateTime.now().toString();
    }

    /**
     *
     * @return the name (identifier) of the current authenticated user, in a format each {@link META} can understand
     */
    public static String getCurrentUserName(){
        Principal currentPrincipal;
        try{
            currentPrincipal = Authentication.getCurrentPrincipal();
            if (currentPrincipal != null) {
                return currentPrincipal.getUsername();
            }
            else {
                return DatabaseConstants.SERVER_USER_NAME;
            }
        }
        //if no Shiro securitymanager is present, this means we're still starting up the server (and thus no securitymanager is configured yet)
        catch(UnavailableSecurityManagerException e){
            return DatabaseConstants.SERVER_START_UP;
        }
    }


    public void touch(){
        this.documentVersion += 1;
        this.applicationVersion = BlocksConfig.getProjectVersion();
        this.updatedAt = getCurrentTime();
        this.updatedBy = getCurrentUserName();
        if (this.createdAt == null || this.createdBy == null) {
            this.createdAt = getCurrentTime();
            this.createdBy = getCurrentUserName();
        }
    }

}

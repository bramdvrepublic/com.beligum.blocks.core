package com.beligum.blocks.models;

import com.beligum.blocks.models.interfaces.BlocksStorable;
import com.beligum.blocks.models.jsonld.JsonLDContext;
import com.beligum.blocks.models.jsonld.ResourceNode;
import com.beligum.blocks.models.jsonld.hibernate.ResourceNodeUserType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/*
 * Created by wouter on 21/04/15.
 */
@TypeDef(name = "jsonb", typeClass = ResourceNodeUserType.class, parameters = {
                @org.hibernate.annotations.Parameter(name = ResourceNodeUserType.CLASS,
                                value = "com.beligum.blocks.models.jsonld.ResourceNode")})
@javax.persistence.Entity
@Table(name="resource")
public class ResourceContext implements Storable
{
    @Id
    @GeneratedValue
    private Long id;


    @Type(type = "jsonb")
    private ResourceNode data;
    private String blockId;
    private String language;

    private Long documentVersion;
    private String applicationVersion;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;



    public ResourceContext()
    {
        getContext().put("mot", "http://www.mot.be/ontology/");
        getContext().put("be", "http://www.beligum.com/blocks/schema/");
    }

    public ResourceContext(ResourceNode resource, String language) {
        this();
        this.data = resource;
        //        this.blockId = this.data.getMainResource().getId();
        this.language = language;
    }

    public ResourceNode getMainresource() {
        return this.data;
    }


    public JsonLDContext getContext()
    {
        JsonLDContext retVal = new JsonLDContext();
        if (this.data != null) {
            //            retVal = this.data.getContext();
        }
        return retVal;
    }


    public String getBlockId()
    {
        return blockId;
    }
    public void setBlockId(String blockId)
    {
        this.blockId = blockId;
    }

    public void createUUID()
    {
        this.blockId = UUID.randomUUID().toString();
        this.data.setId("http://www.mot.be/ontology/" + this.blockId);
    }


    @Override
    public String getLanguage()
    {
        return language;
    }

    @Override
    public void setLanguage(String language)
    {
        this.language = language;
    }


    @Override
    public Long getDocumentVersion()
    {
        return this.documentVersion;
    }

    @Override
    public void setDocumentVersion(Long documentVersion)
    {
        this.documentVersion = documentVersion;
    }

    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }

    @Override
    public void setApplicationVersion(String applicationVersion)
    {
        this.applicationVersion = applicationVersion;
    }

    /**
     * @return the creator of this storable
     */
    @Override
    public String getCreatedBy()
    {
        return this.createdBy;
    }

    @Override
    public void setCreatedBy(String created_by)
    {
        this.createdBy = created_by;
    }
    /**
     * @return the updater of this storable
     */
    @Override
    public String getUpdatedBy()
    {
        return this.updatedBy;
    }
    @Override
    public void setUpdatedBy(String updated_by)
    {
        this.updatedBy = updated_by;
    }
    /**
     * @return the moment of creation of this storable
     */
    @Override
    public String getCreatedAt()
    {
        return createdAt;
    }
    @Override
    public void setCreatedAt(String createdAt)
    {
        this.createdAt = createdAt;
    }
    /**
     * @return the moment of last update of this storable
     */
    @Override
    public String getUpdatedAt()
    {
        return updatedAt;
    }
    @Override
    public void setUpdatedAt(String updatedAt)
    {
        this.updatedAt = updatedAt;
    }



}

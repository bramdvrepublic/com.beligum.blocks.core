package com.beligum.blocks.models;

import com.beligum.base.models.BasicModelImpl;
import com.beligum.blocks.models.jsonld.JsonLDContext;
import com.beligum.blocks.models.jsonld.Resource;
import com.beligum.blocks.models.jsonld.ResourceImpl;
import com.beligum.blocks.models.jsonld.hibernate.ResourceUserType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/*
 * Created by wouter on 21/04/15.
 */
@TypeDef(name = "jsonb", typeClass = ResourceUserType.class, parameters = {
                @org.hibernate.annotations.Parameter(name = ResourceUserType.CLASS,
                                value = "com.beligum.blocks.models.jsonld.Resource")})
@javax.persistence.Entity
@Table(name="resource")
public class ResourceContext extends BasicModelImpl
{

    @Type(type = "jsonb")
    private Resource data;
    private String blockId;
    private String language;


    public ResourceContext()
    {
    }

    public ResourceContext(Resource resource, String language) {
        this();
        this.data = resource;
        this.setBlockId(resource.getId());
        this.language = language;
    }

    public Resource getResource() {
        return this.data;
    }


    public String getBlockId()
    {
        return blockId;
    }
    public void setBlockId(String blockId)
    {
        this.blockId = blockId;
    }


    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }





}

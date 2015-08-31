package com.beligum.blocks.models.sql;

import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Resource;

import javax.persistence.*;
import java.io.IOException;
import java.util.*;

/**
 * Created by wouter on 29/06/15.
 */

@Entity
@Table(name="resource")
public class DBResource extends DBDocumentInfo
{

    protected String blockId;

    @Lob
    protected byte[] data;

    // Default constructor for hibernate
    public DBResource() {

    }

    public DBResource(Resource resource) throws IOException
    {
        this.blockId = resource.getBlockId().toString();
        setResource(resource);
    }

    // ------ GETTERS AND SETTERS ----------

    public Long getId() {
        return this.id;
    }


    // ---------PUBLIC METHODS -----------

    public Resource getResource(Locale locale) throws Exception
    {
        return ResourceFactoryImpl.instance().deserializeResource(this.data, locale);
    }


    // Serializes the resource
    public void setResource(Resource resource) throws IOException
    {
        this.data = ResourceFactoryImpl.instance().serializeResource(resource, true).getBytes();
    }



}

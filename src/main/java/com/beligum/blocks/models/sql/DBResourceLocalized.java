package com.beligum.blocks.models.sql;

import javax.persistence.*;

/**
 * Created by wouter on 1/07/15.
 */
@Entity
@Table(name="resource_language")
public class DBResourceLocalized extends DBDocumentInfo
{
    @ManyToOne
    private DBResource resource;

    @Lob
    private byte[] data;

    public DBResourceLocalized() {

    }


    public DBResourceLocalized(byte[] data) {
        this.setdata(data);
    }

    public byte[] getData() {
        return this.data;
    }

    public void setdata(byte[] data) {
        this.data = data;
    }

    public DBResource getResource() {
        return resource;
    }

    public void setResource(DBResource resource) {
        this.resource = resource;
    }

}

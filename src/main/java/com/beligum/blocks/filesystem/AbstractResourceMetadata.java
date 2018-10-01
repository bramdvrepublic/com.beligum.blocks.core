package com.beligum.blocks.filesystem;

import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public abstract class AbstractResourceMetadata implements ResourceMetadata
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected static DatatypeFactory datatypeFactory;

    //-----CONSTRUCTORS-----
    protected AbstractResourceMetadata() throws IOException
    {
        if (datatypeFactory == null) {
            try {
                datatypeFactory = DatatypeFactory.newInstance();
            }
            catch (DatatypeConfigurationException e) {
                throw new IOException("Error while initializing the data type factory", e);
            }
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public java.time.LocalDateTime getCreatedLocal()
    {
        return this.toLocalDateTime(this.getCreated());
    }
    @Override
    public java.time.LocalDateTime getLastModifiedLocal()
    {
        return this.toLocalDateTime(this.getLastModified());
    }

    //-----PROTECTED METHODS-----
    protected LocalDateTime toLocalDateTime(ZonedDateTime dateTime)
    {
        return dateTime == null ? null : dateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
    protected DatatypeFactory getDatatypeFactory()
    {
        return datatypeFactory;
    }
    //-----PRIVATE METHODS-----

    //-----PRIVATE METHODS-----
}

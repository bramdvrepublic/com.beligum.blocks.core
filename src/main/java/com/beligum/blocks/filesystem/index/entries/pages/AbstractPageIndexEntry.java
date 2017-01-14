package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.NativeProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schemagen.ProtobufSchemaGenerator;
import org.apache.lucene.index.Term;

import java.net.URI;

/**
 * Created by bram on 2/14/16.
 */
public abstract class AbstractPageIndexEntry implements IndexEntry
{
    //-----CONSTANTS-----
    private static ObjectMapper PROTOBUF_MAPPER;

    //-----VARIABLES-----
    protected String id;
    protected String title;
    protected String description;
    protected String image;

    //-----CONSTRUCTORS-----
    protected AbstractPageIndexEntry(String id)
    {
        this.id = id;
    }

    //-----STATIC METHODS-----
    public static Term toLuceneId(URI id)
    {
        return new Term(Field.id.name(), id.toString());
    }
    public static Term toLuceneId(IndexEntry indexEntry)
    {
        return new Term(Field.id.name(), indexEntry.getId().toString());
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getId()
    {
        return id;
    }
    @Override
    public String getTitle()
    {
        return title;
    }
    @Override
    public String getDescription()
    {
        return description;
    }
    @Override
    public String getImage()
    {
        return image;
    }

    //-----PROTECTED METHODS-----
    protected void setId(String id)
    {
        this.id = id;
    }
    protected void setTitle(String title)
    {
        this.title = title;
    }
    protected void setDescription(String description)
    {
        this.description = description;
    }
    protected void setImage(String image)
    {
        this.image = image;
    }

    protected static ObjectMapper getProtobufMapper()
    {
        if (PROTOBUF_MAPPER == null) {
            PROTOBUF_MAPPER = new ProtobufMapper();
        }

        return PROTOBUF_MAPPER;
    }
    //see https://github.com/FasterXML/jackson-dataformats-binary/tree/master/protobuf
    protected static String createProtobufSchema(Class<?> clazz) throws JsonMappingException
    {
        ObjectMapper mapper = getProtobufMapper();
        ProtobufSchemaGenerator gen = new ProtobufSchemaGenerator();
        mapper.acceptJsonFormatVisitor(clazz, gen);
        ProtobufSchema schemaWrapper = gen.getGeneratedSchema();
        NativeProtobufSchema nativeProtobufSchema = schemaWrapper.getSource();

        return nativeProtobufSchema.toString();
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof AbstractPageIndexEntry))
            return false;

        AbstractPageIndexEntry that = (AbstractPageIndexEntry) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;

    }
    @Override
    public int hashCode()
    {
        return getId() != null ? getId().hashCode() : 0;
    }
}

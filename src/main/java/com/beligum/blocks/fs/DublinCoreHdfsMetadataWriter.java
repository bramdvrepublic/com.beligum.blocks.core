package com.beligum.blocks.fs;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.schema.dublincore.simple.v20021212.jaxb.ElementContainer;
import com.beligum.blocks.schema.dublincore.simple.v20021212.jaxb.ObjectFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import javax.xml.bind.JAXB;
import java.io.*;

/**
 * Created by bram on 1/20/16.
 */
public class DublinCoreHdfsMetadataWriter extends AbstractHdfsMetadataWriter
{
    //-----CONSTANTS-----
    private static final String DC_XSD_RESOURCE_PATH = "/com/beligum/blocks/schema/dublincore/simple/v20021212/simpledc20021212.xsd";

    //-----VARIABLES-----
    //valid during an entire session (after a successful init())
    private ObjectFactory factory;

    //valid during an open/write session, nulled after close()
    private ElementContainer root;

    //-----CONSTRUCTORS-----
    public DublinCoreHdfsMetadataWriter() throws IOException
    {
        super();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void init(FileSystem fileSystem) throws IOException
    {
        super.init(fileSystem);

        this.factory = new ObjectFactory();

        throw new RuntimeException("UNTESTED");
    }
    @Override
    public void open(PathInfo<Path> pathInfo) throws IOException
    {
        super.open(pathInfo);

        //read in the existing metadata if it exists, or create a new instance if it doesn't
        if (fileSystem.exists(this.baseMetadataFile)) {
            try (Reader reader = new BufferedReader(new InputStreamReader(fileSystem.open(this.baseMetadataFile)))) {
                this.root = JAXB.unmarshal(reader, ElementContainer.class);
            }
        }

        if (this.root == null) {
            this.root = this.factory.createElementContainer();
        }
    }
    @Override
    public void updateCreator(Person creator) throws IOException
    {
        super.updateCreator(creator);

        throw new RuntimeException("NOT IMPLEMENTED YET");
    }
    @Override
    public void updateTimestamps() throws IOException
    {
        super.updateTimestamps();

        throw new RuntimeException("NOT IMPLEMENTED YET");
    }
    @Override
    public void write() throws IOException
    {
        super.write();

        //write the metadata to disk (overwriting the possibly existing metadata; that's ok since we read it in first)
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(this.fileSystem.create(this.baseMetadataFile, true)))) {
            JAXB.marshal(root, writer);
        }
    }
    @Override
    public void close() throws IOException
    {
        super.close();

        this.root = null;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected String getXsdResourcePath()
    {
        return DC_XSD_RESOURCE_PATH;
    }

    //-----PRIVATE METHODS-----

}

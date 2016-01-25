package com.beligum.blocks.fs;

import com.beligum.base.auth.models.Person;
import com.beligum.base.config.CoreConfiguration;
import com.beligum.blocks.fs.ifaces.Constants;
import com.beligum.blocks.fs.ifaces.HdfsMetadataWriter;
import com.beligum.blocks.fs.ifaces.PathInfo;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by bram on 1/20/16.
 */
public abstract class AbstractHdfsMetadataWriter implements HdfsMetadataWriter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //valid during an entire session (after a successful init())
    protected FileSystem fileSystem;
    protected URL schemaResource;
    protected FileChecksum schemaResourceChecksum;

    //valid during an open/write session, nulled after close()
    protected Path baseMetadataFile;
    protected Path baseMetadataSchema;

    //session control flags
    protected boolean inited;
    protected boolean opened;

    //-----CONSTRUCTORS-----
    protected AbstractHdfsMetadataWriter() throws IOException
    {
        this.inited = false;
        this.opened = false;
    }

    //-----PUBLIC METHODS-----
    @Override
    public void init(FileSystem fileSystem) throws IOException
    {
        this.fileSystem = fileSystem;

        //save the URI, calc the hash (and test it's valid at the same time)
        try {
            this.schemaResource = this.getClass().getResource(this.getXsdResourcePath());
            this.schemaResourceChecksum = this.fileSystem.getFileChecksum(new Path(this.schemaResource.toURI()));
        }
        catch (Exception e) {
            throw new IOException("Error while validating the XSD schema location; " + this.getXsdResourcePath(), e);
        }

        this.inited = true;
    }
    @Override
    public void open(PathInfo<Path> pathInfo) throws IOException
    {
        if (!this.inited) {
            throw new IOException("Please init this reader first");
        }
        else {
            this.baseMetadataSchema = new Path(pathInfo.getMetaMetadataFolder(), Constants.META_METADATA_FILE_METADATA_XSD);
            if (fileSystem.exists(this.baseMetadataSchema)) {
                // If the schema file exists, calculate it's hash and make sure it equals the schema file of the data we're about to write.
                // If it doesn't, we can't reliably proceed cause we currently don't have a means to evolve the schemata
                FileChecksum existingChecksum = this.fileSystem.getFileChecksum(this.baseMetadataSchema);
                //TODO the first check actually means no checksumming is available for this filesystem; fix that (it even exists for local file system!)
                if (existingChecksum!=this.schemaResourceChecksum && !existingChecksum.equals(this.schemaResourceChecksum)) {
                    throw new IOException(
                                    "Checksum of metadata schemas didn't correspond. You're probably trying to write metadata with a newer schema than the one stored on disk, but I don't know how to do that; " +
                                    baseMetadataSchema);
                }
            }
            else {
                //copy the schema resource file to the destination
                try (
                                InputStream is = this.schemaResource.openStream();
                                OutputStream os = this.fileSystem.create(baseMetadataSchema);
                ) {
                    IOUtils.copyBytes(is, os, this.fileSystem.getConf());
                }
            }

            this.baseMetadataFile = new Path(pathInfo.getMetaMetadataFolder(), Constants.META_METADATA_FILE_METADATA_XML);

            this.opened = true;
        }
    }
    @Override
    public void updateSchemaData() throws IOException
    {
        if (!this.opened) {
            throw new IOException("Please open this reader first");
        }
    }
    @Override
    public void updateSoftwareData(CoreConfiguration.ProjectProperties properties) throws IOException
    {
        if (!this.opened) {
            throw new IOException("Please open this reader first");
        }
    }
    @Override
    public void updateFileData() throws IOException
    {
        if (!this.opened) {
            throw new IOException("Please open this reader first");
        }
    }
    @Override
    public void updateCreator(Person creator) throws IOException
    {
        if (!this.opened) {
            throw new IOException("Please open this reader first");
        }
    }
    @Override
    public void updateTimestamps() throws IOException
    {
        if (!this.opened) {
            throw new IOException("Please open this reader first");
        }
    }
    @Override
    public void write() throws IOException
    {
        if (!this.opened) {
            throw new IOException("Please open this reader first");
        }
    }
    @Override
    public void close() throws IOException
    {
        this.baseMetadataFile = null;
        this.baseMetadataSchema = null;
        this.opened = false;
    }

    //-----PROTECTED METHODS-----
    protected abstract String getXsdResourcePath();

    //-----PRIVATE METHODS-----

}

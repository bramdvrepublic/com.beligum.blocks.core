package com.beligum.blocks.fs.metadata.ifaces;

import com.beligum.base.auth.models.Person;
import com.beligum.base.config.CoreConfiguration;
import com.beligum.blocks.fs.ifaces.ResourcePath;

import java.io.IOException;

/**
 * Created by bram on 1/20/16.
 */
public interface MetadataWriter<T> extends AutoCloseable
{
    /**
     * Read the medatadata file; create it if it doesn't exist or read in the existing metadata if it does.
     *
     * @param resourcePath
     * @throws IOException
     */
    void open(ResourcePath resourcePath) throws IOException;

    void updateSchemaData() throws IOException;

    void updateSoftwareData(CoreConfiguration.ProjectProperties properties) throws IOException;

    void updateFileData() throws IOException;

    void updateCreator(Person creator) throws IOException;

    void updateTimestamps() throws IOException;

    void write() throws IOException;

    @Override
    void close() throws IOException;
}

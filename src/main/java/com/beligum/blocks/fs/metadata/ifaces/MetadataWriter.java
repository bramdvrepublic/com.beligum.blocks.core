package com.beligum.blocks.fs.metadata.ifaces;

import com.beligum.base.auth.models.Person;
import com.beligum.base.config.CoreConfiguration;
import com.beligum.blocks.fs.ifaces.PathInfo;

import java.io.IOException;

/**
 * Created by bram on 1/20/16.
 */
public interface MetadataWriter<T>
{
    /**
     * Read the medatadata file; create it if it doesn't exist or read in the existing metadata if it does.
     *
     * @param pathInfo
     * @throws IOException
     */
    void open(PathInfo pathInfo) throws IOException;

    void updateSchemaData() throws IOException;

    void updateSoftwareData(CoreConfiguration.ProjectProperties properties) throws IOException;

    void updateFileData() throws IOException;

    void updateCreator(Person creator) throws IOException;

    void updateTimestamps() throws IOException;

    void write() throws IOException;

    void close() throws IOException;
}

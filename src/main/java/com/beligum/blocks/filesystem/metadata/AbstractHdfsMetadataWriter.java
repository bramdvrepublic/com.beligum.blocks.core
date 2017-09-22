/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.metadata;

import com.beligum.base.models.Person;
import com.beligum.base.config.CoreConfiguration;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.filesystem.metadata.ifaces.MetadataWriter;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 1/20/16.
 */
public abstract class AbstractHdfsMetadataWriter implements MetadataWriter<Path>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //valid during an entire session (after a successful init())
    protected FileContext fileSystem;

    //valid during an open/write session, nulled after close()
    protected Path baseMetadataFile;

    //session control flags
    protected boolean inited;
    protected boolean opened;

    //-----CONSTRUCTORS-----
    protected AbstractHdfsMetadataWriter(FileContext fileSystem) throws IOException
    {
        this.inited = false;
        this.opened = false;

        this.fileSystem = fileSystem;

        this.inited = true;
    }

    //-----PUBLIC METHODS-----
    @Override
    public void open(BlocksResource blocksResource) throws IOException
    {
        if (!this.inited) {
            throw new IOException("Please init this reader first");
        }
        else {
            this.baseMetadataFile = new Path(blocksResource.getMetadataFolder(), BlocksResource.META_METADATA_FILE_METADATA_XML);
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
        this.opened = false;
    }

    //-----PROTECTED METHODS-----
    protected abstract String getXsdResourcePath();

    //-----PRIVATE METHODS-----

}

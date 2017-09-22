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

package com.beligum.blocks.filesystem.metadata.ifaces;

import com.beligum.base.models.Person;
import com.beligum.base.config.CoreConfiguration;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;

import java.io.IOException;

/**
 * Created by bram on 1/20/16.
 */
public interface MetadataWriter<T> extends AutoCloseable
{
    /**
     * Read the medatadata file; instance it if it doesn't exist or read in the existing metadata if it does.
     *
     * @param blocksResource
     * @throws IOException
     */
    void open(BlocksResource blocksResource) throws IOException;

    void updateSchemaData() throws IOException;

    void updateSoftwareData(CoreConfiguration.ProjectProperties properties) throws IOException;

    void updateFileData() throws IOException;

    void updateCreator(Person creator) throws IOException;

    void updateTimestamps() throws IOException;

    void write() throws IOException;

    @Override
    void close() throws IOException;
}

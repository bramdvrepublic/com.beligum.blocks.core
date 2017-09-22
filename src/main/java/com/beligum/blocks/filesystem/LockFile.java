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

package com.beligum.blocks.filesystem;

import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 1/19/16.
 */
public class LockFile implements AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private BlocksResource blocksResource;
    private Path lockFile;

    //-----CONSTRUCTORS-----
    public LockFile(BlocksResource blocksResource, Path lockFile)
    {
        this.blocksResource = blocksResource;
        this.lockFile = lockFile;
    }

    //-----PUBLIC METHODS-----
    public Path getLockFile()
    {
        return lockFile;
    }
    @Override
    public void close() throws IOException
    {
        this.blocksResource.releaseLockFile(this);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

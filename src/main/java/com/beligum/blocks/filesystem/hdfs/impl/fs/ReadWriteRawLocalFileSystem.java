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

package com.beligum.blocks.filesystem.hdfs.impl.fs;

import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.RawLocalFileSystem;

/**
 * Created by bram on 1/14/17.
 */
public class ReadWriteRawLocalFileSystem extends RawLocalFileSystem
{
    //-----CONSTANTS-----
    public static final String SCHEME = FsConstants.LOCAL_FS_URI.getScheme();

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadWriteRawLocalFileSystem()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getScheme()
    {
        return SCHEME;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

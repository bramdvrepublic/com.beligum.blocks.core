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

package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.blocks.filesystem.hdfs.impl.fs.ReadWriteRawLocalFileSystem;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/14/17.
 */
public class LocalReadWriteFS extends AbstractLocalFS
{
    //-----CONSTANTS-----
    public static final String SCHEME = ReadWriteRawLocalFileSystem.SCHEME;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected LocalReadWriteFS(URI uri, Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, conf, new ReadWriteRawLocalFileSystem());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

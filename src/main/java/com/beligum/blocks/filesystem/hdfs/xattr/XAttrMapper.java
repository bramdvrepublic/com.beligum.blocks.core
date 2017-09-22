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

package com.beligum.blocks.filesystem.hdfs.xattr;

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * All classes implementing this method will be registered into the XAttrResolver during startup.
 *
 * Created by bram on 10/23/15.
 */
public interface XAttrMapper
{
    /**
     * The name of the xattr to register.
     * For more information, see https://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-hdfs/ExtendedAttributes.html
     *
     * Eg. user.blocks.media.PROGRESS
     */
    String getXAttribute();

    /**
     * Fetch this xattr data of the supplied path
     */
    Object resolveXAttribute(FileContext fileContext, Path path) throws IOException;
}

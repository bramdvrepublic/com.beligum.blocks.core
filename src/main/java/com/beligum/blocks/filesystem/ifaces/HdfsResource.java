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

package com.beligum.blocks.filesystem.ifaces;

import com.beligum.base.resources.ifaces.Resource;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

/**
 * General superclass for all resources that are built in a HDFS file system
 *
 * Created by bram on 12/30/16.
 */
public interface HdfsResource extends Resource
{
    /**
     * The path to this resource's local data in the current file context
     */
    Path getLocalStoragePath();

    /**
     * The HDFS file context that belongs to this resource.
     * Note that this means the same file can have different resource instances because of a different fileContext
     * (eg. read-only filesystem vs. read-write filesystem)
     */
    FileContext getFileContext();
}

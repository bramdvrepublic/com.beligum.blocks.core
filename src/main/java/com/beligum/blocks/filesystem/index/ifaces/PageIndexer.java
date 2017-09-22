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

package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.hdfs.TX;

import java.io.IOException;

/**
 * Created by bram on 12/04/17.
 */
public interface PageIndexer extends Indexer
{
    /**
     * This is the overridden general connect() method from the super interface to be able to be more specific in it's return type.
     */
    @Override
    PageIndexConnection connect(TX tx) throws IOException;
}

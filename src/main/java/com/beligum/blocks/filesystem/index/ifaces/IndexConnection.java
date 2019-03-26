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

import com.beligum.base.resources.ifaces.Resource;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/21/16.
 */
public interface IndexConnection extends XAClosableResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * Fetch the index entry for the supplied URI from the underlying index store
     */
    IndexEntry get(URI key) throws IOException;

    /**
     * (re-)index the supplied resource into the underlying index store
     */
    void update(Resource resource) throws IOException;

    /**
     * Remove the index entry of the supplied resource from the underlying index store
     */
    void delete(Resource resource) throws IOException;

    /**
     * Remove all entries from the underlying index store and start over
     */
    void deleteAll() throws IOException;

    /**
     * Search the index using the supplied query, filters and options
     */
    IndexSearchResult search(IndexSearchRequest indexSearchRequest) throws IOException;

    /**
     * Low-level search request with implementation-specific query string
     */
    IndexSearchResult search(String query) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

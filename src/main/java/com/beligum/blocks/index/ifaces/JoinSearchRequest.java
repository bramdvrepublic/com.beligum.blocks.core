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

package com.beligum.blocks.index.ifaces;

import com.beligum.blocks.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.util.List;
import java.util.Map;

/**
 * Created by bram on 6/3/17.
 */
public interface JoinSearchRequest extends FilteredSearchRequest
{
     enum JoinOption implements Option
    {
        REGULAR,
        BLOCKJOIN
    }
    enum BlockJoinOption implements Option
    {
        PARENT,
        CHILD
    }
    /**
     *
     * @param from the first leg of the 'join on' query
     * @param to the last leg of the 'join on' query
     * @param rdfClass the rdfClass to want to query
     * @return
     */

    JoinSearchRequest addJoin(IndexEntryField from, IndexEntryField to, RdfClass rdfClass, FilteredSearchRequest filteredSearchRequest, Option... options);
    JoinSearchRequest addJoinFilter(RdfClass rdfClass, RdfProperty rdfProperty, IndexEntryField from, IndexEntryField to, String value, FilterBoolean filterBoolean, Option... options);
    JoinSearchRequest addJoinFilter(RdfClass rdfClass, IndexEntryField indexEntryField, IndexEntryField from, IndexEntryField to, String value, FilterBoolean filterBoolean, Option... options);

}

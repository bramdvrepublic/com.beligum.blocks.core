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

import com.beligum.blocks.rdf.ifaces.RdfClass;

/**
 * Created by bram on 6/3/17.
 */
public interface GraphSearchRequest extends FilteredSearchRequest
{
    enum GraphSearchOption implements FilteredSearchRequest.Option {
        returnRoot,
        leafNodesOnly
    }
    /**
     * Perform a Graph search on the results of the FilteredSearchRequest.
     * @param from: the starting point of the the graph traversal. This property must, of course, exist in the FilteredSearchRequest result.
     * @param to: the end of  the graph traversal. This property must, of course, exist in the FilteredSearchRequest result.
     * @return
     */
    GraphSearchRequest constructGraph(IndexEntryField from, IndexEntryField to, Option... options);
    GraphSearchRequest appendGraphSource(FilteredSearchRequest indexSearchRequest);

}

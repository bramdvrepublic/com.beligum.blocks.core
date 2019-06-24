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

import com.beligum.base.server.ifaces.RequestCloseable;

import java.util.Iterator;

/**
 * An interface to wrap a data model, needed to render out a list (dropdown) of possible values
 * as the result of a SPARQL query. Note that the returned entries may be tuples or triples.
 * In case of tuples, they might be used as value/label pairs, eg. to build a dropdown-filter.
 *
 * This class is made to avoid having to parse the results twice
 * (once in the back-end, once while rendering the front-end).
 *
 * Created by bram on 19/04/17.
 */
public interface RdfResult<E extends RdfResult.RdfResultEntry> extends Iterator<E>
{
    interface RdfResultEntry
    {
    }

    interface Triple<S, P, O> extends RdfResultEntry
    {
        S getSubject();
        P getPredicate();
        O getObject();
    }

    interface Tuple<K, V> extends RdfResultEntry
    {
        K getLabel();
        V getValue();
    }
}

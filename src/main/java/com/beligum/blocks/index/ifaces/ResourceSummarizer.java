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
import org.eclipse.rdf4j.model.Model;

/**
 * Class used to pull some general information (like title, description, image, link, ...) from a resource class to be used to render search results.
 * This is used while indexing a page to be able to render search results from the index without having to look up the whole page.
 *
 * Created by bram on 5/9/16.
 */
public interface ResourceSummarizer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Parse the supplied model and return the generated index that was extracted from it.
     * Note that the type should be allowed to be empty.
     */
    ResourceSummary summarize(RdfClass type, Model model);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

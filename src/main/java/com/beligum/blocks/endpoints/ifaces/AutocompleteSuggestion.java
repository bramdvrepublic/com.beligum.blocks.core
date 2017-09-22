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

package com.beligum.blocks.endpoints.ifaces;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public interface AutocompleteSuggestion
{
    /**
     * The formal (machine-readable) value associated with this suggestion.
     * Mostly, this will be a URI (eg. for resource suggestions), but can also be a plain string value (eg. for enum suggestions)
     */
    String getValue();

    /**
     * The RDF type class of the returned suggestion
     */
    URI getResourceType();

    /**
     * The address of the public page of this suggestion (where you can surf to to get more information about this suggestion).
     * In case of a resource suggestion, this will be the public page (in a suitable language) of the resource.
     * In case of an enum suggestion, this will probably be null.
     */
    URI getPublicPage();

    /**
     * The main name to display to the user for this suggestions (eg. the top line in the auto-complete results entry)
     */
    String getTitle();

    /**
     * The (possibly empty) second line in the auto-complete results entry to further specify the details of the suggestion
     */
    String getSubTitle();
}

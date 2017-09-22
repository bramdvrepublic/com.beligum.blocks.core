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

package com.beligum.blocks.exceptions;

import java.io.IOException;
import java.net.URI;

/**
 * Because some index values depend on others to be indexed first,
 * we need a way to signal (eg. the bulk re-indexer) that the setRollbackOnly happened
 * is because it needs to index another resource first.
 *
 * Created by bram on 8/29/16.
 */
public class NotIndexedException extends IOException
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI resourceBeingIndexed;
    private URI resourceNeedingIndexation;

    //-----CONSTRUCTORS-----
    public NotIndexedException(URI resourceBeingIndexed, URI resourceNeedingIndexation, String message)
    {
        super(message);

        this.resourceBeingIndexed = resourceBeingIndexed;
        this.resourceNeedingIndexation = resourceNeedingIndexation;
    }

    //-----PUBLIC METHODS-----
    public URI getResourceBeingIndexed()
    {
        return resourceBeingIndexed;
    }
    public URI getResourceNeedingIndexation()
    {
        return resourceNeedingIndexation;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

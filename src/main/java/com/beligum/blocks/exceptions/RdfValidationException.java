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

import com.beligum.base.exceptions.PublicApplicationException;
import com.beligum.base.filesystem.MessagesFileEntry;

import javax.validation.ConstraintViolation;
import java.util.Set;

/**
 * RDF exception to use when invalid RDF data is about to be saved.
 */
public class RdfValidationException extends PublicApplicationException
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public RdfValidationException(MessagesFileEntry message, Object... params)
    {
        super(message, params);
    }
    public RdfValidationException(MessagesFileEntry message, Throwable cause, Object... params)
    {
        super(message, cause, params);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

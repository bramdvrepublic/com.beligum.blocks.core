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

package com.beligum.blocks.templating.blocks.directives;

import java.io.StringWriter;

/**
 * Created by bram on 8/15/15.
 */
public class WriterBufferReference
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private TemplateResourcesDirective.Argument type;
    private int writerBufferPosition = -1;

    //-----CONSTRUCTORS-----
    public WriterBufferReference(TemplateResourcesDirective.Argument type, StringWriter writer)
    {
        //we save the position so we can insert right here later on (when everything is rendered)
        this.writerBufferPosition = writer.getBuffer().length();
        this.type = type;
    }

    //-----PUBLIC METHODS-----
    public TemplateResourcesDirective.Argument getType()
    {
        return type;
    }
    public int getWriterBufferPosition()
    {
        return writerBufferPosition;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

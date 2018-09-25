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

package com.beligum.blocks.filesystem.index.results;

import com.beligum.blocks.filesystem.index.ifaces.RdfTupleResult;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.glassfish.jersey.server.monitoring.RequestEvent;

import java.util.NoSuchElementException;

/**
 * Created by bram on 6/05/17.
 */
public class BooleanRdfResult implements RdfTupleResult<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private int index;

    //-----CONSTRUCTORS-----
    public BooleanRdfResult()
    {
        this.index = 0;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext()
    {
        return this.index < 2;
    }
    @Override
    public Tuple<String, String> next()
    {
        Tuple<String, String> retVal;

        switch (this.index) {
            case 0:
                retVal = new StringTuple(core.Entries.toggleLabelYes.toString(), "true");
                break;
            case 1:
                retVal = new StringTuple(core.Entries.toggleLabelNo.toString(), "false");
                break;
            default:
                throw new NoSuchElementException("No element at index " + this.index);
        }

        this.index++;

        return retVal;
    }
    @Override
    public void close(RequestEvent event) throws Exception
    {
        this.close();
    }
    @Override
    public void close() throws Exception
    {
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

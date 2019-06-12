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

package com.beligum.blocks.index.results;

import com.beligum.blocks.index.ifaces.RdfTupleResult;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.glassfish.jersey.server.monitoring.RequestEvent;

import java.util.NoSuchElementException;

/**
 * An iterable key/value list of Strings, to render out eg. a dropdown-box with values and labels.
 *
 * Created by bram on 19/04/17.
 */
public class StringTupleRdfResult implements RdfTupleResult<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private TupleQueryResult tupleQueryResult;
    private String labelBinding;
    private String valueBinding;

    //-----CONSTRUCTORS-----
    public StringTupleRdfResult(TupleQueryResult tupleQueryResult, String labelBinding, String valueBinding)
    {
        this.tupleQueryResult = tupleQueryResult;
        this.labelBinding = labelBinding;
        this.valueBinding = valueBinding;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext()
    {
        return this.tupleQueryResult.hasNext();
    }
    @Override
    public Tuple<String, String> next()
    {
        Tuple<String, String> retVal = null;

        if (this.tupleQueryResult != null && this.tupleQueryResult.hasNext()) {
            BindingSet bindings = this.tupleQueryResult.next();
            Value key = bindings.getValue(this.labelBinding);
            Value value = bindings.getValue(this.valueBinding);
            retVal = new StringTuple(key == null ? null : key.stringValue(), value == null ? null : value.stringValue());
        }
        else {
            throw new NoSuchElementException();
        }

        return retVal;
    }
    @Override
    public void close(boolean forceRollback) throws Exception
    {
        this.close();
    }
    @Override
    public void close() throws Exception
    {
        if (this.tupleQueryResult != null) {
            this.tupleQueryResult.close();
            this.tupleQueryResult = null;
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

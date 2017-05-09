package com.beligum.blocks.filesystem.index.results;

import com.beligum.blocks.filesystem.index.ifaces.RdfTupleResult;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;

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

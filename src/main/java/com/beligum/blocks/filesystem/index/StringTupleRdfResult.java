package com.beligum.blocks.filesystem.index;

import com.beligum.blocks.filesystem.index.ifaces.RdfTupleResult;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import java.util.NoSuchElementException;

/**
 * Created by bram on 19/04/17.
 */
public class StringTupleRdfResult implements RdfTupleResult<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private TupleQueryResult tupleQueryResult;
    private String keyBinding;
    private String valueBinding;

    //-----CONSTRUCTORS-----
    public StringTupleRdfResult(TupleQueryResult tupleQueryResult, String keyBinding, String valueBinding)
    {
        this.tupleQueryResult = tupleQueryResult;
        this.keyBinding = keyBinding;
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
            Value key = bindings.getValue(this.keyBinding);
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

    //-----INNER CLASSES-----
    private class StringTuple implements Tuple<String, String>
    {
        private String key;
        private String value;

        public StringTuple(String key, String value)
        {
            this.key = key;
            this.value = value;
        }
        @Override
        public String getKey()
        {
            return key;
        }
        @Override
        public String getValue()
        {
            return value;
        }
    }
}

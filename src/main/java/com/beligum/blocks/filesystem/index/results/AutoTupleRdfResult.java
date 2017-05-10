package com.beligum.blocks.filesystem.index.results;

import com.beligum.base.server.R;
import com.beligum.blocks.config.InputType;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.filesystem.index.ifaces.RdfTupleResult;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.vocabularies.XSD;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;

import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;

/**
 * An iterable key/value list of Strings, but contrary to StringTupleRdfResult,
 * this will interpret the label value, based on the type of RDF property.
 * <p>
 * Created by bram on 19/04/17.
 */
public class AutoTupleRdfResult implements RdfTupleResult<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfProperty property;
    private TupleQueryResult tupleQueryResult;
    private String labelBinding;
    private String valueBinding;

    //-----CONSTRUCTORS-----
    public AutoTupleRdfResult(RdfProperty property, TupleQueryResult tupleQueryResult, String labelBinding, String valueBinding)
    {
        this.property = property;
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
            Value val = bindings.getValue(this.valueBinding);

            String label = key == null ? null : key.stringValue();
            String value = val == null ? null : val.stringValue();

            //Note: this will do an extra beautification on the label where applicable
            retVal = new StringTuple(this.interpretLabel(label, value), value);
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
    private String interpretLabel(String rawLabel, String rawValue) throws NoSuchElementException
    {
        String retVal = rawLabel;

        if (retVal != null) {
            try {
                //if the value is a boolean, we want to return Yes/No instead of the raw true/false value
                if (this.property.getDataType().equals(XSD.BOOLEAN)) {
                    if (rawValue.equals("true")) {
                        retVal = core.Entries.toggleLabelYes.toString();
                    }
                    else if (rawValue.equals("false")) {
                        retVal = core.Entries.toggleLabelNo.toString();
                    }
                    else {
                        throw new IOException("Encountered unsupported boolean value; this shouldn't happen; " + rawValue);
                    }
                }
                else if (this.property.getWidgetType().equals(InputType.Enum)) {
                    //this translates the raw enum value to a translated label for the current request language
                    ResourceInfo res = this.property.getEndpoint().getResource(this.property, URI.create(rawValue), R.i18n().getOptimalLocale());
                    retVal = res.getLabel();
                }

            }
            catch (Exception e) {
                throw new NoSuchElementExceptionWithCause("Error while trying to build a pretty tuple label (" + rawLabel + "," + rawValue + ") for property " + this.property, e);
            }
        }

        return retVal;
    }

    /**
     * We extend the NoSuchElementException (of the next() method, see Iterable interface)
     * to be able to add a cause
     */
    public static class NoSuchElementExceptionWithCause extends NoSuchElementException
    {
        public NoSuchElementExceptionWithCause(String message, Throwable cause)
        {
            super(message);

            this.initCause(cause);
        }
    }
}

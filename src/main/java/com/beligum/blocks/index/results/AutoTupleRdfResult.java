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

import com.beligum.blocks.config.WidgetType;
import com.beligum.blocks.index.ifaces.IndexSearchResult;
import com.beligum.blocks.index.ifaces.RdfTupleResult;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.index.sparql.SparqlIndexSelectResult;
import com.beligum.blocks.index.sparql.SparqlSelectIndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.XSD;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.eclipse.rdf4j.model.Value;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.Locale;
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
    private Iterator<SparqlSelectIndexEntry> resultIterator;
    private String labelBinding;
    private String valueBinding;
    private Locale language;

    private DateTimeFormatter cachedDateFormatter;
    private DateTimeFormatter cachedTimeFormatter;
    private DateTimeFormatter cachedDateTimeFormatter;

    //-----CONSTRUCTORS-----
    public AutoTupleRdfResult(RdfProperty property, SparqlIndexSelectResult searchResult, String labelBinding, String valueBinding, Locale language)
    {
        this.property = property;
        this.resultIterator = searchResult.iterator();
        this.labelBinding = labelBinding;
        this.valueBinding = valueBinding;
        this.language = language;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext()
    {
        return this.resultIterator.hasNext();
    }
    @Override
    public Tuple<String, String> next()
    {
        Tuple<String, String> retVal = null;

        if (this.resultIterator != null && this.resultIterator.hasNext()) {

            SparqlSelectIndexEntry result = this.resultIterator.next();

            Value key = result.getBindingSet().getValue(this.labelBinding);
            Value val = result.getBindingSet().getValue(this.valueBinding);

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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String interpretLabel(String rawLabel, String rawValue) throws NoSuchElementException
    {
        String retVal = rawLabel;

        if (retVal != null) {
            try {
                //if the value is a boolean, we want to return Yes/No instead of the raw true/false value
                if (this.property.getDataType().equals(XSD.boolean_)) {
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
                else if (this.property.getDataType().equals(XSD.date)) {
                    //Note: local because we only support timezones in dateTime
                    TemporalAccessor value = DateTimeFormatter.ISO_LOCAL_DATE.parse(rawValue);
                    retVal = this.getDateFormatter().format(value);
                }
                else if (this.property.getDataType().equals(XSD.time)) {
                    //Note: local because we only support timezones in dateTime
                    TemporalAccessor value = DateTimeFormatter.ISO_LOCAL_TIME.parse(rawValue);
                    retVal = this.getTimeFormatter().format(value);
                }
                else if (this.property.getDataType().equals(XSD.dateTime)) {
                    TemporalAccessor value = DateTimeFormatter.ISO_DATE_TIME.parse(rawValue);
                    retVal = this.getDateTimeFormatter().format(value);
                }
                else if (this.property.getWidgetType().equals(WidgetType.Enum)) {
                    //this translates the raw enum value to a translated label for the current request language
                    ResourceProxy res = this.property.getEndpoint().getResource(this.property, UriBuilder.fromPath(rawValue).build(), this.language);
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

    private DateTimeFormatter getDateFormatter()
    {
        if (this.cachedDateFormatter == null) {
            //Note that we need at least MEDIUM or it won't be possible to distinguish between eg. 2010 and 1910
            this.cachedDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(this.language);
        }

        return this.cachedDateFormatter;
    }
    private DateTimeFormatter getTimeFormatter()
    {
        if (this.cachedTimeFormatter == null) {
            this.cachedTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(this.language);
        }

        return this.cachedTimeFormatter;
    }
    private DateTimeFormatter getDateTimeFormatter()
    {
        if (this.cachedDateTimeFormatter == null) {
            this.cachedDateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(this.language);
        }

        return this.cachedDateTimeFormatter;
    }
}

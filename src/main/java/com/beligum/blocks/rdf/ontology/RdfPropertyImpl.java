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

package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.InputType;
import com.beligum.blocks.config.InputTypeConfig;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import com.beligum.blocks.rdf.ontology.indexers.DefaultRdfPropertyIndexer;
import com.beligum.blocks.rdf.ontology.vocabularies.XSD;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/25/16.
 */
public class RdfPropertyImpl extends RdfClassImpl implements RdfProperty
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfClass dataType;
    private InputType widgetType;
    private InputTypeConfig widgetConfig;

    //-----CONSTRUCTORS-----
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType)
    {
        this(name, vocabulary, title, label, dataType, InputType.Undefined, null, null);
    }
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType,
                           InputType widgetType,
                           InputTypeConfig widgetConfig,
                           URI[] isSameAs)
    {
        this(name, vocabulary, title, label, dataType, widgetType, widgetConfig, isSameAs, false);
    }
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType,
                           InputType widgetType,
                           InputTypeConfig widgetConfig,
                           URI[] isSameAs,
                           boolean isPublic)
    {
        super(name, vocabulary, title, label, isSameAs, isPublic, null);

        this.widgetType = widgetType;
        //make it uniform; no nulls
        this.widgetConfig = widgetConfig == null ? new InputTypeConfig() : widgetConfig;
        this.dataType = dataType;

        //we don't have subclasses so don't worry about type checking (yet)
        vocabulary.addProperty(this);

        if (this.dataType == null) {
            Logger.error("Datatype of " + this.getName() + " (" + this.getFullName() + ") is null! This is a static-initializer bug and should be fixed");
        }
        else {
            //this is a double-check to make sure we accidently don't select the wrong inputtype for date/time
            if ((dataType.equals(XSD.DATE) && !widgetType.equals(InputType.Date))
                || (dataType.equals(XSD.TIME) && !widgetType.equals(InputType.Time))
                || (dataType.equals(XSD.DATE_TIME) && !widgetType.equals(InputType.DateTime))) {
                throw new RuntimeException("Encountered RDF property with datatype-inputtype mismatch; "+this);
            }
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.PROPERTY;
    }
    @Override
    public RdfClass getDataType()
    {
        return dataType;
    }
    @Override
    public InputType getWidgetType()
    {
        return widgetType;
    }
    @Override
    public InputTypeConfig getWidgetConfig()
    {
        return widgetConfig;
    }
    @Override
    public void setWidgetConfig(InputTypeConfig config)
    {
        this.widgetConfig = config;
    }
    @Override
    public void setEndpoint(RdfQueryEndpoint endpoint)
    {
        this.queryEndpoint = endpoint;
    }
    @Override
    public RdfIndexer.IndexResult indexValue(RdfIndexer indexer, URI resource, Value value, Locale language, RdfQueryEndpoint.SearchOption... options) throws IOException
    {
        return DefaultRdfPropertyIndexer.INSTANCE.index(indexer, resource, this, value, language, options);
    }
    @Override
    public Object prepareIndexValue(String value, Locale language) throws IOException
    {
        return DefaultRdfPropertyIndexer.INSTANCE.prepareIndexValue(this, value, language);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

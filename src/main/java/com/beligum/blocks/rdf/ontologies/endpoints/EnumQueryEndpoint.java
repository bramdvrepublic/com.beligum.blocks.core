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

package com.beligum.blocks.rdf.ontologies.endpoints;

import com.beligum.base.filesystem.ConstantsFileEntry;
import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.server.R;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfEndpoint;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 3/14/16.
 */
public class EnumQueryEndpoint implements RdfEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected RdfClass resourceType;
    protected Map<String, EnumSuggestion> suggestions;

    //-----CONSTRUCTORS-----
    public EnumQueryEndpoint(RdfClass resourceType)
    {
        this.resourceType = resourceType;
        this.suggestions = new LinkedHashMap<>();
    }
    public EnumQueryEndpoint(RdfClass resourceType, Set<MessagesFileEntry> suggestions)
    {
        this.resourceType = resourceType;
        this.suggestions = new LinkedHashMap<>(suggestions.size());
        for (MessagesFileEntry e : suggestions) {
            PropertiesEnumSuggestion sugg = new PropertiesEnumSuggestion(e);
            this.suggestions.put(sugg.getResource(), sugg);
        }
    }
    public EnumQueryEndpoint(RdfClass resourceType, Map<ConstantsFileEntry, MessagesFileEntry> suggestions)
    {
        this.resourceType = resourceType;
        this.suggestions = new LinkedHashMap<>(suggestions.size());
        for (Map.Entry<ConstantsFileEntry, MessagesFileEntry> e : suggestions.entrySet()) {
            PropertiesEnumSuggestion sugg = new PropertiesEnumSuggestion(e.getValue(), e.getKey());
            this.suggestions.put(sugg.getResource(), sugg);
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isExternal()
    {
        return false;
    }
    /**
     * Note that, for now, this method ignores queryType and options parameters
     */
    @Override
    public Iterable<ResourceProxy> search(RdfOntologyMember resourceType, String query, QueryType queryType, Locale language, int maxResults) throws IOException
    {
        Iterable<ResourceProxy> retVal = new ArrayList<>();

        if (this.resourceType.equals(resourceType)) {
            //Note: two little stunts to modify the returned language of the title, based on the passed-in language in this method
            if (!StringUtils.isEmpty(query)) {
                retVal = Arrays.asList(new ResourceProxy[] { new LangFilteredEnumSuggestion(this.getSuggestions().get(query), language) });
            }
            else {
                //if no specific query is passed, we return all values
                retVal = Iterables.transform(this.getSuggestions().values(),
                                             new Function<EnumSuggestion, ResourceProxy>()
                                             {
                                                 @Override
                                                 public ResourceProxy apply(EnumSuggestion from)
                                                 {
                                                     return new LangFilteredEnumSuggestion(from, language);
                                                 }
                                             }
                );
            }
        }

        return retVal;
    }
    /**
     * Note that we'll have to force this method a little bit, because the passed-in resourceIds will actually be enum values (that can be anything).
     * For an enum, it's handy to have this to translate values to their counterpart label (in a specific language), so use this method this way to
     * pass a lookup for a resourceId: UriBuilder.fromPath(rawValue).build()
     */
    @Override
    public ResourceProxy getResource(RdfOntologyMember resourceType, URI resourceId, Locale language) throws IOException
    {
        //Note: the getPath() converts special URI characters back to their native form
        return new LangFilteredEnumSuggestion(this.getSuggestions().get(resourceId.getPath()), language);
    }
    @Override
    public RdfProperty[] getLabelCandidates(RdfClass localResourceType)
    {
        //we're not a resource endpoint
        return new RdfProperty[0];
    }
    @Override
    public URI getExternalResourceId(URI resourceId, Locale language)
    {
        //nothing special to redirect to; we'll render the resource out ourselves
        return null;
    }
    @Override
    public Model getExternalRdfModel(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        //we're a local endpoint
        return null;
    }
    @Override
    public RdfClass getExternalClasses(RdfClass localResourceType)
    {
        //we're a local endpoint
        return null;
    }

    //-----PROTECTED METHODS-----
    //allows us to override this in subclasses
    protected Map<String, EnumSuggestion> getSuggestions()
    {
        return this.suggestions;
    }

    //-----PRIVATE METHODS-----
    public interface EnumSuggestion extends ResourceProxy
    {
        //two extra methods to be able to handle the language dynamically
        String getLabelFor(Locale lang);
        String getDescriptionFor(Locale lang);
    }

    public class SimpleEnumSuggestion implements EnumSuggestion
    {
        protected String title;
        protected String value;

        public SimpleEnumSuggestion()
        {
            this(null, null);
        }
        public SimpleEnumSuggestion(String title, String value)
        {
            this.title = title;
            this.value = value;
        }

        @Override
        public URI getUri()
        {
            return RdfTools.createRelativeResourceId(this.getTypeOf(), this.getResource());
        }
        //Note: for enums, this value will be used to fill the @content attribute on the html property element;
        // it's the formal 'value' part of the enum's AutocompleteSuggestion
        @Override
        public String getResource()
        {
            return value;
        }
        @Override
        public RdfClass getTypeOf()
        {
            return resourceType;
        }
        @Override
        public Locale getLanguage()
        {
            //Note: this will most likely be called from a AJAX call from the admin interface;
            // meaning we want to know the values matching the current resource/url, so use the referer language to generate the title
            //(although the value will probably be used, but we still might incorporate the title in the page html somewhere too...)
            return R.i18n().getOptimalRefererLocale();
        }
        @Override
        public boolean isExternal()
        {
            return false;
        }
        @Override
        public URI getParentUri()
        {
            return null;
        }
        @Override
        public String getLabel()
        {
            return this.getLabelFor(this.getLanguage());
        }
        @Override
        public String getDescription()
        {
            return null;
        }
        @Override
        public URI getImage()
        {
            return null;
        }
        @Override
        public String getLabelFor(Locale lang)
        {
            return this.title;
        }
        @Override
        public String getDescriptionFor(Locale lang)
        {
            return null;
        }
    }

    public class PropertiesEnumSuggestion extends SimpleEnumSuggestion
    {
        protected MessagesFileEntry title;
        protected ConstantsFileEntry value;

        public PropertiesEnumSuggestion()
        {
            this(null, null);
        }
        public PropertiesEnumSuggestion(MessagesFileEntry title)
        {
            this(title, null);
        }
        public PropertiesEnumSuggestion(MessagesFileEntry title, ConstantsFileEntry value)
        {
            this.title = title;
            this.value = value;
        }

        @Override
        public String getLabel()
        {
            //Note: this will most likely be called from a AJAX call from the admin interface;
            // meaning we want to know the values matching the current resource/url, so use the referer language to generate the title
            //(although the value will probably be used, but we still might incorporate the title in the page html somewhere too...)
            return this.getLabelFor(R.i18n().getOptimalRefererLocale());
        }
        //check out the constructors above: if the value is null, we're supposed to take the title instead
        @Override
        public String getResource()
        {
            return this.value != null ? this.value.getValue() : this.getLabel();
        }
        @Override
        public String getLabelFor(Locale lang)
        {
            return this.title == null ? null : this.title.toString(lang);
        }
    }

    /**
     * This is a helper class to be able to choose the locale of the returned value on-the-fly (see above)
     */
    public class LangFilteredEnumSuggestion implements ResourceProxy
    {
        private EnumSuggestion suggestion;
        private Locale language;

        public LangFilteredEnumSuggestion(EnumSuggestion suggestion, Locale language)
        {
            this.suggestion = suggestion;
            this.language = language;
        }

        @Override
        public URI getUri()
        {
            return RdfTools.createRelativeResourceId(this.getTypeOf(), this.getResource());
        }
        @Override
        public RdfClass getTypeOf()
        {
            return this.suggestion == null ? null : this.suggestion.getTypeOf();
        }
        @Override
        public Locale getLanguage()
        {
            return this.language;
        }
        @Override
        public boolean isExternal()
        {
            return false;
        }
        @Override
        public URI getParentUri()
        {
            return null;
        }
        @Override
        public String getResource()
        {
            return this.suggestion == null ? null : this.suggestion.getResource();
        }
        @Override
        public String getLabel()
        {
            return this.suggestion == null ? null : this.suggestion.getLabelFor(this.language);
        }
        @Override
        public String getDescription()
        {
            return this.suggestion == null ? null : this.suggestion.getDescriptionFor(this.language);
        }
        @Override
        public URI getImage()
        {
            return null;
        }
    }
}

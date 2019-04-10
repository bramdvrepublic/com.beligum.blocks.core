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
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.ifaces.RdfEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.local.WrappedSuggestionResourceInfo;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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
    protected Map<String, EnumAutocompleteSuggestion> suggestions;

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
            this.suggestions.put(sugg.getValue(), sugg);
        }
    }
    public EnumQueryEndpoint(RdfClass resourceType, Map<ConstantsFileEntry, MessagesFileEntry> suggestions)
    {
        this.resourceType = resourceType;
        this.suggestions = new LinkedHashMap<>(suggestions.size());
        for (Map.Entry<ConstantsFileEntry, MessagesFileEntry> e : suggestions.entrySet()) {
            PropertiesEnumSuggestion sugg = new PropertiesEnumSuggestion(e.getValue(), e.getKey());
            this.suggestions.put(sugg.getValue(), sugg);
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
    public Collection<ResourceProxy> search(RdfOntologyMember resourceType, String query, QueryType queryType, Locale language, int maxResults) throws IOException
    {
        Collection<ResourceProxy> retVal = new ArrayList<>();

        if (this.resourceType.equals(resourceType)) {
            //Note: two little stunts to modify the returned language of the title, based on the passed-in language in this method
            if (!StringUtils.isEmpty(query)) {
                retVal = Arrays.asList(new AutocompleteSuggestion[] { new LangFilteredEnumSuggestion(this.getSuggestions().get(query), language) });
            }
            else {
                retVal = Collections2.transform(this.getSuggestions().values(), new Function<EnumAutocompleteSuggestion, AutocompleteSuggestion>()
                {
                    public AutocompleteSuggestion apply(EnumAutocompleteSuggestion from)
                    {
                        return new LangFilteredEnumSuggestion(from, language);
                    }
                });
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
        return new WrappedSuggestionResourceInfo(this.getSuggestions().get(resourceId.getPath()), language);
    }
    @Override
    public RdfProperty[] getLabelCandidates(RdfClass localResourceType)
    {
        //we're no resource endpoint
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
    protected Map<String, EnumAutocompleteSuggestion> getSuggestions()
    {
        return this.suggestions;
    }

    //-----PRIVATE METHODS-----
    public interface EnumAutocompleteSuggestion extends AutocompleteSuggestion
    {
        //two extra methods to be able to handle the language dynamically
        String getTitleFor(Locale lang);

        String getSubTitleFor(Locale lang);
    }

    public class SimpleEnumSuggestion implements EnumAutocompleteSuggestion
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

        //Note: for enums, this value will be used to fill the @content attribute on the html property element;
        // it's the formal 'value' part of the enum's AutocompleteSuggestion
        @Override
        public String getValue()
        {
            return value;
        }
        @Override
        public URI getResourceType()
        {
            return resourceType.getCurieName();
        }
        @Override
        public URI getPublicPage()
        {
            return null;
        }
        @Override
        public String getTitle()
        {
            //Note: this will most likely be called from a AJAX call from the admin interface;
            // meaning we want to know the values matching the current resource/url, so use the referer language to generate the title
            //(although the value will probably be used, but we still might incorporate the title in the page html somewhere too...)
            return this.getTitleFor(R.i18n().getOptimalRefererLocale());
        }
        @Override
        public String getTitleFor(Locale lang)
        {
            return this.title;
        }
        @Override
        public String getSubTitle()
        {
            return null;
        }
        @Override
        public String getSubTitleFor(Locale lang)
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

        //check out the constructors above: if the value is null, we're supposed to take the title instead
        @Override
        public String getValue()
        {
            return this.value != null ? this.value.getValue() : this.getTitle();
        }
        @Override
        public String getTitle()
        {
            //Note: this will most likely be called from a AJAX call from the admin interface;
            // meaning we want to know the values matching the current resource/url, so use the referer language to generate the title
            //(although the value will probably be used, but we still might incorporate the title in the page html somewhere too...)
            return this.getTitleFor(R.i18n().getOptimalRefererLocale());
        }
        @Override
        public String getTitleFor(Locale lang)
        {
            return this.title == null ? null : this.title.toString(lang);
        }
    }

    /**
     * This is a helper class to be able to choose the locale of the returned value on-the-fly (see above)
     */
    public class LangFilteredEnumSuggestion implements AutocompleteSuggestion
    {
        private EnumAutocompleteSuggestion suggestion;
        private Locale language;
        public LangFilteredEnumSuggestion(EnumAutocompleteSuggestion suggestion, Locale language)
        {
            this.suggestion = suggestion;
            this.language = language;
        }

        @Override
        public String getValue()
        {
            return this.suggestion == null ? null : this.suggestion.getValue();
        }
        @Override
        public URI getResourceType()
        {
            return this.suggestion == null ? null : this.suggestion.getResourceType();
        }
        @Override
        public URI getPublicPage()
        {
            return null;
        }
        @Override
        public String getTitle()
        {
            return this.suggestion == null ? null : this.suggestion.getTitleFor(this.language);
        }
        @Override
        public String getSubTitle()
        {
            return this.suggestion == null ? null : this.suggestion.getSubTitleFor(this.language);
        }
    }
}

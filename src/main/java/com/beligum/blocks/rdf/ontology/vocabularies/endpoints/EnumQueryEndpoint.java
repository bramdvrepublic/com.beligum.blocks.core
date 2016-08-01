package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

import com.beligum.base.filesystem.ConstantsFileEntry;
import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.server.R;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 3/14/16.
 */
public class EnumQueryEndpoint implements RdfQueryEndpoint
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
    /**
     * Note that, for now, this method ignores queryType and options parameters
     */
    @Override
    public Collection<AutocompleteSuggestion> search(RdfClass resourceType, String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException
    {
        Collection<AutocompleteSuggestion> retVal = new ArrayList<>();

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
    @Override
    public ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        throw new NotImplementedException();
    }
    @Override
    public URI getExternalResourceRedirect(URI resourceId, Locale language)
    {
        //nothing special to redirect to; we'll render the resource out ourselves
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
        public String getTitle()
        {
            //Note: this will most likely be called from a AJAX call from the admin interface;
            // meaning we want to know the values matching the current resource/url, so use the referer language to generate the title
            //(although the value will probably be used, but we still might incorporate the title in the page html somewhere too...)
            return this.getTitleFor(R.i18nFactory().getOptimalRefererLocale());
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
        private MessagesFileEntry title;

        public PropertiesEnumSuggestion(MessagesFileEntry entry)
        {
            this(entry, null);
        }
        public PropertiesEnumSuggestion(MessagesFileEntry title, ConstantsFileEntry value)
        {
            this.title = title;
            this.value = value == null ? null : value.getValue();
        }

        //check out the constructors above: if the value is null, we're supposed to take the title instead
        @Override
        public String getValue()
        {
            return value != null ? value : this.getTitle();
        }
        @Override
        public String getTitleFor(Locale lang)
        {
            return this.title == null ? null : com.beligum.base.i18n.I18nFactory.instance().getResourceBundle(lang).get(this.title);
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

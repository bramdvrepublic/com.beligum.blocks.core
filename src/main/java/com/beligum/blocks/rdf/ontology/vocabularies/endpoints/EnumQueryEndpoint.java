package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfClass;
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
    private final Map<String, MessagesFileEntry> entries;

    //-----CONSTRUCTORS-----
    public EnumQueryEndpoint(Set<MessagesFileEntry> entries)
    {
        this.entries = new LinkedHashMap<>();
        for (MessagesFileEntry e : entries) {
            this.entries.put(e.getCanonicalKey(), e);
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public List<AutocompleteSuggestion> search(RdfClass resourceType, String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        if (!StringUtils.isEmpty(query)) {
            MessagesFileEntry msg = this.entries.get(query);
            if (msg!=null) {
                retVal.add(new MessageSuggestion(resourceType, msg));
            }
        }
        else {
            for (Map.Entry<String, MessagesFileEntry> e : this.entries.entrySet()) {
                retVal.add(new MessageSuggestion(resourceType, e.getValue()));
            }
        }

        return retVal;
    }
    @Override
    public ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        ResourceInfo retVal = null;

        return retVal;
    }
    @Override
    public URI getExternalResourceRedirect(URI resourceId, Locale language)
    {
        //nothing special to redirect to; we'll render the resource out ourselves
        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    public static class MessageSuggestion implements AutocompleteSuggestion
    {
        private RdfClass resourceType;
        private MessagesFileEntry entry;

        public MessageSuggestion(RdfClass resourceType, MessagesFileEntry entry)
        {
            this.resourceType = resourceType;
            this.entry = entry;
        }

        @Override
        public URI getResourceId()
        {
            return URI.create(this.entry.getCanonicalKey());
        }
        @Override
        public URI getResourceType()
        {
            return this.resourceType.getCurieName();
        }
        @Override
        public String getTitle()
        {
            return this.entry.getI18nValue();
        }
        @Override
        public String getSubTitle()
        {
            return null;
        }
    }
}

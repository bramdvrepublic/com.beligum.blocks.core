package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.server.R;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.resources.ResourceIndexer;
import com.beligum.blocks.filesystem.index.entries.resources.SimpleResourceIndexer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;
import java.util.Set;

/**
 * Created by bram on 2/27/16.
 */
public class RdfClassImpl extends AbstractRdfResourceImpl implements RdfClass
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;
    private RdfVocabulary vocabulary;
    private MessagesFileEntry title;
    private MessagesFileEntry label;
    private URI[] isSameAs;
    //we need to be able to set this from the RdfProperty interface (to make static initializing possible)
    protected RdfQueryEndpoint queryEndpoint;
    private Set<RdfProperty> properties;
    private ResourceIndexer resourceIndexer;

    //-----CONSTRUCTORS-----
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label)
    {
        this(name, vocabulary, title, label, null, false, null, null);
    }
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs)
    {
        this(name, vocabulary, title, label, isSameAs, false, null, null);
    }
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs,
                        boolean isPublic,
                        RdfQueryEndpoint queryEndpoint)
    {
        this(name, vocabulary, title, label, isSameAs, isPublic, queryEndpoint, null);
    }
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs,
                        boolean isPublic,
                        RdfQueryEndpoint queryEndpoint,
                        ResourceIndexer resourceIndexer)
    {
        super(isPublic);

        this.name = name;
        this.vocabulary = vocabulary;
        this.title = title;
        this.label = label;
        //make it uniform (always an array)
        this.isSameAs = isSameAs == null ? new URI[] {} : isSameAs;
        this.queryEndpoint = queryEndpoint;
        this.properties = null;
        this.resourceIndexer = resourceIndexer;
        //revert to default if null (this behaviour is expected in com.beligum.blocks.fs.index.entries.pages.SimplePageIndexEntry)
        if (this.resourceIndexer == null) {
            this.resourceIndexer = new SimpleResourceIndexer();
        }

        //only add ourself to the selected vocabulary if we are a pure class
        if (this.vocabulary != null && this.getClass().equals(RdfClassImpl.class)) {
            this.vocabulary.addClass(this);
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public RdfVocabulary getVocabulary()
    {
        return vocabulary;
    }
    @Override
    public URI getFullName()
    {
        return this.vocabulary == null ? null : vocabulary.resolve(name);
    }
    @Override
    public URI getCurieName()
    {
        return this.vocabulary == null ? null : URI.create(vocabulary.getPrefix() + ":" + name);
    }
    @Override
    public String getTitleKey()
    {
        return title.getCanonicalKey();
    }
    @Override
    public String getTitle()
    {
        //Note: we can't return the regular optimal locale, because this will probably be called from an admin endpoint
        return this.title == null ? null : this.title.toString(R.i18n().getOptimalRefererLocale());
    }
    @Override
    public MessagesFileEntry getTitleMessage()
    {
        return title;
    }
    @Override
    public String getLabelKey()
    {
        return label.getCanonicalKey();
    }
    @Override
    public String getLabel()
    {
        //Note: we can't return the regular optimal locale, because this will probably be called from an admin endpoint
        return this.label == null ? null : this.label.toString(R.i18n().getOptimalRefererLocale());
    }
    @Override
    public MessagesFileEntry getLabelMessage()
    {
        return label;
    }
    @Override
    public URI[] getIsSameAs()
    {
        return isSameAs;
    }
    @Override
    public RdfQueryEndpoint getEndpoint()
    {
        return queryEndpoint;
    }
    @Override
    public Set<RdfProperty> getProperties()
    {
        return properties;
    }
    @Override
    public void setProperties(Set<RdfProperty> properties)
    {
        this.properties = properties;
    }
    @Override
    public ResourceIndexer getResourceIndexer()
    {
        return resourceIndexer;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "" + this.getCurieName();
    }
}

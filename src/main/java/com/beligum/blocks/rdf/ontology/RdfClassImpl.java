package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

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
    private RdfQueryEndpoint queryEndpoint;
    private RdfProperty[] properties;

    //-----CONSTRUCTORS-----
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
                        RdfQueryEndpoint queryEndpoint,
                        RdfProperty[] properties)
    {
        super(isPublic);

        this.name = name;
        this.vocabulary = vocabulary;
        this.title = title;
        this.label = label;
        //make it uniform (always an array)
        this.isSameAs = isSameAs == null ? new URI[] {} : isSameAs;
        this.queryEndpoint = queryEndpoint;
        this.properties = properties;

        //only add ourself to the selected vocabulary if we are a pure class
        if (this.getClass().equals(RdfClassImpl.class)) {
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
        return vocabulary.getNamespace().resolve(name);
    }
    @Override
    public URI getCurieName()
    {
        return URI.create(vocabulary.getPrefix() + ":" + name);
    }
    @Override
    public String getTitle()
    {
        return title.getI18nValue();
    }
    @Override
    public String getLabel()
    {
        return label.getI18nValue();
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
    public RdfProperty[] getProperties()
    {
        return properties;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return ""+this.getCurieName();
    }
}

package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.models.AbstractJsonObject;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.net.URI;

/**
 * Created by bram on 2/27/16.
 */
public class RdfClassImpl extends AbstractJsonObject implements RdfClass
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;
    private URI vocabulary;
    private String vocabularyPrefix;
    private MessagesFileEntry title;
    private MessagesFileEntry label;
    private URI[] isSameAs;

    //-----CONSTRUCTORS-----
    public RdfClassImpl(String name,
                        URI vocabulary,
                        String vocabularyPrefix,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs)
    {
        this.name = name;
        this.vocabulary = vocabulary;
        this.vocabularyPrefix = vocabularyPrefix;
        this.title = title;
        this.label = label;
        //make it uniform (always an array)
        this.isSameAs = isSameAs == null ? new URI[] {} : isSameAs;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public URI getVocabulary()
    {
        return vocabulary;
    }
    @Override
    public String getVocabularyPrefix()
    {
        return vocabularyPrefix;
    }
    @Override
    public URI getFullName()
    {
        return vocabulary.resolve(name);
    }
    @Override
    public URI getCurieName()
    {
        return URI.create(vocabularyPrefix+":"+name);
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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

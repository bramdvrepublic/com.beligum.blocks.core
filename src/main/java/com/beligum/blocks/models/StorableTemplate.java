package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.interfaces.BlocksVersionedStorable;
import com.beligum.blocks.parsers.ElementParser;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 17/03/15.
 */
public abstract class StorableTemplate extends BasicTemplate implements BlocksVersionedStorable
{

    private Long documentVersion;
    private String applicationVersion;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;

    public StorableTemplate() {
    }


    public StorableTemplate(Element node, String language) throws ParseException
    {
        super(node, language);
        String reference = ElementParser.getReferenceTo((Element) node);
        if (reference != null) {
            this.setId(Blocks.factory().getIdForString(reference));
        }
    }


    @Override
    public Long getDocumentVersion()
    {
        return this.documentVersion;
    }

    @Override
    public void setDocumentVersion(Long documentVersion)
    {
        this.documentVersion = documentVersion;
    }

    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }

    @Override
    public void setApplicationVersion(String applicationVersion)
    {
        this.applicationVersion = applicationVersion;
    }

    /**
     * @return the creator of this storable
     */
    @Override
    public String getCreatedBy()
    {
        return this.createdBy;
    }

    @Override
    public void setCreatedBy(String created_by)
    {
        this.createdBy = created_by;
    }
    /**
     * @return the updater of this storable
     */
    @Override
    public String getUpdatedBy()
    {
        return this.updatedBy;
    }
    @Override
    public void setUpdatedBy(String updated_by)
    {
        this.updatedBy = updated_by;
    }
    /**
     * @return the moment of creation of this storable
     */
    @Override
    public String getCreatedAt()
    {
        return createdAt;
    }
    @Override
    public void setCreatedAt(String createdAt)
    {
        this.createdAt = createdAt;
    }
    /**
     * @return the moment of last update of this storable
     */
    @Override
    public String getUpdatedAt()
    {
        return updatedAt;
    }
    @Override
    public void setUpdatedAt(String updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    @Override
    public String getLanguage()
    {
        return language;
    }
    @Override
    public void setLanguage(String language)
    {
        this.language = language;
    }

}

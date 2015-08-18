package com.beligum.blocks.models.sql;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.models.WebPageImpl;
import com.beligum.blocks.models.interfaces.WebPage;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.persistence.*;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by wouter on 30/06/15.
 */
@Entity
@Table(name="page")
public class DBPage extends DBDocumentInfo
{


    private String blockId;

    private String masterPage;

    @Lob
    private String webPage;

    private String language;


    // Default constructor for Hibernate
    public DBPage() {

    }

    public DBPage(WebPage webPage) throws JsonProcessingException
    {
        this.setWebPage(webPage);
    }

    public Long getId() {
        return this.id;
    }

    public void setWebPage(WebPage webPage) throws JsonProcessingException
    {
        this.blockId = webPage.getBlockId().toString();
        this.language = webPage.getLanguage().getLanguage();
        this.masterPage = webPage.getMasterpageId().toString();
        this.webPage = webPage.toJson();
    }

    public WebPage getWebPage() throws IOException
    {
        return WebPageImpl.pageMapper.readValue(this.webPage, WebPage.class);
    }

    public Locale getLanguage() {
        Locale retVal = BlocksConfig.instance().getLocaleForLanguage(this.language);
        if (retVal == null) {
            retVal = new Locale(this.language);
        }
        return retVal;
    }

}

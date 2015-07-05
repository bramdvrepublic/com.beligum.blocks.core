package com.beligum.blocks.resources.sql;

import com.beligum.base.models.BasicModelImpl;
import com.beligum.base.utils.json.JsonObjectIdResolver;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.pages.WebPageImpl;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.interfaces.DocumentInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import java.io.IOException;
import java.util.Date;
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

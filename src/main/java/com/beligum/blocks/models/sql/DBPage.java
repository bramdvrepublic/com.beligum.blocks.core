package com.beligum.blocks.models.sql;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.models.WebPageImpl;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
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


    @Lob
    private String data;



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
        this.data = ResourceFactoryImpl.instance().serializeWebpage(webPage, true);
    }

    public WebPage getWebPage() throws IOException
    {
        return ResourceFactoryImpl.instance().deserializeWebpage(this.data.getBytes(), Locale.ROOT);
    }


}

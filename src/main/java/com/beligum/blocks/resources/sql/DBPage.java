package com.beligum.blocks.resources.sql;

import com.beligum.base.models.BasicModelImpl;
import com.beligum.base.utils.json.JsonObjectIdResolver;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.interfaces.DocumentInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wouter on 30/06/15.
 */
@Entity
@Table(name="page")
public class DBPage extends DBDocumentInfo
{

    private String blockId;

    private String masterPage;

    private String pageTemplate;

    @Lob
    private String html;

    private String language;

    @Lob
    private byte[] pageResource;

    // Default constructor for Hibernate
    public DBPage() {

    }

    public DBPage(WebPage webPage) {
        this.blockId = webPage.getBlockId().toString();
        this.pageTemplate = webPage.getPageTemplate();
        this.html = webPage.getParsedHtml();
        this.language = webPage.getLanguage().getLanguage();
        this.masterPage = webPage.getMasterpageId().toString();
    }

    public Long getId() {
        return this.id;
    }

    public String getHtml() {
        return html;
    }

    public void setWebPage(WebPage webPage) {
        this.blockId = webPage.getBlockId().toString();
        this.pageTemplate = webPage.getPageTemplate();
        this.html = webPage.getParsedHtml();
        this.language = webPage.getLanguage().getLanguage();
        this.masterPage = webPage.getMasterpageId().toString();
    }
}

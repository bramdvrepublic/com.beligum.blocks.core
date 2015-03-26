package com.beligum.blocks.core.models;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import org.jsoup.nodes.Element;

import java.net.URL;

/**
 * Created by wouter on 16/03/15.
 */
public abstract class StoredTemplate extends StorableTemplate
{

    protected String pageTemplateName;
    protected String pageTitle;

    public StoredTemplate() {
        super();
    }

    public StoredTemplate(Element node, String language) throws ParseException
    {
        super(node, language);
    }


    public StoredTemplate(Element node, URL url) throws ParseException
    {
        this(node, Blocks.urlDispatcher().getLanguage(url));
        this.setId(Blocks.factory().getIdForString(Blocks.urlDispatcher().findId(url)));


    }


    public void setLanguage(String language) {
        this.language = language;
    }

    protected String findPageTemplateName() {
        String retVal = ParserConstants.DEFAULT_PAGE_TEMPLATE;
        Blueprint blueprint = this.getBlueprint();
        if (this.getBlueprintName() != null && blueprint != null) {
            this.getBlueprint().getPageTemplateName();
        }
        return retVal;
    }

    public String getPageTemplateName() {
        if (this.pageTemplateName == null) {
            this.pageTemplateName = findPageTemplateName();
        }
        return this.pageTemplateName;
    }

    public String getPageTitle() {
        String retVal = Blocks.config().getDefaultPageTitle();
        if (this.pageTitle != null) {
            retVal = this.pageTitle;
        }
        return pageTitle;
    }
}

package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.URLMapping.simple.UrlDispatcher;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import org.jsoup.nodes.Element;

import javax.mail.Store;
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
        this(node, BlocksConfig.getInstance().getUrlDispatcher().getLanguage(url));
        this.setId(BlocksConfig.getInstance().getDatabase().getIdForString(BlocksConfig.getInstance().getUrlDispatcher().findId(url)));


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
        String retVal = BlocksConfig.getDefaultPageTitle();
        if (this.pageTitle != null) {
            retVal = this.pageTitle;
        }
        return pageTitle;
    }
}

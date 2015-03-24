package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.URLMapping.simple.UrlDispatcher;
import com.beligum.blocks.core.config.BlocksConfig;
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
}

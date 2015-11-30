package com.beligum.blocks.controllers.interfaces;

import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.interfaces.WebPath;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 5/06/15.
 */
public interface PersistenceController
{
    public static final String RESOURCE_CLASS = "resource";
    public static final String PATH_CLASS = "path";
    public static final String WEB_PAGE_CLASS = "webpage";
    public static final String MASTER_WEB_PAGE_CLASS = "masterwebpage";
    public static final String RESOURCE_TYPE_FIELD = "@type";

    public WebPage getWebPage(URI masterWebPage, Locale locale) throws IOException;

    ;

    public void deleteWebPage(URI masterPage) throws Exception;

    ;

    public WebPage saveWebPage(WebPage webPage, boolean doVersion) throws Exception;

    ;

    public Resource getResource(URI id, Locale language) throws Exception;

    public Resource saveResource(Resource resource) throws Exception;

    public Resource deleteResource(Resource resource) throws Exception;

    ;

    // returns a webnode (containing all urls and redirects for this page)
    public Map<String, WebPath> getPaths(URI masterPage);

    public Map<String, WebPath> getLanguagePaths(String pathName);

    // returns a webnode (containing all urls and redirects for this page)
    public WebPath getPath(Path path, Locale locale);

    public WebPath savePath(WebPath path) throws Exception;

}

package com.beligum.blocks.routing.ifaces;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebNode
{
    public Integer getStatusCode();
    public URI getPageUrl();

    public void setStatusCode(Integer statusCode);
    public void setPageUrl(URI pageUrl);

    public WebPath getChildPath(String name, Locale locale);
    public WebPath getParentPath(String name, Locale locale);

    public boolean isNotFound();
    public boolean isPage();
    public boolean isRedirect();

}

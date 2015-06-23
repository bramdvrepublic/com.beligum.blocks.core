package com.beligum.blocks.routing.ifaces;

import com.beligum.blocks.resources.interfaces.DocumentInfo;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebNode extends DocumentInfo
{
    public final Integer NOT_FOUND = 404;
    public final Integer REDIRECT = 303;
    public final Integer OK = 200;


    public Integer getStatusCode();
    public URI getPageUrl();

    public void setPageOk(URI pageUrl);
    public void setPageRedirect(URI pageUrl);
    public void setPageNotFound();

    public WebPath getChildPath(String name, Locale locale);
    public WebPath getParentPath(String name, Locale locale);

    public boolean isNotFound();
    public boolean isPage();
    public boolean isRedirect();

}

package com.beligum.blocks.routing.ifaces.nodes;

import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebNode
{
    public Integer getStatusCode();
    public String getPageUrl();

    public WebPath getChildPath(String name, Locale locale);
    public WebPath getParentPath(String name, Locale locale);


}

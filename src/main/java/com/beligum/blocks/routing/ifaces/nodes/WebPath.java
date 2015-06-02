package com.beligum.blocks.routing.ifaces.nodes;

import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPath
{
    public String getName(Locale locale);
    public void setName(String name, Locale locale);
    public WebNode getParentWebNode();
    public WebNode getChildWebNode();
}

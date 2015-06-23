package com.beligum.blocks.routing.ifaces;

import com.beligum.blocks.resources.interfaces.DocumentInfo;

import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPath extends DocumentInfo
{
    public String getName(Locale locale);
    public void setName(String name, Locale locale);
    public WebNode getParentWebNode();
    public WebNode getChildWebNode();
}

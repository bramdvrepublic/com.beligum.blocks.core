package com.beligum.blocks.pages.ifaces;

import com.beligum.blocks.resources.interfaces.DocumentInfo;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Created by wouter on 19/06/15.
 */
public interface MasterWebPage extends DocumentInfo
{

    public URI getBlockId();

    public Set<Locale> getLanguages();

    public Locale getDefaultLanguage();

    public WebPage getPageForLocale(Locale locale);
}

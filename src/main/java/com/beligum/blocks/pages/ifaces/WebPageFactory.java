package com.beligum.blocks.pages.ifaces;

import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPageFactory
{

    public WebPage createPage(Locale language);

    public WebPage createPage(String id, Locale language);

    public WebPage get(String id, Locale language);

}

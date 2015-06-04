package com.beligum.blocks.pages.ifaces;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 */
public interface WebPageController
{

    public WebPage createPage(Locale language);

    public WebPage createPage(URI id, Locale language);

    public WebPage get(String id, Locale language);

    public String render(URI uri);

    public String render(WebPage webPage);

    public WebPage save(URI uri, String html) throws Exception;

    public WebPage delete(WebPage webPage);

}

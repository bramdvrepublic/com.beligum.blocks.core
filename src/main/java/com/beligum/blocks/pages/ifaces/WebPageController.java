package com.beligum.blocks.pages.ifaces;

import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 */
public interface WebPageController
{
    public String show(WebPage webPage);

    public WebPage save(WebPage webPage);

    public WebPage delete(WebPage webPage);

}

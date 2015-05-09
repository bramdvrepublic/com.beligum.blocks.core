package com.beligum.blocks.templating.webcomponents.html5.ifaces;

/**
 * Created by bram on 5/7/15.
 */
public interface Html extends Cloneable
{
    String getHtml();
    Iterable<HtmlElement> select(String selector);
    Object getDelegateObject();
}

package com.beligum.blocks.templating.webcomponents.html5.ifaces;

/**
 * Created by bram on 5/7/15.
 */
public interface HtmlElement extends Html
{
    String getTagName();
    String getAttribute(String name);
    Iterable<HtmlAttribute> getAttributes();
    void replaceWith(HtmlElement replaceElement);
}

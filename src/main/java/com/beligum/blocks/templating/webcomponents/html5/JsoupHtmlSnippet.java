package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlElement;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlSnippet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Created by bram on 5/7/15.
 */
public class JsoupHtmlSnippet extends AbstractJsoupWrapper implements HtmlSnippet
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected Document jsoupDocument;

    //-----CONSTRUCTORS-----
    public JsoupHtmlSnippet(Document jsoupDocument)
    {
        this.jsoupDocument = jsoupDocument;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Iterable<HtmlElement> select(String selector)
    {
        return new WrappedJsoupIterator<Element, HtmlElement>(this.jsoupDocument.select(selector).iterator())
        {
            @Override
            public HtmlElement wrapNext(Element element)
            {
                return new JsoupHtmlElement(element);
            }
        };
    }
    @Override
    protected Node getJsoupNode()
    {
        return this.jsoupDocument;
    }
    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
}

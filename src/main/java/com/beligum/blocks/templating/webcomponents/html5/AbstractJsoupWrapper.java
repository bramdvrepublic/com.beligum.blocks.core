package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.blocks.templating.webcomponents.html5.ifaces.Html;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlElement;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.Collections;
import java.util.Iterator;

/**
 * Created by bram on 5/9/15.
 */
public abstract class AbstractJsoupWrapper implements Html
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public Object getDelegateObject()
    {
        return this.getJsoupNode();
    }
    @Override
    public String getHtml()
    {
        String retVal = null;
        if (this.getJsoupNode()!=null) {
            retVal = this.getJsoupNode().outerHtml();
        }

        return retVal;
    }
    @Override
    public Iterable<HtmlElement> select(String selector)
    {
        Iterable<HtmlElement> retVal = null;

        Node jsoupNode = getJsoupNode();
        if (jsoupNode != null && jsoupNode instanceof Element) {
            return new WrappedJsoupIterator<Element, HtmlElement>(((Element)jsoupNode).select(selector).iterator())
            {
                @Override
                public HtmlElement wrapNext(Element element)
                {
                    return new JsoupHtmlElement(element);
                }
            };
        }

        if (retVal==null) {
            retVal = new Iterable<HtmlElement>()
            {
                @Override
                public Iterator<HtmlElement> iterator()
                {
                    return Collections.emptyIterator();
                }
            };
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----
    protected abstract Node getJsoupNode();

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "" + this.getJsoupNode();
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof AbstractJsoupWrapper))
            return false;

        AbstractJsoupWrapper that = (AbstractJsoupWrapper) o;

        return !(this.getJsoupNode() != null ? !this.getJsoupNode().equals(that.getJsoupNode()) : that.getJsoupNode() != null);

    }
    @Override
    public int hashCode()
    {
        return this.getJsoupNode() != null ? this.getJsoupNode().hashCode() : 0;
    }
}

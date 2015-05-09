package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlAttribute;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlElement;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Created by bram on 5/7/15.
 */
public class JsoupHtmlElement extends AbstractJsoupWrapper implements HtmlElement
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected Element jsoupNode;

    //-----CONSTRUCTORS-----
    public JsoupHtmlElement(Element jsoupElement)
    {
        this.jsoupNode = jsoupElement;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getTagName()
    {
        return this.jsoupNode.tagName();
    }
    @Override
    public String getAttribute(String name)
    {
        return this.jsoupNode.attributes().get(name);
    }
    @Override
    public Iterable<HtmlAttribute> getAttributes()
    {
        return new WrappedJsoupIterator<Attribute, HtmlAttribute>(this.jsoupNode.attributes().iterator())
        {
            @Override
            public HtmlAttribute wrapNext(Attribute attribute)
            {
                return new JsoupHtmlAttribute(attribute);
            }
        };
    }
    @Override
    public void replaceWith(HtmlElement replaceElement)
    {
        this.jsoupNode.replaceWith((Node) replaceElement.getDelegateObject());
    }

    //-----PROTECTED METHODS-----
    @Override
    protected Node getJsoupNode()
    {
        return this.jsoupNode;
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----

}

package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlAttribute;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Node;

/**
 * Created by bram on 5/9/15.
 */
public class JsoupHtmlAttribute extends AbstractJsoupWrapper implements HtmlAttribute
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Attribute jsoupAttribute;

    //-----CONSTRUCTORS-----
    public JsoupHtmlAttribute(Attribute jsoupAttribute)
    {
        this.jsoupAttribute = jsoupAttribute;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return this.jsoupAttribute.getKey();
    }
    @Override
    public String getValue()
    {
        return this.jsoupAttribute.getValue();
    }
    @Override
    public Object getDelegateObject()
    {
        return this.jsoupAttribute;
    }
    @Override
    protected Node getJsoupNode()
    {
        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

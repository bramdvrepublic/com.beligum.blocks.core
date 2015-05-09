package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlExternalScriptElement;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlScriptElement;
import org.jsoup.nodes.Element;

/**
 * Created by bram on 5/8/15.
 */
public class JsoupHtmlScriptElement extends JsoupHtmlElement implements HtmlScriptElement, HtmlExternalScriptElement
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public JsoupHtmlScriptElement(Element element)
    {
        super(element);

        //external script or not?
        if (element.hasAttr("src")) {

        }
        else {

        }
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

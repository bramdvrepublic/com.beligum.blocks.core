package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlExternalStyleElement;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlStyleElement;
import org.jsoup.nodes.Element;

/**
 * Created by bram on 5/8/15.
 */
public class JsoupHtmlStyleElement extends JsoupHtmlElement implements HtmlStyleElement, HtmlExternalStyleElement
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public JsoupHtmlStyleElement(Element element)
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

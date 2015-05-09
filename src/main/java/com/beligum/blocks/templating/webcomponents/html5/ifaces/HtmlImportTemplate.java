package com.beligum.blocks.templating.webcomponents.html5.ifaces;

import com.beligum.blocks.templating.webcomponents.WebcomponentsTemplateEngine;
import com.beligum.blocks.templating.webcomponents.html5.JsoupHtmlElement;

/**
 * Created by bram on 5/7/15.
 */
public interface HtmlImportTemplate extends HtmlSnippet
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    Iterable<HtmlScriptElement> getScripts();
    Iterable<HtmlStyleElement> getStyles();
    String getName();
    JsoupHtmlElement renderContent(HtmlElement instanceElement);
    boolean checkReload(WebcomponentsTemplateEngine engine) throws Exception;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

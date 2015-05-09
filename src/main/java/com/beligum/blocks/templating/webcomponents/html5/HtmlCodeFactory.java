package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlSnippet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by bram on 5/7/15.
 */
public class HtmlCodeFactory
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * This is the general creation method to create a general html-string to a more general Html interface.
     * Don't use this to create internal import templates; use the path-supplied variant instead
     * @param htmlDocument
     * @return
     * @throws ParseException
     */
    public static HtmlSnippet create(String htmlDocument) throws ParseException, IOException
    {
        return create(htmlDocument, null);
    }
    /**
     * This variant of the create method is needed when you want to create valid html import templates (we need the path to calculate it's name)
     * @param htmlDocument
     * @param resource
     * @return
     * @throws ParseException
     */
    public static HtmlSnippet create(String htmlDocument, ResourceSearchResult resource) throws ParseException, IOException
    {
        HtmlSnippet retVal = null;

        if (htmlDocument!=null) {
            Document document = parseHtml(htmlDocument);

            if (JsoupHtmlImportTemplate.representsHtmlImportTemplate(document)) {
                retVal = new JsoupHtmlImportTemplate(document, resource);
            }
            else {
                retVal = new JsoupHtmlSnippet(document);
            }
        }

        return retVal;
    }

    //-----PUBLIC METHODS-----
    /**
     * Parse html to jsoup-document.
     * Note: if the html received contains an empty head, only the body-html is returned.
     *
     * @param html
     */
    public static Document parseHtml(String html)
    {
        Document retVal = new Document(Blocks.config().getSiteDomain());
        Document parsed = Jsoup.parse(html, Blocks.config().getSiteDomain(), Parser.htmlParser());
        /*
         * If only part of a html-file is being parsed (which starts f.i. with a <div>-tag), Jsoup will add <html>-, <head>- and <body>-tags, which is not what we want
         * Thus if the head (or body) is empty, but the body (or head) is not, we only want the info in the body (or head).
         */
        if (parsed.head().childNodes().isEmpty() && !parsed.body().childNodes().isEmpty()) {
            for (org.jsoup.nodes.Node child : parsed.body().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if (parsed.body().childNodes().isEmpty() && !parsed.head().childNodes().isEmpty()) {
            for (org.jsoup.nodes.Node child : parsed.head().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if (parsed.body().childNodes().isEmpty() && parsed.body().childNodes().isEmpty()) {
            //add nothing to the retVal so an empty document will be returned
        }
        else {
            retVal = parsed;
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

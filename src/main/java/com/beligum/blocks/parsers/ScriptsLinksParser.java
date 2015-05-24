package com.beligum.blocks.parsers;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.exceptions.ParseException;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Created by bram on 4/27/15.
 */
public class ScriptsLinksParser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    LinkedHashMap<String, String> links = new LinkedHashMap<>();
    LinkedHashMap<String, String> scripts = new LinkedHashMap<>();

    //-----CONSTRUCTORS-----
    public ScriptsLinksParser()
    {
    }

    //-----PUBLIC METHODS-----
    public Node parse(Node node) throws ParseException
    {
        if (node.nodeName().equals("link")) {
            //if an include has been found, import the wanted html-file
            String href = node.attr("href");
            if (href.isEmpty()) {
                throw new ParseException("Encountered a <link> element without a href attribute; can't proceed");
            }

            //if we already have a link for this href, don't add it again
            if (!this.links.containsKey(href)) {
                this.links.put(href, node.outerHtml());
            }
            else {
                Logger.warn("Double <link> tag detected for href=\"" + href + "\", not adding it again; " + node.outerHtml());
            }

            Node emtpyNode = new TextNode("", null);
            node.replaceWith(emtpyNode);
            node = emtpyNode;
        }
        else if (node.nodeName().equals("script")) {
            //if a script has been found, add it to the scripts-stack

            String src = node.attr("src");
            //let's allow inline <script> tags
            if (src.isEmpty()) {
                src = UUID.randomUUID().toString();
            }
            //if we already have a src for this script, don't add it again
            if (!this.scripts.containsKey(src)) {
                this.scripts.put(src, node.outerHtml());
            }
            else {
                Logger.warn("Double <script> tag detected for src=\""+src+"\", not adding it again; "+node.outerHtml());
            }

            Node emtpyNode = new TextNode("", null);
            node.replaceWith(emtpyNode);
            node = emtpyNode;
        }

        return node;
    }
    /**
     * @return Returns a src -> tag map, eg "/assets/scripts/blocks.js" -> "<script src="/assets/scripts/blocks.js" type="application/javascript"></script>"
     * so we can check if this link has already been included or not.
     * Note that scripts can be inline and thus don't have a src key, in which case a random UUID was generated as the key.
     */
    public LinkedHashMap<String, String> getScripts()
    {
        return this.scripts;
    }
    /**
     * @return Returns a link -> tag map, eg "/assets/styles/styles.less" -> "<link href="/assets/styles/styles.less" rel="stylesheet">"
     * so we can check if this link has already been included or not.
     * Note that links must always have a href (otherwise an exception is thrown during parsing)
     */
    public LinkedHashMap<String, String> getLinks()
    {
        return this.links;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

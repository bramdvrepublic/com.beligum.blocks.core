package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.models.storables.Entity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.List;

/**
 * Created by wouter on 21/11/14.
 */
public abstract class AbstractParser
{
    public void parse(String html) {

    }

    public static Entity cacheEntity(URL url, String html) {
        Document doc = Jsoup.parse(html);
        return EntityParsingNodeVisitor.cache(url, doc);

    }

    public static Entity parseEntity(URL url, String html) {
        Document doc = Jsoup.parse(html);
        return EntityParsingNodeVisitor.parse(url, doc);

    }
    /*
    * Static methods to analyze html elements
    * */


    public static boolean hasResource(Element element) {
        boolean retVal = false;
        if (element.hasAttr("resource")) {
            retVal = true;
        }
        return retVal;
    }

    public static boolean isTemplate(Element node) {
        boolean retVal = false;
        if (node.tagName().equals("html") && node.hasAttr("template")) {
            retVal = true;
        }
        return retVal;
    }

    public static boolean isLayoutable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-layout")) {
            retVal = true;
        }
        return retVal;
    }

    public static boolean isReplaceable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-replace")) {
            retVal = true;
        }
        return retVal;
    }

    public static boolean isBootstrapContainer(Element node) {
        return node.hasClass("container");
    }

    public static boolean isBootstrapRow(Element node) {
        return node.hasClass("row");
    }

    public static boolean isBootstrapColumn(Element node) {
        boolean retVal = false;
        for (String cName: node.classNames()) {
            if (cName.startsWith("col-")) {
                retVal = true;
                break;
            }
        }
        return retVal;
    }

    public static boolean isBootstrapLayout(Element node) {
        return isBootstrapRow(node) || isBootstrapContainer(node) || isBootstrapColumn(node);
    }
    public static boolean isBootstrapLayout(List<Element> elements) {
        boolean retVal = true;
        for (Element el: elements) {
            if (!isBootstrapLayout(el)) {
                retVal = false;
                break;
            }
        }
        return retVal;
    }

    public static boolean isReference(Element node) {
        return node.hasAttr("reference");
    }

    public static boolean isProperty(Element node) {
        boolean retVal = false;
        if (node.hasAttr("property")) {
            retVal = true;
        }
        return retVal;
    }

    public static String getProperty(Element node) {
        return node.attr("property");
    }

    public static boolean isType(Element node) {
        boolean retVal = false;
        if (node.hasAttr("typeof")) {
            retVal = true;
        }
        return retVal;
    }

    public static String getType(Element node) {
        String retVal = null;
        if (node.hasAttr("typeof")) {
            retVal = node.attr("typeof");
        }
        if (retVal == null) retVal = CSSClasses.DEFAULT_ENTITY_CLASS;
        return retVal;
    }

    public static boolean isBlueprint(Element node) {
        boolean retVal = false;
        if (node.hasAttr("blueprint")) {
            retVal = true;
        }
        return retVal;
    }

    public static boolean isEditable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-edit")) {
            retVal = true;
        }
        return retVal;
    }

    public static boolean isInlineEditable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-edit-inline")) {
            retVal = true;
        }
        return retVal;
    }

    public static boolean isBlock(Element node)
    {
        return isType(node) || isProperty(node);
    }

    public static boolean isMutable(Element node) {
        return isLayoutable(node) || isReplaceable(node) || isEditable(node) || isInlineEditable(node);
    }


    public static String getResource(Element element) {
        return element.attr("resource");
    }


}

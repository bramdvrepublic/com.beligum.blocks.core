package com.beligum.blocks.parsers;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Created by wouter on 17/03/15.
 */
public class ElementParser
{
    public static boolean isReadOnly(Element node)
    {
        return node.hasAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY);
    }

    public static boolean isPageTemplateContentNode(Element node)
    {
        return node.hasAttr(ParserConstants.PAGE_TEMPLATE_CONTENT_ATTR);
    }

    public static boolean isPageTemplateRoot(Element node)
    {
        return node.nodeName().equals("html") && node.hasAttr(ParserConstants.PAGE_TEMPLATE_ATTR);
    }

    public static boolean isCanLayout(Element node)
    {
        return node.hasAttr(ParserConstants.CAN_LAYOUT);
    }

    public static boolean isAddableBlock(Element node)
    {
        return !node.hasAttr(ParserConstants.NOT_ADDABLE_BLOCK);
    }

    public static boolean isPageBlock(Element node)
    {
        return node.hasAttr(ParserConstants.PAGE_BLOCK);
    }

    public static boolean isTypeOf(Element node)
    {
        return node.hasAttr(ParserConstants.TYPE_OF);
    }

    public static boolean isBlueprint(Element node)
    {
        return node.hasAttr(ParserConstants.BLUEPRINT);
    }

    public static boolean isPrefix(Element node)
    {
        return node.hasAttr(ParserConstants.PREFIX);
    }

    public static boolean isProperty(Element node)
    {
        return node.hasAttr(ParserConstants.PROPERTY) || node.hasAttr(ParserConstants.BLUEPRINT_PROPERTY);
    }

    public static boolean isUseBlueprint(Element node)
    {
        return node.hasAttr(ParserConstants.USE_BLUEPRINT);
    }

    public static boolean isReferenceTo(Element node)
    {
        return node.hasAttr(ParserConstants.REFERENCE_TO);
    }

    public static boolean isResource(Element node)
    {
        return node.hasAttr(ParserConstants.RESOURCE);
    }

    public static boolean isSingleton(Element node)
    {
        return node.hasAttr(ParserConstants.SINGLETON);
    }

    public static boolean hasPrefix(Element node)
    {
        return node.hasAttr(ParserConstants.PREFIX);
    }

    public static boolean hasInList(Element node)
    {
        return node.hasAttr(ParserConstants.INLIST);
    }

//    public static URL getHref(Element node) {
//        URL retVal = null;
//        try {
//            if (node.hasAttr("href")) {
//                retVal = URLFactory.createURL(node.attr("href"));
//            } else if (node.hasAttr("src")) {
//                retVal = URLFactory.createURL(node.attr("src"));
//            }
//        }
//        catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//        return retVal;
//    }
//


    public static String getBlueprintName(Element node)
    {
        if (node.hasAttr(ParserConstants.USE_BLUEPRINT) && !node.attr(ParserConstants.USE_BLUEPRINT).isEmpty()) {
            return node.attr(ParserConstants.USE_BLUEPRINT);
        }
        else if (node.hasAttr(ParserConstants.BLUEPRINT) && !node.attr(ParserConstants.BLUEPRINT).isEmpty()) {
            return node.attr(ParserConstants.BLUEPRINT);
        }
        else {
            return null;
        }
    }

    public static String getReferenceUrl(Element node)
    {
        if (node.hasAttr(ParserConstants.REFERENCE_TO) && !node.attr(ParserConstants.REFERENCE_TO).isEmpty()) {
            return node.attr(ParserConstants.REFERENCE_TO);
        }
        else if (node.hasAttr(ParserConstants.RESOURCE) && !node.attr(ParserConstants.RESOURCE).isEmpty()) {
            return node.attr(ParserConstants.RESOURCE);
        }
        else {
            return null;
        }
    }

    public static String getPrefix(Element node)
    {
        if (node.hasAttr(ParserConstants.PREFIX) && !node.attr(ParserConstants.PREFIX).isEmpty()) {
            return node.attr(ParserConstants.PREFIX);
        }
        else {
            return null;
        }
    }

    public static String getSingletonName(Element node)
    {
        if (node.hasAttr(ParserConstants.SINGLETON) && !node.attr(ParserConstants.SINGLETON).isEmpty()) {
            return node.attr(ParserConstants.SINGLETON);
        }
        else {
            return null;
        }
    }

    public static String getProperty(Element node)
    {
        if (node.hasAttr(ParserConstants.PROPERTY) && !node.attr(ParserConstants.PROPERTY).isEmpty()) {
            return node.attr(ParserConstants.PROPERTY);
        }
        else if (node.hasAttr(ParserConstants.BLUEPRINT_PROPERTY) && !node.attr(ParserConstants.BLUEPRINT_PROPERTY).isEmpty()) {
            return node.attr(ParserConstants.BLUEPRINT_PROPERTY);

        }
        else {
            return null;

        }
    }

    public static String getPagetemplateName(Element node)
    {
        if (node.hasAttr(ParserConstants.PAGE_TEMPLATE_ATTR) && !node.attr(ParserConstants.PAGE_TEMPLATE_ATTR).isEmpty()) {
            return node.attr(ParserConstants.PAGE_TEMPLATE_ATTR);
        }
        else {
            return null;
        }
    }

    public static String getReferenceTo(Element node)
    {
        String retVal = null;
        if (node.hasAttr(ParserConstants.REFERENCE_TO) && !node.attr(ParserConstants.REFERENCE_TO).isEmpty()) {
            retVal = node.attr(ParserConstants.REFERENCE_TO);
        }
        return retVal;
    }

    public static String getResource(Element node)
    {
        String retVal = null;
        if (node.hasAttr(ParserConstants.RESOURCE) && !node.attr(ParserConstants.RESOURCE).isEmpty()) {
            retVal = node.attr(ParserConstants.RESOURCE);
        }
        return retVal;
    }

    public static String getTypeOf(Element node)
    {
        String retVal = null;
        if (node.hasAttr(ParserConstants.TYPE_OF) && !node.attr(ParserConstants.TYPE_OF).isEmpty()) {
            retVal = node.attr(ParserConstants.TYPE_OF);
        }
        return retVal;
    }

    public static Locale getLanguage(Element node)
    {
        Locale retVal = Locale.ROOT;
        if (node.hasAttr(ParserConstants.LANGUAGE) && !node.attr(ParserConstants.LANGUAGE).isEmpty()) {
            String lang = node.attr(ParserConstants.LANGUAGE);
            if (lang != null) retVal = Blocks.config().getLocaleForLanguage(lang);
        }
        return retVal;
    }

    public static Element setLanguage(Element node, String language)
    {
        node.attr(ParserConstants.LANGUAGE, language);
        return node;
    }

}

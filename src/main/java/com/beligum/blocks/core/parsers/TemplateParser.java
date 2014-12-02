package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.jsoup.ToHtmlVisitor;
import com.beligum.blocks.core.parsers.jsoup.ToTemplateVisitor;
import com.beligum.blocks.core.parsers.jsoup.Traversor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
* Created by wouter on 21/11/14.
*/
public class TemplateParser
{

    /**
     * Parse all templates found in the specified html and cache them in the correct cacher. (PageTemplate in PageTemplateCache, EntityTemplateClass in EntityTemplateClassCache)
     * @param html the html to be parsed
     * @throws ParseException
     */
    public void cacheTemplates(String html) throws ParseException
    {
        Document doc = Jsoup.parse(html);
        ToTemplateVisitor visitor = new ToTemplateVisitor(true);
        Traversor traversor = new Traversor(visitor);
        traversor.traverse(doc);

    }

    public EntityTemplate parseEntityTemplate(URL url, String html) throws ParseException
    {
        Document doc = Jsoup.parse(html);
        return ToTemplateVisitor.parse(doc, url);

    }

//    public Set<String> getChildIdsFromTemplate(String template)
//    {
//        Set<String> childIds = new HashSet<>();
//        Document templateDOM = Jsoup.parse(template, BlocksConfig.getSiteDomain(), Parser.xmlParser());
//        //TODO: hier bezig
//        Elements children = templateDOM.select("[" + ParserConstants.REFERENCE_TO + "]");
//        for(Element child : children){
//            childIds.add(child.attr(ParserConstants.REFERENCE_TO));
//        }
//        return childIds;
//    }

    public String renderEntityInsidePageTemplate(PageTemplate pageTemplate, EntityTemplate entityTemplate) throws ParseException
    {
        Document DOM = Jsoup.parse(pageTemplate.getTemplate());
        Elements referenceBlocks = DOM.select("[" + ParserConstants.REFERENCE_TO + "]");
        for(Element reference : referenceBlocks){
            //TODO: implement this, I just needed a compiling version
            reference.replaceWith(new TextNode(entityTemplate.getTemplate(), ""));
        }
        Traversor traversor = new Traversor(new ToHtmlVisitor());
        traversor.traverse(DOM);
        return DOM.outerHtml();
    }

}

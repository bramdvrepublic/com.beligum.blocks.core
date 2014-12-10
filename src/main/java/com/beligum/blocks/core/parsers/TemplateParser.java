package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import sun.org.mozilla.javascript.ast.Block;

import java.net.MalformedURLException;
import java.net.URL;

/**
* Created by wouter on 21/11/14.
*/
public class TemplateParser
{
    //TODO BAS: use this enum
    public enum Modes{
        CACHE_CLASSES,
        NEW_INSTANCE,
        UPDATE_ENTITY
    }

    /**
     * Parse all templates found in the specified html and cache them in the correct cacher. (PageTemplate in PageTemplateCache, EntityTemplateClass in EntityTemplateClassCache)
     * @param html the html to be parsed
     * @throws ParseException
     */
    public static void cacheTemplatesFromFile(String html) throws ParseException
    {
        Document doc = parse(html);
        Traversor traversor = new Traversor(new FileToCacheVisitor());
        traversor.traverse(doc);

    }

    /**
     * Save a new entity-template-instance of class 'entityTempalteClass' to db, and also all it's children.
     * @param entityTemplateClass
     * @return true if save succeeded
     */
    public static URL saveNewEntityTemplateToDb(EntityTemplateClass entityTemplateClass) throws ParseException
    {
        String pageStringId = "";
        try {
            Document doc = parse(entityTemplateClass.getTemplate());
            ClassToStoredInstanceVisitor visitor = new ClassToStoredInstanceVisitor();
            Traversor traversor = new Traversor(visitor);
            traversor.traverse(doc);
            pageStringId = visitor.getReferencedId(doc.child(0));
            RedisID pageId = new RedisID(pageStringId, RedisID.NO_VERSION);
            return pageId.getUrl();
        }
        catch(IDException e){
            throw new ParseException("Couldn't construct url for new " + EntityTemplate.class.getSimpleName() + "-instance: " + pageStringId, e);
        }

    }

    public static String renderEntityInsidePageTemplate(PageTemplate pageTemplate, EntityTemplate entityTemplate) throws ParseException
    {
        //TODO BAS SH: we need to let the parser start at the root of the page-template and find a generic way to replace entities into it. We need this because for the moment we get a nullpointerexception when we're parsing the template to html, since we replace the outer-most entity with it's class-template-nodes, whom don't have the correct parents (the ones of the page-template). If we have a generic way to parse the entities into a page-template, we won't be having this problem, plus it will more easily be possible to render multiple entities in 1 pagetemplate.
        Document DOM = parse(pageTemplate.getTemplate());
        Elements referenceBlocks = DOM.select("[" + ParserConstants.REFERENCE_TO + "]");
        for(Element reference : referenceBlocks){
            Document entityDOM = Jsoup.parse(entityTemplate.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser());
            //a Document has a <#root>-tag indicating the fact that it is indeed a Document, we only want the actual html to be put into the reference-element
            Element entityRoot = entityDOM.child(0);
            reference.replaceWith(entityRoot);
        }
        Traversor traversor = new Traversor(new ToHtmlVisitor());
        traversor.traverse(DOM);
        return DOM.outerHtml();
    }

    public static void updateEntity(URL url, String html) throws ParseException
    {
        try{
            RedisID newVersion = new RedisID(url);
            Document newDOM = parse(html);
            //TODO BAS: this should be something of the form
            Traversor traversor = new Traversor(new HtmlToStoreVisitor());
            traversor.traverse(newDOM);
        }
        catch(IDException e){
            throw new ParseException("Could not create a new version for '" + url + "'.", e);
        }
    }

    //    public Set<String> getChildIdsFromTemplate(String template)
    //    {
    //        Set<String> childIds = new HashSet<>();
    //        Document templateDOM = Jsoup.parse(template, BlocksConfig.getSiteDomain(), Parser.xmlParser());
    //        Elements children = templateDOM.select("[" + ParserConstants.REFERENCE_TO + "]");
    //        for(Element child : children){
    //            childIds.add(child.attr(ParserConstants.REFERENCE_TO));
    //        }
    //        return childIds;
    //    }

    /**
     * Parse html to jsoup-document, using xml-parser
     * @param html
     * @return
     */
    private static Document parse(String html){
        return Jsoup.parse(html, BlocksConfig.getSiteDomain(), Parser.xmlParser());
    }

}

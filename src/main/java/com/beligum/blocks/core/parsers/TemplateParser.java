package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.net.URL;

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
    public static void cacheTemplatesFromFile(String html) throws ParseException
    {
        Document doc = parse(html);
        Traversor traversor = new Traversor(new FileToCacheVisitor());
        traversor.traverse(doc);

    }

    /**
     * Save a new entity-template-instance of class 'entityTempalteClass' to db, and also all it's children.
     * @param entityTemplateClass
     * @return the url of the freshly saved template
     */
    public static URL saveNewEntityTemplateToDb(URL pageURL, EntityTemplateClass entityTemplateClass) throws ParseException
    {
        String pageStringId = "";
        try {
            Element doc = parse(entityTemplateClass.getTemplate());
            ClassToStoredInstanceVisitor visitor = new ClassToStoredInstanceVisitor(pageURL);
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
        Element DOM = parse(pageTemplate.getTemplate());
        Elements referenceBlocks = DOM.select("[" + ParserConstants.REFERENCE_TO + "=" + ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME +"]");
        for(Element reference : referenceBlocks){
            Element entityRoot = TemplateParser.parse(entityTemplate.getTemplate()).child(0);
            reference.replaceWith(entityRoot);
        }
        Traversor traversor = new Traversor(new ToHtmlVisitor());
        traversor.traverse(DOM);
        return DOM.outerHtml();
    }

    public static String renderTemplate(AbstractTemplate template) throws ParseException
    {
        Element classDOM = parse(template.getTemplate());
        Traversor traversor = new Traversor(new ToHtmlVisitor());
        Node classRoot = classDOM.child(0);
        traversor.traverse(classRoot);
        return classDOM.outerHtml();
    }

    public static URL updateEntity(String html) throws ParseException
    {
        Document newDOM = parse(html);
        Traversor traversor = new Traversor(new HtmlToStoreVisitor());
        traversor.traverse(newDOM);
        return traversor.getPageUrl();
    }

    /**
     * Parse html to jsoup-document, using xml-parser
     * @param html
     * @return
     */
    //this method is protected, so all classes in this package can access it!
    protected static Document parse(String html){
        //a Document has a <#root>-tag indicating the fact that it is indeed a Document, we only want the actual html to be put into the reference-element
        return Jsoup.parse(html, BlocksConfig.getSiteDomain(), Parser.xmlParser());
    }

}

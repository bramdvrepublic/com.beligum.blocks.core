package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.visitors.ClassToStoredInstanceVisitor;
import com.beligum.blocks.core.parsers.visitors.FileToCacheVisitor;
import com.beligum.blocks.core.parsers.visitors.HtmlToStoreVisitor;
import com.beligum.blocks.core.parsers.visitors.ToHtmlVisitor;
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
     * Save a new entity-template-instance of class 'entityTemplateClass' to db, and also all it's children.
     * @param language the language the new entity is written in (must be the same as specified in pageUrl-parameter,
     *                 if one is present there), if no such language is specified the primary language
     *                 of the entity-class is used
     * @param entityTemplateClass
     * @return the url of the freshly saved template
     */
    public static URL saveNewEntityTemplateToDb(URL pageURL, String language, AbstractTemplate entityTemplateClass) throws ParseException
    {
        String pageStringId = "";
        try {
            if(!Languages.containsLanguageCode(language)){
                language = entityTemplateClass.getLanguage();
            }
            String html = entityTemplateClass.getTemplate(language);
            if(html == null){
                html = entityTemplateClass.getTemplate();
            }
            Element doc = parse(html);
            ClassToStoredInstanceVisitor visitor = new ClassToStoredInstanceVisitor(pageURL, language);
            Traversor traversor = new Traversor(visitor);
            traversor.traverse(doc);
            pageStringId = visitor.getReferencedId(doc.child(0));
            RedisID pageId = new RedisID(pageStringId, RedisID.NO_VERSION, language);
            return pageId.getLanguagedUrl();
        }
        catch(IDException e){
            throw new ParseException("Couldn't construct url for new " + EntityTemplate.class.getSimpleName() + "-instance: " + pageStringId, e);
        }

    }

    /**
     * Render the html of a certain entity inside a page-template, using the primary language of the entity-template
     * @param pageTemplate
     * @param entityTemplate
     * @return
     * @throws ParseException
     */
    public static String renderEntityInsidePageTemplate(PageTemplate pageTemplate, EntityTemplate entityTemplate) throws ParseException
    {
        String language = entityTemplate.getLanguage();
        return renderEntityInsidePageTemplate(pageTemplate, entityTemplate, language);
    }

    /**
     * Render the html of a certain entity inside a page-template, using the specified language
     * @param pageTemplate
     * @param entityTemplate
     * @param language
     * @return
     * @throws ParseException
     */
    public static String renderEntityInsidePageTemplate(PageTemplate pageTemplate, EntityTemplate entityTemplate, String language) throws ParseException
    {
        if(!Languages.isNonEmptyLanguageCode(language)){
            throw new ParseException("No language specified!");
        }
        String html = pageTemplate.getTemplate(language);
        //if the requested language cannot be found, use the default language
        if(html == null){
            html = pageTemplate.getTemplate();
        }
        Element DOM = parse(html);
        Elements referenceBlocks = DOM.select("[" + ParserConstants.REFERENCE_TO + "=" + ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME +"]");
        for(Element reference : referenceBlocks){
            String entityHtml = entityTemplate.getTemplate(language);
            //if the requested language cannot be found, use the default language
            if(entityHtml == null){
                entityHtml = entityTemplate.getTemplate();
            }
            Element entityRoot = TemplateParser.parse(entityHtml).child(0);
            reference.replaceWith(entityRoot);
        }
        Traversor traversor = new Traversor(new ToHtmlVisitor(entityTemplate.getUrl(), language));
        traversor.traverse(DOM);
        return DOM.outerHtml();
    }

    /**
     * Renders the template in the primary language of the specified template
     * @param template
     * @return
     * @throws ParseException
     */
    public static String renderTemplate(AbstractTemplate template) throws ParseException
    {
        Element classDOM = parse(template.getTemplate());
        Traversor traversor = new Traversor(new ToHtmlVisitor(template.getId().getUrl(), template.getLanguage()));
        Node classRoot = classDOM.child(0);
        traversor.traverse(classRoot);
        return classDOM.outerHtml();
    }

    public static void updateEntity(URL entityUrl, String html) throws ParseException
    {
        Document newDOM = parse(html);
        Traversor traversor = new Traversor(new HtmlToStoreVisitor(entityUrl));
        traversor.traverse(newDOM);
//        return traversor.getPageUrl();
    }

    /**
     * Parse html to jsoup-document.
     * Note: if the html received contains an empty head, only the body-html is returned.
     * @param html
     * @return
     */
    public static Document parse(String html){
        Document retVal = new Document(BlocksConfig.getSiteDomain());
        Document parsed = Jsoup.parse(html, BlocksConfig.getSiteDomain(), Parser.htmlParser());
        /*
         * If only part of a html-file is being parsed (which starts f.i. with a <div>-tag), Jsoup will add <html>-, <head>- and <body>-tags, which is not what we want
         * Thus if the head is empty, but the body is not, we only want the info in the body.
         */
        if(parsed.head().children().isEmpty() && !parsed.body().children().isEmpty()){
            retVal.appendChild(parsed.body().child(0));
        }
        else{
            retVal = parsed;
        }
        return retVal;
    }

}

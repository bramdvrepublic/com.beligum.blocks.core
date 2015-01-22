package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.visitors.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by wouter on 21/11/14.
 */
public class TemplateParser
{

    /**
     * Parse all templates found in the specified html and cache them in the specified collection.
     * @param html the html to be parsed
     * @param cache a {@link java.util.List} in which the {@link PageTemplate}s and {@link EntityTemplateClass}es should be cached
     * @throws ParseException
     */
    public static void cacheBlueprintsFromFile(String html, List<AbstractTemplate> cache) throws ParseException
    {
        Document doc = parse(html);
        Traversor traversor = new Traversor(new BlueprintVisitor(cache));
        traversor.traverse(doc);

    }

    public static void injectDefaultsForTemplates(Collection<? extends AbstractTemplate> templates) throws ParseException
    {
        for(AbstractTemplate template : templates) {
            Map<RedisID, String> htmlTemplates = template.getTemplates();
            for(RedisID language : htmlTemplates.keySet()) {
                Document doc = parse(htmlTemplates.get(language));
                Traversor traversor = new Traversor(new DefaultsVisitor(language.getLanguage(), template));
                traversor.traverse(doc);
            }
        }
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

//    /**
//     * Render the html of a certain entity inside a page-template, using the primary language of the entity-template
//     * @param pageTemplate
//     * @param entityTemplate
//     * @throws ParseException
//     */
//    public static String renderEntityInsidePageTemplate(PageTemplate pageTemplate, EntityTemplate entityTemplate) throws ParseException
//    {
//        String language = entityTemplate.getLanguage();
//        return renderEntityInsidePageTemplate(pageTemplate, entityTemplate, language);
//    }

    /**
     * Render the html of a certain entity inside a page-template, using the specified language
     * Uses scripts and links injection to place all javascripts and css-class in the right order:
     * 1) links of page-template, 2) links of blueprints, 3) links of dynamic blocks, 4) scripts of page-template, 5) scripts of blueprints, 6) scripts of dynamic blocks
     * @param pageTemplate
     * @param entityTemplate
     * @param language
     * @throws ParseException
     */
    public static String renderEntityInsidePageTemplate(PageTemplate pageTemplate, EntityTemplate entityTemplate, String language) throws ParseException
    {
        try{
            if(!Languages.isNonEmptyLanguageCode(language)){
                throw new ParseException("No language specified!");
            }
            String html = pageTemplate.getTemplate(language);
            //if the requested language cannot be found, use the default language
            if(html == null){
                html = pageTemplate.getTemplate();
            }
            Document DOM = parse(html);
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
            ToHtmlVisitor visitor = new ToHtmlVisitor(entityTemplate.getUrl(), language, pageTemplate.getLinks(), pageTemplate.getScripts());
            Traversor traversor = new Traversor(visitor);
            traversor.traverse(DOM);
            //inject links and scripts found by the visitor while parsing the DOM
            List<Node> links = visitor.getLinks();
            List<Node> scripts = visitor.getScripts();
            for(Node link : links){
                DOM.head().appendChild(link);
            }
            for(Node script : scripts){
                DOM.head().appendChild(script);
            }
            return DOM.outerHtml();
        }
        catch (Exception e){
            throw new ParseException("Exception while rendering entity '" + entityTemplate.getName() + "' in tempalte '" + pageTemplate.getName() + "'.", e);
        }
    }

    /**
     * Renders the template in the primary language of the specified template, no scripts or links are injected
     * @param template
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
     */
    public static Document parse(String html){
        Document retVal = new Document(BlocksConfig.getSiteDomain());
        Document parsed = Jsoup.parse(html, BlocksConfig.getSiteDomain(), Parser.htmlParser());
        /*
         * If only part of a html-file is being parsed (which starts f.i. with a <div>-tag), Jsoup will add <html>-, <head>- and <body>-tags, which is not what we want
         * Thus if the head (or body) is empty, but the body (or head) is not, we only want the info in the body (or head).
         */
        if(parsed.head().children().isEmpty() && !parsed.body().children().isEmpty()){
            for(Node child : parsed.body().children()) {
                retVal.appendChild(child);
            }
        }
        else if(parsed.body().children().isEmpty() && !parsed.head().children().isEmpty()){
            for(Node child : parsed.head().children()) {
                retVal.appendChild(child);
            }
        }
        else{
            retVal = parsed;
        }
        return retVal;
    }

}

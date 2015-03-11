package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.caching.BlueprintsCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.visitors.*;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.core.framework.utils.Logger;
import org.apache.shiro.SecurityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

/**
 * Created by wouter on 21/11/14.
 */
public class TemplateParser
{

    /**
     * Parse all templates found in the specified html and cache them in the specified collection.
     * @param fileHtml the html to be parsed
     * @param cache a {@link java.util.List} in which the {@link PageTemplate}s and {@link com.beligum.blocks.core.models.redis.templates.Blueprint}es should be cached
     * @param foundEntityClassNames the names of all entity-classes already found
     * @throws ParseException
     */
    public static void findTemplatesFromFile(String fileHtml, List<AbstractTemplate> cache, Set<String> foundEntityClassNames) throws ParseException
    {
        Document doc = parse(fileHtml);
        Traversor traversor = new Traversor(new FindBlueprintsVisitor(cache, foundEntityClassNames));
        traversor.traverse(doc);
    }

    /**
     * Method taking a list of templates and using those to save default-entities to db and eventually cache all templates to the application-cache, holding references to the saved default-entities.
     * This method also selects which templates will be cached to the application-cache, if multiple templates with the same name are present. (F.i. blueprints are always preferred.)
     * @param foundTemplates a list of templates, containing all templates found while visiting the template-files on disk
     * @throws ParseException
     */
    public static void injectDefaultsInFoundTemplatesAndCache(List<? extends AbstractTemplate> foundTemplates) throws ParseException
    {
        try {
            Map<String, PageTemplate> allPageTemplates = new HashMap<>();
            allPageTemplates.put(ParserConstants.DEFAULT_PAGE_TEMPLATE, PageTemplateCache.getInstance().get(ParserConstants.DEFAULT_PAGE_TEMPLATE));
            Map<String, Blueprint> allBlueprints = new HashMap<>();
            allBlueprints.put(ParserConstants.DEFAULT_BLUEPRINT, BlueprintsCache.getInstance().get(ParserConstants.DEFAULT_BLUEPRINT));
            //split the list of templates up into page-templates and entity-classes
            for (AbstractTemplate template : foundTemplates) {
                if (template instanceof PageTemplate) {
                    PageTemplate replacedTemplate = allPageTemplates.put(template.getName(), (PageTemplate) template);
                    //default page-templates should be added to the cache no matter what, so the last one encountered is kept
                    if (replacedTemplate != null && replacedTemplate.getName().contentEquals(ParserConstants.DEFAULT_PAGE_TEMPLATE)) {
                        Logger.warn("Default-" + PageTemplate.class.getSimpleName() + " will be replaced. This should only happen once!");
                    }
                    //no two page-templates with the same name can be defined
                    else if (replacedTemplate != null) {
                        throw new ParseException(
                                        "Cannot add two " + PageTemplate.class.getSimpleName() + "s with the same name '" + template.getName() + "' to the cache. First found \n \n" +
                                        replacedTemplate +
                                        "\n \n and then \n \n" + template + "\n \n");
                    }
                }
                else if (template instanceof Blueprint) {
                    Blueprint replacedTemplate = allBlueprints.put(template.getName(), (Blueprint) template);
                    //default blueprints should be added to the cache no matter what, so the last one encountered is kept
                    if (replacedTemplate != null && replacedTemplate.getName().contentEquals(ParserConstants.DEFAULT_BLUEPRINT)) {
                        Logger.warn("Default-" + Blueprint.class.getSimpleName() + " will be replaced. This should only happen once!");
                    }
                    //if an entity-class with this name was already present, check if it was a non-blueprint, if not, throw an exception since only one blueprint can be defined per class
                    else if (replacedTemplate != null) {
                        Map<BlocksID, String> replacedTemplates = replacedTemplate.getTemplates();
                        boolean isBlueprint = false;
                        for (BlocksID languageId : replacedTemplates.keySet()) {
                            //TODO BAS!: here also the "typeof"-attribute should be checked for presence
                            isBlueprint = new SuperVisitor().isBlueprint(TemplateParser.parse(replacedTemplates.get(languageId)).child(0));
                            if (isBlueprint) {
                                throw new ParseException("A " + Blueprint.class.getSimpleName() + " of type '" + replacedTemplate.getName() +
                                                         "' was already present in cache. Cannot have two blueprints for the same type. Found two! First encountered \n \n " + replacedTemplate +
                                                         "\n \n  and then \n \n" + template + "\n \n");
                            }
                        }
                    }
                }
                //only page-templates and blueprintes should be present in the list of found templates
                else {
                    throw new ParseException("Found unsupported " + AbstractTemplate.class.getSimpleName() + "-type " + template.getClass().getSimpleName() + ".");
                }
            }

            //create defaults for all found entity-classes and cache to application-cache
            for (String templateName : allBlueprints.keySet()) {
                //during traversal of a template, all it's child-types are cached too
                if(!BlueprintsCache.getInstance().contains(templateName) || templateName.equals(ParserConstants.DEFAULT_BLUEPRINT)) {
                    AbstractTemplate template = allBlueprints.get(templateName);
                    Map<BlocksID, String> htmlTemplates = template.getTemplates();
                    for (BlocksID language : htmlTemplates.keySet()) {
                        Document doc = parse(htmlTemplates.get(language));
                        Traversor traversor = new Traversor(new CachingAndDefaultsVisitor(language.getLanguage(), doc, template, allBlueprints, allPageTemplates));
                        traversor.traverse(doc);
                    }
                }
            }

            //create defaults for all found page-templates and cache to application-cache
            for (String templateName : allPageTemplates.keySet()) {
                if(!PageTemplateCache.getInstance().contains(templateName) || templateName.equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)) {
                    AbstractTemplate template = allPageTemplates.get(templateName);
                    Map<BlocksID, String> htmlTemplates = template.getTemplates();
                    for (BlocksID language : htmlTemplates.keySet()) {
                        Document doc = parse(htmlTemplates.get(language));
                        Traversor traversor = new Traversor(new CachingAndDefaultsVisitor(language.getLanguage(), doc, template, allBlueprints, allPageTemplates));
                        traversor.traverse(doc);
                    }
                }
            }
        }
        catch (Exception e){
            throw new ParseException("Error while injecting defaults into templates to be cached.", e);
        }
    }

    /**
     * Save a new entity-template-instance of class 'entityTemplateClass' to db, and also all it's children.
     * @param id the id the new template will be given
     * @param entityTemplateClass
     * @return the url of the freshly saved template
     */
    public static void saveNewEntityTemplateToDb(BlocksID id, AbstractTemplate entityTemplateClass) throws ParseException
    {
        try {
            String language = id.getLanguage();
            if(!Languages.isLanguageCode(language)){
                language = entityTemplateClass.getLanguage();
            }
            String html = entityTemplateClass.getTemplate(language);
            if(html == null){
                html = entityTemplateClass.getTemplate();
            }
            Element doc = parse(html);
            BlueprintToStoredInstanceVisitor visitor = new BlueprintToStoredInstanceVisitor(id.getUrl(), language);
            Traversor traversor = new Traversor(visitor);
            traversor.traverse(doc);
        }
        catch(Exception e){
            throw new ParseException("Couldn't save new template instance to db", e);
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
     * Uses scripts and links injection to place all javascripts and css-classes in the right order:
     * 1) links of page-template
     * 2) links of blueprints
     * 3) links of dynamic blocks
     * 4) scripts of page-template
     * 5) scripts of blueprints
     * 6) scripts of dynamic blocks
     * 7) frontend-scripts if an admin is logged in
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
//                Element entityRoot = TemplateParser.parse(entityHtml).child(0);
                reference.attr(ParserConstants.REFERENCE_TO, entityTemplate.getUnversionedId());
                reference.attr(ParserConstants.USE_BLUEPRINT, entityTemplate.getBlueprintType());
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
            //inject frontend links and scripts if logged in as administrator
            if(SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                addFrontendScripts(DOM, visitor.getLinks(), visitor.getScripts());
            }
            return DOM.outerHtml();
        }
        catch (Exception e){
            throw new ParseException("Exception while rendering entity '" + entityTemplate.getName() + "' in template '" + pageTemplate.getName() + "'.", e);
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

    public static void updateEntity(BlocksID id, String html) throws ParseException
    {
        Document newDOM = parse(html);
        Traversor traversor = new Traversor(new HtmlToStoreVisitor(id.getUrl(), id.getLanguage(), newDOM));
        traversor.traverse(newDOM);
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
        if(parsed.head().childNodes().isEmpty() && !parsed.body().childNodes().isEmpty()){
            for(Node child : parsed.body().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if(parsed.body().childNodes().isEmpty() && !parsed.head().childNodes().isEmpty()){
            for(Node child : parsed.head().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if(parsed.body().childNodes().isEmpty() && parsed.body().childNodes().isEmpty()){
            //add nothing to the retVal so an empty document will be returned
        }
        else{
            retVal = parsed;
        }
        return retVal;
    }

    /**
     *
     * @param DOM the DOM in which the blocks frontend scripts should be injected
     * @param presentLinks the links which are already present in the DOM
     * @param presentScripts the scripts which are already present in the DOM
     * @throws IOException
     * @throws ParseException
     */
    private static void addFrontendScripts(Document DOM, List<Node> presentLinks, List<Node> presentScripts) throws IOException, ParseException
    {
        //find all sources that are already being loaded by the DOM
        Set<String> presentSources = new HashSet<>();
        for(Node link : presentLinks){
            presentSources.add(link.attr("href"));
        }
        for(Node script : presentScripts){
            presentSources.add(script.attr("src"));
        }

        //get all links and scripts from the front-end file
        SuperVisitor visitor = new SuperVisitor();
        Document frontend = visitor.getSource(BlocksConfig.getFrontEndScripts());
        Elements frontendLinks = frontend.select("link");
        Elements frontendScripts = frontend.select("script");

        //select all links and scripts whose source isn't present in the DOM yet
        Document frontendToBeAdded = new Document(BlocksConfig.getSiteDomain());
        for(Element link : frontendLinks){
            if(!presentSources.contains(link.attr("href"))){
                frontendToBeAdded.appendChild(link);
            }
        }
        for(Element script : frontendScripts){
            if(!presentSources.contains(script.attr("src"))){
                frontendToBeAdded.appendChild(script);
            }
        }

        //append all to the specified DOM
        Element head = DOM.head();
        Element includeAt = head.appendElement("link");
        visitor.includeSource(includeAt, frontendToBeAdded);

    }

}

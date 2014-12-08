package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.jsoup.ClassToStoredInstanceVisitor;
import com.beligum.blocks.core.parsers.jsoup.FileToCacheVisitor;
import com.beligum.blocks.core.parsers.jsoup.ToHtmlVisitor;
import com.beligum.blocks.core.parsers.jsoup.Traversor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
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
    public void cacheTemplatesFromFile(String html) throws ParseException
    {
        Document doc = Jsoup.parse(html);
        Traversor traversor = new Traversor(new FileToCacheVisitor());
        traversor.traverse(doc);

    }

    /**
     * Save a new entity-template-instance of class 'entityTempalteClass' to db, and also all it's children.
     * @param entityTemplateClass
     * @return true if save succeeded
     */
    public URL saveNewEntityTemplateToDb(EntityTemplateClass entityTemplateClass) throws ParseException
    {
        try {
            Document doc = Jsoup.parse(entityTemplateClass.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser());
            ClassToStoredInstanceVisitor visitor = new ClassToStoredInstanceVisitor();
            Traversor traversor = new Traversor(visitor);
            traversor.traverse(doc);
            //TODO BAS SH2: must be checked properly for null-values and empty-strings
            return new URL(visitor.getReferencedId(doc.child(0)));
        }
        catch(MalformedURLException e){
            throw new ParseException("Couldn't construct url for new " + EntityTemplate.class.getSimpleName() + "-instance.", e);
        }

    }

    public String renderEntityInsidePageTemplate(PageTemplate pageTemplate, EntityTemplate entityTemplate) throws ParseException
    {
        Document DOM = Jsoup.parse(pageTemplate.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser());
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

}
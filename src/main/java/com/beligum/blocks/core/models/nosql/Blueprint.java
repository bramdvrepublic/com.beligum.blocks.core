package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.mongo.MongoStoredTemplate;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.blocks.core.parsers.MongoVisitor.template.BlueprintVisitor;
import com.beligum.blocks.core.parsers.SimpleTraversor;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by wouter on 16/03/15.
 */
public abstract class Blueprint extends MongoStoredTemplate
{
    // complete blueprint
    private String fullTemplate;
    // template without properties
    private String template;
    /**true if this is a class which can be created as a new page*/
    private boolean pageBlock;
    /**true if this is a class which can be added as a new block*/
    private boolean addableBlock;
    private boolean canChange;

    private String entityName;

    protected LinkedHashSet<String> links = new LinkedHashSet<>();
    /**the scripts this abstract template needs*/
    protected LinkedHashSet<String> scripts = new LinkedHashSet<>();

    public Blueprint() {

    }

    public Blueprint(Element element, String language) throws ParseException
    {
        super(element, language);
        this.fullTemplate = element.outerHtml();
        this.name = this.blueprintName;
        this.addableBlock = ElementParser.isAddableBlock(element);
        this.pageBlock = ElementParser.isPageBlock(element);
        this.entityName = ElementParser.getTypeOf(element);
        this.canChange = ElementParser.isCanLayout(element);
        String pageTemplateName = ElementParser.getPagetemplateName(element);
        this.pageTemplateName = pageTemplateName != null ? pageTemplateName : this.pageTemplateName;
    }

    @Override
    protected BlueprintVisitor getVisitor() {
        return new BlueprintVisitor();
    }

    @Override
    public BlueprintVisitor parse() throws ParseException
    {
        BlueprintVisitor blueprintVisitor = (BlueprintVisitor)super.parse();
//        SimpleTraversor.traverseProperties(this.transientElement, blueprintVisitor);
        links = blueprintVisitor.getLinks();
        scripts = blueprintVisitor.getScripts();
        this.value = this.transientElement.html();
        try {
//            this.transientElement = parseTemplateToElement(this.getRenderedTemplate(this.isReadOnly()));
        } catch (Exception e)  {
            Logger.error(e);
            throw  new ParseException("Could not parse template string to element");
        }
        return blueprintVisitor;
    }


//    private Element addBlockCssClass(Element element) throws ParseException
//    {
//        String blueprintCssClass = getBlueprintCssClass(element);
//        if(!element.classNames().contains(blueprintCssClass)){
//            Set<String> classNames = element.classNames();
//            LinkedHashSet<String> newClassNames = new LinkedHashSet<>();
//            newClassNames.add(blueprintCssClass);
//            newClassNames.addAll(classNames);
//            element.classNames(newClassNames);
//        }
//        return element;
//    }
//
//    public String getBlueprintCssClass(Node node) throws ParseException
//    {
//        String type = getBlueprintName();
//        if(StringUtils.isEmpty(type)){
//            return null;
//        }
//        else{
//            return ParserConstants.CSS_CLASS_PREFIX + type;
//        }
//    }

//    public Element getTemplateAsElement(boolean readOnly) throws Exception
//    {
//        Element retVal = this.getElement();
//        Blueprint blueprint = TemplateCache.getInstance().getBlueprint(this.blueprint, this.language);
//        if (properties.values().size() > 0) {
//            SimpleTraversor.traverseProperties((Element)retVal, new ToHtmlVisitor(readOnly, blueprint.isReadOnly(), properties, blueprint.getProperties(), language));
//        }
//        return retVal;
//
//    }

    public StringBuilder getRenderedTemplate(boolean readOnly, boolean fetchSingleton)
    {
        StringBuilder retVal = new StringBuilder(this.value);
        if (properties.values().size() > 0) {
            retVal = this.fillTemplateWithProperties(retVal, readOnly, this, fetchSingleton);
        }
        return this.renderInsideElement(retVal, readOnly);
    }

    public boolean isFixed() {
        return !this.canChange;
    }

    public LinkedHashSet<String> getLinks() {
        return this.links;
    }

    public LinkedHashSet<String> getScripts() {
        return this.links;
    }

    public boolean isAddableBlock() {return this.addableBlock;}
    public boolean isPageBlock() {return this.pageBlock;}

    public String getEntityName() {
        return this.entityName;
    }

    public String getTemplate() { return this.value;}

    @Override
    public Blueprint getBlueprint() {
        return this;
    }

    @Override
    protected String findPageTemplateName() {
        return ParserConstants.DEFAULT_PAGE_TEMPLATE;
    }

}
package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.template.BlueprintVisitor;
import org.jsoup.nodes.Element;

import java.util.LinkedHashSet;

/**
 * Created by wouter on 16/03/15.
 */
public abstract class Blueprint extends StoredTemplate
{

    /**
     * true if this is a class which can be created as a new page
     */
    private boolean pageBlock;
    /**
     * true if this is a class which can be added as a new block
     */
    private boolean addableBlock;
    private boolean canChange;
    private String rdfType;
    private String rdfTypePrefix;

    protected LinkedHashSet<String> links = new LinkedHashSet<>();
    /**
     * the scripts this abstract template needs
     */
    protected LinkedHashSet<String> scripts = new LinkedHashSet<>();

    public Blueprint()
    {

    }

    public Blueprint(Element element, String language) throws ParseException
    {
        super(element, language);
        this.name = this.blueprintName;
        this.wrapper = false;

        this.addableBlock = ElementParser.isAddableBlock(element);
        this.pageBlock = ElementParser.isPageBlock(element);
        this.rdfType = ElementParser.getTypeOf(element);
        this.canChange = ElementParser.isCanLayout(element);
        String pageTemplateName = ElementParser.getPagetemplateName(element);
        this.pageTemplateName = pageTemplateName != null ? pageTemplateName : this.pageTemplateName;
    }

    @Override
    protected BlueprintVisitor getVisitor()
    {
        return new BlueprintVisitor();
    }

    @Override
    public BlueprintVisitor parse() throws ParseException
    {
        BlueprintVisitor blueprintVisitor = (BlueprintVisitor) super.parse();
        //        SimpleTraversor.traverseProperties(this.transientElement, blueprintVisitor);
        links = blueprintVisitor.getLinks();
        scripts = blueprintVisitor.getScripts();
        this.value = this.transientElement.html();
        for (BasicTemplate property : this.properties) {
            if (property.isWrapper())
                this.wrapper = true;
        }
        if (this.getProperties().size() > 1 && this.wrapper)
            throw new ParseException("Anonymous blueprint can have only 1 property");
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

    public void setRdfTypeAndPrefix(String property) throws ParseException
    {
        if (property.startsWith("http://")) {
            int lastIndex = property.lastIndexOf("#");
            if (lastIndex == -1) {
                lastIndex = property.lastIndexOf("/");
            }
            String schemaUrl = property.substring(0, lastIndex + 1);
            this.rdfType = property.substring(lastIndex + 1, property.length());
            this.rdfTypePrefix = Blocks.rdfFactory().getPrefixForSchema(schemaUrl);
        }
        else {
            String[] namespacedName = this.rdfType.split(":");
            if (namespacedName.length == 2) {
                this.rdfTypePrefix = namespacedName[0];
                this.name = namespacedName[1];
            }
            else if (namespacedName.length > 2) {
                throw new ParseException("Illegal prefix for property");
            }
            else {
                this.rdfTypePrefix = Blocks.config().getDefaultRdfPrefix();
            }
        }
        int x = 0;
    }

    //    public StringBuilder getRenderedTemplate(boolean readOnly, boolean fetchSingleton)
    //    {
    //        StringBuilder retVal = new StringBuilder(this.value);
    //        if (properties.size() > 0) {
    //            retVal = this.renderTemplate(retVal, readOnly, this, fetchSingleton);
    //        }
    //        return this.renderInsideElement(retVal, readOnly);
    //    }

    public boolean isFixed()
    {
        return !this.canChange;
    }

    public LinkedHashSet<String> getLinks()
    {
        return this.links;
    }

    public LinkedHashSet<String> getScripts()
    {
        return this.links;
    }

    public boolean isAddableBlock()
    {
        return this.addableBlock;
    }
    public boolean isPageBlock()
    {
        return this.pageBlock;
    }

    public String getRdfType()
    {
        return this.rdfType;
    }

    public String getRdfTypePrefix()
    {
        return this.rdfTypePrefix;
    }

    public String getTemplate()
    {
        return this.value;
    }

    @Override
    public Blueprint getBlueprint()
    {
        return this;
    }

    @Override
    protected String findPageTemplateName()
    {
        return ParserConstants.DEFAULT_PAGE_TEMPLATE;
    }

}
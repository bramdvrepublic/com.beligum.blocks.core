package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.jsondb.StringNode;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.template.BlueprintVisitor;
import com.beligum.blocks.utils.UrlTools;
import org.jsoup.nodes.Element;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;

/**
* Created by wouter on 16/03/15.
*/
public class Blueprint extends StoredTemplate
{

    public static final String pageBlock = ParserConstants.BLOCKS_SCHEMA + "pageBlock";
    public static final String addableBlock = ParserConstants.BLOCKS_SCHEMA + "addableBlock";
    public static final String canChange = ParserConstants.BLOCKS_SCHEMA + "canChange";
    public static final String rdfType = ParserConstants.BLOCKS_SCHEMA + "rdfType,";
    public static final String link = ParserConstants.BLOCKS_SCHEMA + "link,";
    public static final String script = ParserConstants.BLOCKS_SCHEMA + "script,";


    /**true if this is a class which can be created as a new page*/
//    private boolean pageBlock;
    /**true if this is a class which can be added as a new block*/
//    private boolean addableBlock;
//    private boolean canChange;
//    private String rdfType;

//    protected LinkedHashSet<String> links = new LinkedHashSet<>();
//    /**the scripts this abstract template needs*/
//    protected LinkedHashSet<String> scripts = new LinkedHashSet<>();

    public Blueprint()
    {

    }

    public Blueprint(Element element, Locale language) throws ParseException
    {
        super(element, language);
        this.setName(this.getBlueprintName());
        this.setWrapper(false);
        this.setBlockId(UrlTools.createLocalResourceId("blueprint", this.getBlueprintName()));
        this.set(ParserConstants.JSONLD_TYPE, new StringNode(ParserConstants.BLOCKS_BLUEPRINT_TYPE));
        this.setAddableBlock(ElementParser.isAddableBlock(element));
        this.setPageBlock(ElementParser.isPageBlock(element));
        this.setRdfType(ElementParser.getTypeOf(element));
        this.setCanChange(ElementParser.isCanLayout(element));
        String pageTemplateName = ElementParser.getPagetemplateName(element);
        this.setPageTemplateName(pageTemplateName != null ? pageTemplateName : this.getPageTemplateName());
    }

    @Override
    protected BlueprintVisitor getVisitor() {
        return new BlueprintVisitor(this);
    }

    @Override
    public BlueprintVisitor parse() throws ParseException
    {
        BlueprintVisitor blueprintVisitor = (BlueprintVisitor)super.parse();
//        SimpleTraversor.traverseProperties(this.transientElement, blueprintVisitor);
//        links = blueprintVisitor.getLinks();
//        scripts = blueprintVisitor.getScripts();
        this.setValue(this.transientElement.html());
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

    public boolean isFixed() {
        return !getBoolean(Blueprint.canChange);
    }

    public void setCanChange(Boolean value) {
        set(Blueprint.canChange, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }


    public LinkedHashSet<String> getLinks() {
        LinkedHashSet<String> links = new LinkedHashSet<>();
        Node listNode = get(Blueprint.link);
        if (listNode != null && listNode.isString()) {
            links.add(listNode.asString());
        } else if (listNode != null && listNode.isIterable()) {
            Iterator<Node> it = listNode.getIterable().iterator();
            while (it.hasNext()) {
                Node node = it.next();
                if (node.isString()) {
                    links.add(node.asString());
                }
            }



        }
        return links;
    }

    public void addLink(String link) {
        add(Blueprint.link, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }

    public void addScript(String script) {
        add(Blueprint.script, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }

    public LinkedHashSet<String> getScripts() {
        LinkedHashSet<String> scripts = new LinkedHashSet<>();
        Node listNode = get(Blueprint.script);
        if (listNode != null && listNode.isString()) {
            scripts.add(listNode.asString());
        } else if (listNode != null && listNode.isIterable()) {
            Iterator<Node> it = listNode.getIterable().iterator();
            while (it.hasNext()) {
                Node node = it.next();
                if (node.isString()) {
                    scripts.add(node.asString());
                }
            }
        }
        return scripts;
    }

    public boolean isAddableBlock() {return getBoolean(Blueprint.addableBlock);
    }
    public void setAddableBlock(Boolean value) {set(Blueprint.addableBlock, Blocks.resourceFactory().asNode(value, Locale.ROOT));}

    public boolean isPageBlock() {return getBoolean(Blueprint.pageBlock);}
    public void setPageBlock(Boolean value) {
        set(Blueprint.pageBlock, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }

    public String getRdfTypes() {
        return this.rdfType;
    }
    public void setRdfType(String value) {
        set(Blueprint.rdfType, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }

    public String getTemplate() { return this.getValue();}

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
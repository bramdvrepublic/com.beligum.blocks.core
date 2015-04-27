package com.beligum.blocks.models;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.jsonld.Node;
import com.beligum.blocks.models.jsonld.StringNode;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.template.BlueprintVisitor;
import com.beligum.blocks.utils.UrlFactory;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.poi.hssf.util.HSSFColor;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

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

    public Blueprint() {

    }

    public Blueprint(Element element, String language) throws ParseException
    {
        super(element, language);
        this.setName(this.getBlueprintName());
        this.setWrapper(false);
        this.set(ParserConstants.JSONLD_TYPE, new StringNode(UrlFactory.createLocalType("Blueprint")));
        this.set(ParserConstants.JSONLD_ID, new StringNode(UrlFactory.createLocalResourceId("Blueprint", this.getBlueprintName())));
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


    public boolean isFixed() {
        return !getBoolean(Blueprint.canChange);
    }

    public void setCanChange(Boolean value) {
        setBoolean(Blueprint.canChange, value);
    }


    public LinkedHashSet<String> getLinks() {
        LinkedHashSet<String> links = new LinkedHashSet<>();
        Node listNode = get(Blueprint.link);
        if (listNode != null && listNode.isString()) {
            links.add(listNode.getString());
        } else if (listNode != null && listNode.isList()) {
            Iterator<Node> it = listNode.getList().iterator();
            while (it.hasNext()) {
                Node node = it.next();
                if (node.isString()) {
                    links.add(node.getString());
                }
            }
        }
        return links;
    }

    public void addLink(String link) {
        addString(Blueprint.link, link, null);
    }

    public void addScript(String script) {
        addString(Blueprint.script, script, null);
    }

    public LinkedHashSet<String> getScripts() {
        LinkedHashSet<String> scripts = new LinkedHashSet<>();
        Node listNode = get(Blueprint.script);
        if (listNode != null && listNode.isString()) {
            scripts.add(listNode.getString());
        } else if (listNode != null && listNode.isList()) {
            Iterator<Node> it = listNode.getList().iterator();
            while (it.hasNext()) {
                Node node = it.next();
                if (node.isString()) {
                    scripts.add(node.getString());
                }
            }
        }
        return scripts;
    }

    public boolean isAddableBlock() {return getBoolean(Blueprint.addableBlock);
    }
    public void setAddableBlock(Boolean value) {setBoolean(Blueprint.addableBlock, value);}

    public boolean isPageBlock() {return getBoolean(Blueprint.pageBlock);}
    public void setPageBlock(Boolean value) {
        setBoolean(Blueprint.pageBlock, value);
    }

    public String getRdfType() {
        return this.rdfType;
    }
    public void setRdfType(String value) {
        setString(Blueprint.rdfType, value, null);
    }

    public String getTemplate() { return this.getValue();}

    @Override
    public Blueprint getBlueprint() {
        return this;
    }

    @Override
    protected String findPageTemplateName() {
        return ParserConstants.DEFAULT_PAGE_TEMPLATE;
    }


}
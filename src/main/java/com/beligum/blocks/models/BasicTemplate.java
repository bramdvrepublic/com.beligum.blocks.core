package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.jsonld.ResourceImpl;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.Traversor;
import com.beligum.blocks.parsers.visitors.template.PropertyVisitor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

//import org.jsoup.nodes.Node;

/**
 * Created by wouter on 16/03/15.
 */
public class BasicTemplate extends ResourceImpl
{
    public static final String blueprintName = ParserConstants.BLOCKS_SCHEMA + "blueprintname";
    public static final String name = ParserConstants.BLOCKS_SCHEMA + "name";
    public static final String language = ParserConstants.BLOCKS_SCHEMA + "language";
    public static final String value = ParserConstants.BLOCKS_SCHEMA + "value";
    public static final String htmlElement = ParserConstants.BLOCKS_SCHEMA + "htmlElement";
    public static final String readonly = ParserConstants.BLOCKS_SCHEMA + "readonly";
    public static final String href = ParserConstants.BLOCKS_SCHEMA + "href";
    public static final String wrapper = ParserConstants.BLOCKS_SCHEMA + "wrapper";
    public static final String inlist = ParserConstants.BLOCKS_SCHEMA + "inlist";
    public static final String entity = ParserConstants.BLOCKS_SCHEMA + "entity";


//    protected String blueprintName;
//    protected String name;
//    protected String language;
//    protected String value;
//    protected HtmlElement element;
//    protected boolean readOnly = false;
//    protected String rdfNamespace;
//    protected URL href;
//    protected boolean wrapper;
//    protected boolean inList;
//
//    protected Entity entity;

    // The current template with properties filtered out
    @JsonIgnore
    protected Element transientElement;

    // The current template with properties
    @JsonIgnore
    protected Element renderedTransientElement;



    public BasicTemplate(ResourceImpl node) {
        super(node);
    }

    public BasicTemplate(Element node) throws ParseException
    {

        this.transientElement = node;
        this.setElement(new HtmlElement(node));
//        this.setName(ElementParser.getProperty(node));
//        if (this.getName() == null) {
//            this.setName("");
//            this.setWrapper(true);
//        }

        this.setInList(ElementParser.hasInList(node));

        this.setBlueprintName(ElementParser.getBlueprintName(node));
        this.setReadOnly(ElementParser.isReadOnly(node));

//        this.setHref(ElementParser.getHref(node));

        if (this.getBlueprintName() == null || this.isWrapper()) {
            this.setValue(node.html());
        }
        if(!(this instanceof Blueprint) && !this.isWrapper()) {
            this.parse();

        }
        //        this.html = node.outerHtml();

    }


    public BasicTemplate() {
    }




    public Boolean isWrapper()
    {
        return getBoolean(BasicTemplate.wrapper);
    }

    public void setWrapper(Boolean value)
    {
        setBoolean(BasicTemplate.wrapper, value);
    }

    public boolean isInList()
    {
        return getBoolean(BasicTemplate.inlist);
    }

    public void setInList(Boolean value)
    {
        setBoolean(BasicTemplate.inlist, value);
    }

    public PropertyVisitor parse() throws ParseException
    {
        PropertyVisitor propertyVisitor = this.getVisitor();
        Traversor.traverseProperties(this.transientElement, propertyVisitor);
//        this.getWrappedResource().putAll(propertyVisitor.getProperties());
        this.setValue(this.transientElement.html());
        return propertyVisitor;
    }

    public void addProperty(String name, Element element) throws ParseException
    {
        BasicTemplate template = new BasicTemplate(element);
        add(name, template);
    }


    protected PropertyVisitor getVisitor() {
        return new PropertyVisitor(this);
    }

    public void setValue(String value) {
        setString(BasicTemplate.value, value, null);
    }


    public String getBlueprintName() {
        return getString(BasicTemplate.blueprintName);
    }

    public void setBlueprintName(String value) {
        setString(BasicTemplate.blueprintName, value);
    }

    public Blueprint getBlueprint() {
        Blueprint retVal = null;
        if (this.getBlueprintName() != null) {
            retVal = Blocks.templateCache().getBlueprint(this.getBlueprintName());
        }
        return retVal;
    }

    public String getName() {
        return getString(BasicTemplate.name);
    }

    public void setName(String value) {
        setString(BasicTemplate.name, value);
    }


    public HtmlElement getElement() {
        HtmlElement retval = null;
        com.beligum.blocks.models.jsonld.Node element = getFirst(BasicTemplate.htmlElement);
        if (element != null && element.isResource()) {
            retval = new HtmlElement((ResourceImpl)element);
        }
        return retval;
    }

    public void setElement(HtmlElement el) {
        add(BasicTemplate.htmlElement, el);
    }

    public boolean isReadOnly() {
        return getBoolean(BasicTemplate.readonly);
    }

    public void setReadOnly(Boolean value) {
        setBoolean(BasicTemplate.readonly, value);
    }

    public static Element parse(String html){
        Element retVal = new Element(Tag.valueOf("div"), Blocks.config().getSiteDomain().toString());
        Document parsed = Jsoup.parse(html, Blocks.config().getSiteDomain().toString(), Parser.htmlParser());
        /*
         * If only part of a html-file is being parsed (which starts f.i. with a <div>-tag), Jsoup will add <html>-, <head>- and <body>-tags, which is not what we want
         * Thus if the head (or body) is empty, but the body (or head) is not, we only want the info in the body (or head).
         */
        if (parsed.head().childNodes().isEmpty() && !parsed.body().childNodes().isEmpty()) {
            for (Node child : parsed.body().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if (parsed.body().childNodes().isEmpty() && !parsed.head().childNodes().isEmpty()) {
            for (Node child : parsed.head().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if (parsed.body().childNodes().isEmpty() && parsed.body().childNodes().isEmpty()) {
            //add nothing to the retVal so an empty document will be returned
        }
        else {
            retVal = parsed;
        }

        if (retVal.children().size() == 1) {
            retVal = retVal.children().first();
        }

        return retVal;
    }

    public static String getPropertyForKey(String key)
    {
        String retVal = key;
        String[] splittedKey = key.split("/");
        if (splittedKey.length > 1) {
            retVal = splittedKey[0];
        }
        return retVal;
    }



    public String getValue() {
        String retVal = getString(BasicTemplate.value);
        if (retVal == null) retVal = "";
        return retVal;
    }

}

package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.jsonld.OrientResource;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.beligum.blocks.models.jsonld.jsondb.ResourceImpl;
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

import java.util.Locale;

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



    public BasicTemplate(Resource node) {
        super(node);
    }

    public BasicTemplate(Element node) throws ParseException
    {

        this.transientElement = node;
        this.setElement(new HtmlElement(node));

        this.setInList(ElementParser.hasInList(node));

        this.setBlueprintName(ElementParser.getBlueprintName(node));
        this.setReadOnly(ElementParser.isReadOnly(node));

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
        set(BasicTemplate.wrapper, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }

    public boolean isInList()
    {
        return getBoolean(BasicTemplate.inlist);
    }

    public void setInList(Boolean value)
    {
        set(BasicTemplate.inlist, Blocks.resourceFactory().asNode(value, Locale.ROOT));
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

    //    public StringBuilder getRenderedTemplate(boolean readOnly, boolean fetchSingeltons)
    //    {
    //        StringBuilder retVal = new StringBuilder(this.value);
    //        Blueprint blueprint = getBlueprint();
    //
    //        // TODO fix dynamic templates
    //        if (blueprint == null) {
    //            // Dynamic block
    //            //
    //        } else if (Blocks.blockHandler().isDynamicBlock(this.getBlueprintName())) {
    //            return Blocks.blockHandler().getDynamicBlock(this.getBlueprintName()).renderContent(this);
    //        } else if (readOnly) {
    //            retVal = blueprint.getRenderedTemplate(readOnly, fetchSingeltons);
    //        } else {
    //            // check if blueprint is readonly -> all properties read only except not-read
    //            if (blueprint.isFixed() || readOnly) retVal = new StringBuilder(blueprint.getTemplate());
    //            ArrayList<BasicTemplate> mixedProperties = mixProperties(readOnly, blueprint.isReadOnly(), properties, blueprint.getProperties());
    //            retVal = this.renderTemplate(new StringBuilder(retVal), readOnly, blueprint, fetchSingeltons);
    //
    //        }
    //        return this.renderInsideElement(retVal, readOnly);
    //    }

    //    public void fillTemplateValuesWithEntityValues(Entity entity, PropertyFinder<EntityField> propertyFinder) {
    //        // find each property
    //        if (this.getProperties().size() > 0) {
    //            for (BasicTemplate template : this.getProperties()) {
    //                String key = template.getTemplateTagName();
    //                EntityField property = propertyFinder.getProperty(key, entity.getProperties());
    //                if (property != null && property instanceof Entity) {
    //                    template.fillTemplateValuesWithEntityValues((Entity) property, new PropertyFinder<EntityField>());
    //                }
    //                else if (property != null) {
    //                    template.setValue(property.getValue());
    //                    propertyFinder.propertyFound(key);
    //                } else {
    //                    Logger.debug("Could not find a value in the entity to fill this template");
    //                }
    //            }
    //        } else if (this.getBlueprintName() != null) {
    //            // Fill this template itself
    //           EntityField field = propertyFinder.getProperty(this.getTemplateTagName(), entity.getProperties());
    //           if (field != null && field instanceof Entity) {
    //               this.fillTemplateValuesWithEntityValues((Entity)field, propertyFinder);
    //           } else if (field != null) {
    //               this.setValue(field.getValue());
    //               propertyFinder.propertyFound(this.getTemplateTagName());
    //           } else {
    //               Logger.debug("Could not find a value in the entity to fill this template");
    //           }
    //        } else {
    //            Logger.debug("This is a blueprint without properties so we can't fill anything");
    //        }
    //    }
    protected PropertyVisitor getVisitor() {
        return new PropertyVisitor(this);
    }

    public void setValue(String value) {
        set(BasicTemplate.value, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }


    //    protected String findNextPropertyInTemplate(StringBuilder template) {
    //        String retVal = null;
    //        int start = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_START);
    //        if (start > -1) {
    //            int end = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_END);
    //            if (end > -1 && end > start) {
    //                start += ParserConstants.TEMPLATE_PROPERTY_START.length();
    //                retVal = template.substring(start, end);
    //            }
    //        }
    //        return retVal;
    //    }
    //
    //    protected void replacePropertyWithValue(StringBuilder template, String property, StringBuilder value) {
    //        StringBuilder retVal = template;
    //        String propertyKey = ParserConstants.TEMPLATE_PROPERTY_START + property + ParserConstants.TEMPLATE_PROPERTY_END;
    //        int index = retVal.indexOf(propertyKey);
    //        if (index >= 0) {
    //            retVal = retVal.replace(index, index + propertyKey.length(), value.toString());
    //        }
    //    }

    //    protected ArrayList<BasicTemplate> mixProperties(boolean parentReadOnly, boolean blueprintReadOnly, ArrayList<BasicTemplate> instanceProperties, ArrayList<BasicTemplate> blueprintProperties)
    //
    //    {
    //        ArrayList<BasicTemplate> retVal = new ArrayList<>();
    //
    //        if (parentReadOnly) {
    //            retVal = blueprintProperties;
    //        } else {
    //            PropertyFinder<BasicTemplate> propertyFinder = new PropertyFinder();
    //            for (BasicTemplate property : instanceProperties) {
    //                String key = property.getTemplateTagName();
    //                // get numbered property
    //                BasicTemplate instanceProperty = propertyFinder.getProperty(key, instanceProperties);
    //                BasicTemplate blueprintProperty = propertyFinder.getProperty(key, blueprintProperties);
    //                if (blueprintProperty == null && propertyFinder.getFirstProperty(key, blueprintProperties) != null) {
    //                    blueprintProperty = propertyFinder.getFirstProperty(key, blueprintProperties);
    //                } else if (instanceProperty.getBlueprintName() != null) {
    //                    blueprintProperty = Blocks.templateCache().getBlueprint(instanceProperty.getBlueprintName(), instanceProperty.getLanguage());
    //                }
    //
    //                if (blueprintProperty != null && (blueprintReadOnly || blueprintProperty.isReadOnly())) {
    //                    retVal.add(blueprintProperty);
    //                } else if (blueprintProperty == null && blueprintReadOnly) {
    //                    // TODO put nothing or instance?
    //                    retVal.add(instanceProperty);
    //                } else {
    //                    retVal.add(instanceProperty);
    //                }
    //                propertyFinder.propertyFound(key);
    //
    //            }
    //        }
    //        return retVal;
    //
    //    }

    //    @JsonIgnore
    //    public Element getTemplateAsElement()
    //    {
    //        Element retVal = null;
    //        if (this.transientElement == null) {
    //            this.transientElement = parse(this.renderInsideElement(new StringBuilder(this.value), false).toString());
    //        }
    //        retVal = this.transientElement.clone();
    //        if (retVal == null) retVal = new Element(Tag.valueOf("div"), null);
    //        return retVal;
    //    }

    public ArrayList<BasicTemplate> getProperties()
    {
        return this.properties;
    }
    
    public String getBlueprintName() {
        return getString(BasicTemplate.blueprintName);
    }

    public void setBlueprintName(String value) {
        set(BasicTemplate.blueprintName, Blocks.resourceFactory().asNode(value, Locale.ROOT));
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
        set(BasicTemplate.name, Blocks.resourceFactory().asNode(value, Locale.ROOT));
    }


    public HtmlElement getElement() {
        HtmlElement retval = null;
        com.beligum.blocks.models.jsonld.interfaces.Node element = getFirst(BasicTemplate.htmlElement);
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
        set(BasicTemplate.readonly, Blocks.resourceFactory().asNode(value, Locale.ROOT));
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

    @Override
    public Object getDBId()
    {
        return null;
    }
    @Override
    public Object setDBId()
    {
        return null;
    }
}

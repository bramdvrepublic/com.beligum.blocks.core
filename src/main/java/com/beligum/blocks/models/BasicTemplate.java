package com.beligum.blocks.models;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.interfaces.NamedProperty;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.template.PropertyVisitor;
import com.beligum.blocks.parsers.Traversor;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.utils.PropertyFinder;
import com.beligum.blocks.utils.URLFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

import java.net.URL;
import java.util.*;

/**
 * Created by wouter on 16/03/15.
 */
public class BasicTemplate implements NamedProperty
{
    protected String blueprintName;
    protected String name;
    protected String language;
    protected String value;
    protected HtmlElement element;
    protected boolean readOnly = false;
    protected String rdfNamespace;
    protected URL href;


    protected Entity entity;

    // The current template with properties filtered out
    @JsonIgnore
    protected Element transientElement;

    // The current template with properties
    @JsonIgnore
    protected Element renderedTransientElement;

    // set after reading html from client to indicate this is the content element of the page
    @JsonIgnore
    protected boolean templateContent = false;

    protected ArrayList<BasicTemplate> properties = new ArrayList<BasicTemplate>();


    public BasicTemplate(Element node, String language) throws ParseException
    {
        if (language == null) {
            this.language = Blocks.config().getDefaultLanguage();
        } else {
            this.language = language;
        }

        this.transientElement = node;
        this.element = new HtmlElement(node);
        this.name = ElementParser.getProperty(node);

        this.blueprintName = ElementParser.getBlueprintName(node);
        this.readOnly = ElementParser.isReadOnly(node);

        this.href = ElementParser.getHref(node);

        this.templateContent = false;

        if (this.getBlueprintName() == null) {
            this.value = node.html();
        }
        if(!(this instanceof Blueprint)) {
            this.parse();
        }
//        this.html = node.outerHtml();

    }


    public BasicTemplate() {
        this.value = "";
    }

    public PropertyVisitor parse() throws ParseException
    {
        PropertyVisitor propertyVisitor = this.getVisitor();
        Traversor.traverseProperties(this.transientElement, propertyVisitor);
        properties = propertyVisitor.getProperties();
        this.value = this.transientElement.html();
        return propertyVisitor;
    }



    protected PropertyVisitor getVisitor() {
        return new PropertyVisitor();
    }

//    public StringBuilder getRenderedTemplate(boolean readOnly, boolean fetchSingeltons)
//    {
//        StringBuilder retVal = new StringBuilder(this.value);
//        Blueprint blueprint = getBlueprint();
//
//        // TODO fix dynamic blocks
//        if (blueprint == null) {
//            // Dynamic block
//            //
//        } else if (Blocks.blockHandler().isDynamicBlock(this.getBlueprintName())) {
//            return Blocks.blockHandler().getDynamicBlock(this.getBlueprintName()).render(this);
//        } else if (readOnly) {
//            retVal = blueprint.getRenderedTemplate(readOnly, fetchSingeltons);
//        } else {
//            // check if blueprint is readonly -> all properties read only except not-read
//            if (blueprint.isFixed() || readOnly) retVal = new StringBuilder(blueprint.getTemplate());
//            ArrayList<BasicTemplate> mixedProperties = mixProperties(readOnly, blueprint.isReadOnly(), properties, blueprint.getProperties());
//            retVal = this.fillTemplateWithProperties(new StringBuilder(retVal), readOnly, blueprint, fetchSingeltons);
//
//        }
//        return this.renderInsideElement(retVal, readOnly);
//    }

//    public void fillTemplateValuesWithEntityValues(Entity entity, PropertyFinder<EntityField> propertyFinder) {
//        // find each property
//        if (this.getProperties().size() > 0) {
//            for (BasicTemplate template : this.getProperties()) {
//                String key = template.getName();
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
//           EntityField field = propertyFinder.getProperty(this.getName(), entity.getProperties());
//           if (field != null && field instanceof Entity) {
//               this.fillTemplateValuesWithEntityValues((Entity)field, propertyFinder);
//           } else if (field != null) {
//               this.setValue(field.getValue());
//               propertyFinder.propertyFound(this.getName());
//           } else {
//               Logger.debug("Could not find a value in the entity to fill this template");
//           }
//        } else {
//            Logger.debug("This is a blueprint without properties so we can't fill anything");
//        }
//    }

    public void setValue(String value) {
        this.value = value;
    }

//    protected StringBuilder fillTemplateWithProperties(StringBuilder template, boolean readOnly, BasicTemplate blueprint, boolean fetchSingletons)
//    {
//        // find property
//        String nextProperty = findNextPropertyInTemplate(template);
//        PropertyFinder<BasicTemplate> propertyFinder = new PropertyFinder();
//        while (nextProperty != null) {
//            BasicTemplate property = propertyFinder.getProperty(nextProperty, properties);
//            BasicTemplate blueprintProperty = propertyFinder.getProperty(nextProperty, blueprint.getProperties());
//
//
//            if (property == null && blueprintProperty != null) {
//                property = propertyFinder.getProperty(nextProperty, blueprint.getProperties());
//            } else if (property != null && property instanceof Singleton && fetchSingletons) {
//                BlockId singletonId = ((Singleton)property).getId();
//                StoredTemplate singleton = Blocks.database().fetch(singletonId, this.language, Blocks.factory().getSingletonClass());
//                if (singleton != null) property = singleton;
//            }
//            propertyFinder.propertyFound(nextProperty);
//
//            if (property != null) {
//                boolean propertyReadOnly = readOnly;
//                if (!readOnly) {
//                    if (blueprintProperty != null && blueprint.isReadOnly() || property.isReadOnly()) {
//                        propertyReadOnly = true;
//                    }
//                }
//                StringBuilder propertyValue = property.getRenderedTemplate(propertyReadOnly, fetchSingletons);
//                replacePropertyWithValue(template, nextProperty, propertyValue);
//
//
//            } else {
//                replacePropertyWithValue(template, nextProperty, new StringBuilder());
//
//            }
//
//            nextProperty = findNextPropertyInTemplate(template);
//        }
//        return template;
//    }

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
//                String key = property.getName();
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



    public String renderStartElement(boolean readOnly, boolean showResource)
    {

        StringBuilder retVal = new StringBuilder();
        retVal.append("<").append(this.element.getTag()).append(" ");
        HashSet<String> attributes = new HashSet<String>(this.element.getAttributes().keySet());
        Blueprint blueprint = this.getBlueprint();

        // Set the right attributes on the element
        if (readOnly) {
            if (blueprint != null) {
                attributes = new HashSet<String>(blueprint.getElement().getAttributes().keySet());
            }
            attributes.remove(ParserConstants.CAN_EDIT_PROPERTY);
            attributes.remove(ParserConstants.CAN_LAYOUT);

        } else if (!readOnly && blueprint != null){
            // property is can edit
            attributes.remove(ParserConstants.CAN_EDIT_PROPERTY);
            attributes.addAll(blueprint.getElement().getAttributes().keySet());
        } else {
            attributes.add(ParserConstants.CAN_EDIT_PROPERTY);
        }

        attributes.remove(ParserConstants.REFERENCE_TO);
        attributes.remove(ParserConstants.RESOURCE);
        attributes.remove(ParserConstants.LANGUAGE);

        for (String key: attributes) {
            String value = this.getElement().getAttributes().get(key);
            if (value == null && blueprint != null)  value = blueprint.getElement().getAttributes().get(key);

            if (key.equals(ParserConstants.PROPERTY) && blueprint != null && blueprint.getRdfType() == null) {
                retVal.append(addAtribute(ParserConstants.BLUEPRINT_PROPERTY, this.name));
            } else if (key.equals(ParserConstants.PROPERTY)) {
                retVal.append(addAtribute(ParserConstants.PROPERTY, this.name));
            } else if (key.equals("class") && blueprint != null) {
                LinkedHashSet<String> classes = new LinkedHashSet<>();
                classes.addAll(Arrays.asList((ParserConstants.CSS_CLASS_PREFIX + blueprint.getName() + " " + value).split(" ")));
                classes.addAll(Arrays.asList(blueprint.getElement().getAttributes().get(key).split(" ")));

                retVal.append(addAtribute(key, StringUtils.join(classes.toArray(), " ")));
            } else if (key.equals(ParserConstants.TYPE_OF)) {
                retVal.append(addAtribute(ParserConstants.TYPE_OF, blueprint.getRdfType()));
            }
            else
            {
                retVal.append(addAtribute(key, this.element.getAttributes().get(key)));
            }
        }

        if (!showResource) {
            // do nothing
        } else if (this instanceof StoredTemplate) {
            if (((StoredTemplate)this).getId() != null) {
                // add resource
                if (this.entity != null) {
                    retVal.append(addAtribute(ParserConstants.RESOURCE, ((StoredTemplate)this).getId().toString()));
                }
//                else {
//                    retVal.append(addAtribute(ParserConstants.REFERENCE_TO, ((StoredTemplate)this).getId().toString()));
//                }
            }
        } else if (this.entity != null && this.entity.getId() != null){
            retVal.append(addAtribute(ParserConstants.RESOURCE, this.entity.getId().toString()));
        }

        retVal.append(addAtribute(ParserConstants.LANGUAGE, language) + ">");


        return retVal.toString();
    }

    private String addAtribute(String key, String value) {
        String retVal = key;
        if (!(value == null || value.isEmpty())) {
            retVal+= "=\"" + value + "\"";
        } else {
            retVal+= "=\"\"";
        }
        retVal += " ";
        return retVal;
    }


    public String renderEndElement() {
        String retVal = "</" + this.element.getTag() + ">";
        return retVal;
    }



    public ArrayList<BasicTemplate>  getProperties() {
        return this.properties;
    }

    public String getBlueprintName() {
        return this.blueprintName;
    }

    @JsonIgnore
    public Blueprint getBlueprint() {
        Blueprint retVal = null;
        if (this.getBlueprintName() != null) {
            retVal = Blocks.templateCache().getBlueprint(this.getBlueprintName(), this.language);
        }
        return retVal;
    }

    public String getName() {
        return this.name;
    }

    public HtmlElement getElement() {
        return this.element;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    @JsonIgnore
    public Entity getEntityProperties(Entity entity)
    {
        Entity entityToFill = entity;
        if (this.getBlueprintName() == null) {
            // this is a field so store the value
            // Todo also catch content, language, datatype
            if (element.getAttributes().containsKey(ParserConstants.SRC)) {
                entityToFill.addProperty(new EntityField(this.name, element.getAttributes().get(ParserConstants.SRC)));
                entityToFill.addProperty(new EntityField(URLFactory.makeAbsolute(this.name, ParserConstants.CAPTION), value));
            } else if (element.getAttributes().containsKey(ParserConstants.HREF)) {
                entityToFill.addProperty(new EntityField(this.name, element.getAttributes().get(ParserConstants.HREF)));
                entityToFill.addProperty(new EntityField(URLFactory.makeAbsolute(this.name, ParserConstants.CAPTION), value));
            } else if (element.getAttributes().containsKey(ParserConstants.CONTENT)) {
                entityToFill.addProperty(new EntityField(this.name, element.getAttributes().get(ParserConstants.CONTENT)));
                entityToFill.addProperty(new EntityField(URLFactory.makeAbsolute(this.name, ParserConstants.CAPTION), value));
            } else {
                entityToFill.addProperty(new EntityField(this.name, value));
            }
        } else {
            // if blueprint has typeOf then create new entity
            Blueprint blueprint = Blocks.templateCache().getBlueprint(this.getBlueprintName(), this.language);
            String entityName = blueprint.getRdfType();
            if (entityName != null) {
                this.entity = Blocks.factory().createEntity(entityName, this.language);
                entityToFill = this.entity;
            }
            // Add properties to typeof
            for (BasicTemplate property : properties) {
                entityToFill = property.getEntityProperties(entityToFill);
            }
            if (entityToFill != entity) entity.addProperty(entityToFill);

        }
        return entity;
    }

    public List<Entity> getRootEntities()
    {
        ArrayList<Entity> retVal = new ArrayList<Entity>();
        if (this.getBlueprintName() != null) {
            Blueprint blueprint = Blocks.templateCache().getBlueprint(this.getBlueprintName(), this.language);
            if (blueprint != null && blueprint.getRdfType() != null) {
                this.entity = Blocks.factory().createEntity(blueprint.getRdfType(), this.language);

                // Does this allready contain an id, then use this as
                if (this instanceof StoredTemplate && ((StoredTemplate)this).getId() != null) {
                    this.entity.setId(((StoredTemplate)this).getId());
                }
                retVal.add(this.entity);
                for (BasicTemplate template: this.properties) {
                    template.getEntityProperties(this.entity);
                }
            } else {
                for (BasicTemplate template : this.properties) {
                    retVal.addAll(template.getRootEntities());
                }
            }
        }
        return retVal;
    }

    public String getLanguage() {
        String retVal = this.language;
        if (retVal == null) {
            retVal = Blocks.config().getDefaultLanguage();
        }
        return retVal;
    }

    public static Element parse(String html){
        Element retVal = new Element(Tag.valueOf("div"), Blocks.config().getSiteDomain());
        Document parsed = Jsoup.parse(html, Blocks.config().getSiteDomain(), Parser.htmlParser());
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

        if (retVal.children().size() == 1) {
            retVal = retVal.children().first();
        }

        return retVal;
    }

    public static String getPropertyForKey(String key) {
        String retVal = key;
        String[] splittedKey = key.split("/");
        if (splittedKey.length > 1) {
            retVal = splittedKey[0];
        }
        return retVal;
    }



    public boolean isTemplateContent() {
        return this.templateContent;
    }

    public void isTemplateContent(boolean value) {
        this.templateContent = value;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof BasicTemplate))
            return false;

        BasicTemplate that = (BasicTemplate) o;

        if (!properties.equals(that.properties))
            return false;
        if (!value.equals(that.value))
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = value.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }


}

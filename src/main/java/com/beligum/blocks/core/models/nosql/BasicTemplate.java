package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.mongo.MongoEntity;
import com.beligum.blocks.core.mongo.MongoStoredTemplate;
import com.beligum.blocks.core.mongocache.TemplateCache;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.blocks.core.parsers.MongoVisitor.template.PropertyVisitor;
import com.beligum.blocks.core.parsers.SimpleTraversor;
import com.beligum.core.framework.models.AbstractJsonObject;
import com.beligum.core.framework.utils.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

import java.util.*;

/**
 * Created by wouter on 16/03/15.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
                @JsonSubTypes.Type(value=BasicTemplate.class, name="BasicTemplate"),
                @JsonSubTypes.Type(value=StoredTemplate.class, name="Storedtemplate"),
                @JsonSubTypes.Type(value=MongoStoredTemplate.class, name="MongoStoredTemplate")

})
public class BasicTemplate
{
    protected String blueprintName;
    protected String name;
    protected String language;
    protected String value;
    protected HtmlElement element;


    protected String singletonName;
    protected boolean singleton;
    protected boolean readOnly = false;


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

    protected LinkedHashMap<String, BasicTemplate> properties = new LinkedHashMap<>();


    public BasicTemplate(Element node, String language) throws ParseException
    {
        if (language == null) {
            this.language = BlocksConfig.getDefaultLanguage();
        } else {
            this.language = language;
        }

        this.transientElement = node;
        this.element = new HtmlElement(node);
        this.name = ElementParser.getProperty(node);
        this.blueprintName = ElementParser.getBlueprintName(node);
        this.readOnly = ElementParser.isReadOnly(node);
        this.singleton = ElementParser.isSingleton(node);
        if (this.singleton) {
            this.singletonName = ElementParser.getSingletonName(node);
        }



        this.templateContent = false;

        if (this.getBlueprintName() == null) {
            this.value = node.html();
        }
        if(!(this instanceof Blueprint)) {
            this.parse();
        }
//        this.html = node.outerHtml();


    }

    public BasicTemplate(Element node) throws ParseException
    {
        // check if language attribute else default language
        this(node, ElementParser.getLanguage(node));
    }

    public BasicTemplate() {
        this.value = "";
    }

    public PropertyVisitor parse() throws ParseException
    {
        PropertyVisitor propertyVisitor = this.getVisitor();
        SimpleTraversor.traverseProperties(this.transientElement, propertyVisitor);
        properties = propertyVisitor.getProperties();
        this.value = this.transientElement.html();
        return propertyVisitor;
    }


    protected PropertyVisitor getVisitor() {
        return new PropertyVisitor();
    }

    public StringBuilder getRenderedTemplate(boolean readOnly, boolean fetchSingeltons)
    {
        StringBuilder retVal = new StringBuilder(this.value);
        Blueprint blueprint = getBlueprint();

        // TODO fix dynamic blocks
        if (blueprint == null) {
            // Dynamic block
        } else if (readOnly) {
            retVal = new StringBuilder(blueprint.getRenderedTemplate(readOnly, fetchSingeltons));
        } else {
            // check if blueprint is readonly -> all properties read only except not-read
            if (blueprint.isFixed() || readOnly) retVal = new StringBuilder(blueprint.getTemplate());
            LinkedHashMap<String, BasicTemplate> mixedProperties = mixProperties(readOnly, blueprint.isReadOnly(), properties, blueprint.getProperties());
            retVal = this.fillTemplateWithProperties(new StringBuilder(retVal), readOnly, blueprint, fetchSingeltons);

        }
        return this.renderInsideElement(retVal, readOnly);
    }

    public void fillTemplateValuesWithEntityValues(Entity entity, Set<String> usedProperties) {
        // find each property
        if (this.getProperties().size() > 0) {
            for (BasicTemplate template : this.getProperties().values()) {
                String key = ElementParser.getPropertyKey(template.getName(), usedProperties);
                if (entity.getProperties().get(key) != null && entity.getProperties().get(key) instanceof Entity) {
                    template.fillTemplateValuesWithEntityValues((Entity)entity.getProperties().get(key), new HashSet<String>());
                }
                else if (entity.getProperties().get(key) != null) {
                    template.setValue((String) entity.getProperties().get(key));
                    usedProperties.add(key);
                } else {
                    Logger.debug("Could not find a value in the entity to fill this template");
                }
            }
        } else if (this.getBlueprintName() != null) {
           String key = ElementParser.getPropertyKey(this.getName(), usedProperties);
           if (entity.getProperties().get(key) != null && entity.getProperties().get(key) instanceof Entity) {
               this.fillTemplateValuesWithEntityValues((Entity)entity.getProperties().get(key), usedProperties);
           } else if (entity.getProperties().get(key) != null) {
               this.setValue((String) entity.getProperties().get(key));
               usedProperties.add(key);
           } else {
               Logger.debug("Could not find a value in the entity to fill this template");
           }
        } else {
            Logger.debug("This is a blueprint without properties so we can't fill anything");
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    protected StringBuilder fillTemplateWithProperties(StringBuilder template, boolean readOnly, BasicTemplate blueprint, boolean fetchSingletons)
    {
        // find property
        String nextProperty = findNextPropertyInTemplate(template);
        while (nextProperty != null) {
            BasicTemplate property = properties.get(nextProperty);

            if (property == null && blueprint.getProperties().get(nextProperty) != null) {
                property = blueprint.getProperties().get(nextProperty);
            } else if (property != null && property.isSingleton() && fetchSingletons) {
                BlockId singletonId = BlocksConfig.getInstance().getDatabase().getIdForString(property.getSingleton());
                property = BlocksConfig.getInstance().getDatabase().fetchSingletonTemplate(singletonId, this.language);
            }

            if (property != null) {

                BasicTemplate blueprintProperty = blueprint.getProperties().get(nextProperty);

                boolean propertyReadOnly = readOnly;
                if (!readOnly) {
                    if (blueprintProperty != null && blueprint.isReadOnly() || property.isReadOnly()) {
                        propertyReadOnly = true;
                    }
                }
                StringBuilder propertyValue = property.getRenderedTemplate(propertyReadOnly, fetchSingletons);
                replacePropertyWithValue(template, nextProperty, propertyValue);
                nextProperty = findNextPropertyInTemplate(template);

            } else {
                replacePropertyWithValue(template, nextProperty, new StringBuilder());
                nextProperty = findNextPropertyInTemplate(template);
            }
        }
        return template;
    }

    protected String findNextPropertyInTemplate(StringBuilder template) {
        String retVal = null;
        int start = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_START);
        if (start > -1) {
            int end = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_END);
            if (end > -1 && end > start) {
                start += ParserConstants.TEMPLATE_PROPERTY_START.length();
                retVal = template.substring(start, end);
            }
        }
        return retVal;
    }

    protected void replacePropertyWithValue(StringBuilder template, String property, StringBuilder value) {
        StringBuilder retVal = template;
        String propertyKey = ParserConstants.TEMPLATE_PROPERTY_START + property + ParserConstants.TEMPLATE_PROPERTY_END;
        int index = retVal.indexOf(propertyKey);
        if (index >= 0) {
            retVal = retVal.replace(index, index + propertyKey.length(), value.toString());
        }
    }

    protected LinkedHashMap<String, BasicTemplate> mixProperties(boolean parentReadOnly, boolean blueprintReadOnly, LinkedHashMap<String, BasicTemplate> instanceProperties, LinkedHashMap<String, BasicTemplate> blueprintProperties)

    {
        LinkedHashMap<String, BasicTemplate> retVal = new LinkedHashMap<>();

        if (parentReadOnly) {
            retVal = blueprintProperties;
        } else {

            for (String propertyKey : instanceProperties.keySet()) {
                BasicTemplate instanceProperty = instanceProperties.get(propertyKey);
                BasicTemplate blueprintProperty = blueprintProperties.get(propertyKey);
                if (blueprintProperty == null && blueprintProperties.get(BasicTemplate.getPropertyForKey(propertyKey)) != null) {
                    blueprintProperty = blueprintProperties.get(BasicTemplate.getPropertyForKey(propertyKey));
                } else if (instanceProperty.getBlueprintName() != null) {
                    blueprintProperty = BlocksConfig.getInstance().getTemplateCache().getBlueprint(instanceProperty.getBlueprintName(), instanceProperty.getLanguage());
                }

                if (blueprintProperty != null && (blueprintReadOnly || blueprintProperty.isReadOnly())) {
                    retVal.put(propertyKey, blueprintProperty);
                } else if (blueprintProperty == null && blueprintReadOnly) {
                    // TODO put nothing or instance?
                    retVal.put(propertyKey, instanceProperty);
                } else {
                    retVal.put(propertyKey, instanceProperty);
                }


            }
        }
        return retVal;

    }


    @JsonIgnore
    public Element getTemplateAsElement()
    {
        Element retVal = null;
        if (this.transientElement == null) {
            this.transientElement = parse(this.renderInsideElement(new StringBuilder(this.value), false).toString());
        }
        retVal = this.transientElement.clone();
        if (retVal == null) retVal = new Element(Tag.valueOf("div"), null);
        return retVal;
    }

    @JsonIgnore
    public Element getRenderedTemplateAsElement()
    {
        Element retVal = null;
        if (this.renderedTransientElement == null) {
            this.renderedTransientElement = parse(this.getRenderedTemplate(false, false).toString());
        }
        retVal = this.renderedTransientElement.clone();
        if (retVal == null) retVal = new Element(Tag.valueOf("div"), null);
        return retVal;
    }

    public String renderStartElement(boolean readOnly)
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

        for (String key: attributes) {
            String value = this.getElement().getAttributes().get(key);
            if (value == null && blueprint != null)  value = blueprint.getElement().getAttributes().get(key);

            if (key.equals(ParserConstants.PROPERTY) && blueprint != null) {
                retVal.append(addAtribute(ParserConstants.BLUEPRINT_PROPERTY, value));
            } else if (key.equals("class") && blueprint != null) {
                LinkedHashSet<String> classes = new LinkedHashSet<>();
                classes.addAll(Arrays.asList((ParserConstants.CSS_CLASS_PREFIX + blueprint.getName() + " " + value).split(" ")));
                classes.addAll(Arrays.asList(blueprint.getElement().getAttributes().get(key).split(" ")));

                retVal.append(addAtribute(key, StringUtils.join(classes.toArray(), " ")));
            } else {
                retVal.append(addAtribute(key, this.element.getAttributes().get(key)));
            }
        }

        if (this.templateContent) {
            // do nothing
        } else if (this instanceof StoredTemplate) {
            if (((StoredTemplate)this).getId() != null) {
                // add resource
                if (this.entity != null) {
                    retVal.append(addAtribute(ParserConstants.RESOURCE, ((StoredTemplate)this).getId().toString()));
                } else {
                    retVal.append(addAtribute(ParserConstants.REFERENCE_TO, ((StoredTemplate)this).getId().toString()));
                }
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
        }
        retVal += " ";
        return retVal;
    }


    public String renderEndElement(String language) {
        String retVal = "</" + this.element.getTag() + ">";
        return retVal;
    }

    public StringBuilder renderInsideElement(StringBuilder template, boolean readonly)
    {
        template.insert(0, this.renderStartElement(readonly));
        template.append(renderEndElement(language));
        return template;
    }




    public LinkedHashMap<String, BasicTemplate>  getProperties() {
        return this.properties;
    }

    public String getBlueprintName() {
        return this.blueprintName;
    }

    @JsonIgnore
    public Blueprint getBlueprint() {
        Blueprint retVal = null;
        if (this.getBlueprintName() != null) {
            retVal = BlocksConfig.getInstance().getTemplateCache().getBlueprint(this.getBlueprintName(), this.language);
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
        // TODO blueprint should be null
        if (this.getBlueprintName() == null) {
            // this is a field so store the value
            entityToFill.addProperty(this.name, value);
        } else {
            // if blueprint has typeOf then create new entity
            Blueprint blueprint = BlocksConfig.getInstance().getTemplateCache().getBlueprint(this.getBlueprintName(), this.language);
            String entityName = blueprint.getEntityName();
            if (entityName != null) {
                this.entity = BlocksConfig.getInstance().getDatabase().createEntity(entityName, this.language);
                entityToFill = this.entity;
            }
            // Add properties to typeof
            for (BasicTemplate property : properties.values()) {
                entityToFill = property.getEntityProperties(entityToFill);
            }
            if (entityToFill != entity) entity.addProperty(this.name, entityToFill);

        }
        return entity;
    }

    public List<Entity> getRootEntities()
    {
        ArrayList<Entity> retVal = new ArrayList<Entity>();
        // TODO WOUTER fix ""
        if (this.getBlueprintName() != null) {
            Blueprint blueprint = BlocksConfig.getInstance().getTemplateCache().getBlueprint(this.getBlueprintName(), this.language);
            if (blueprint != null && blueprint.getEntityName() != null) {
                this.entity = BlocksConfig.getInstance().getDatabase().createEntity(blueprint.getEntityName(), this.language);
                if (this instanceof StoredTemplate && ((StoredTemplate)this).getId() != null) this.entity.setId(((StoredTemplate)this).getId());
                retVal.add(this.entity);
                for (BasicTemplate template: this.properties.values()) {
                    template.getEntityProperties(this.entity);
                }
            } else {
                for (BasicTemplate template : this.properties.values()) {
                    retVal.addAll(template.getRootEntities());
                }
            }
        }
        return retVal;
    }

    public String getLanguage() {
        String retVal = this.language;
        if (retVal == null) {
            retVal = BlocksConfig.getDefaultLanguage();
        }
        return retVal;
    }

    public static Element parse(String html){
        Element retVal = new Element(Tag.valueOf("div"), BlocksConfig.getSiteDomain());
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

//    protected static Element parseTemplateToElement(String html){
//        Element retVal = new Element(Tag.valueOf("div"), BlocksConfig.getSiteDomain());
//        Document doc = new Document(BlocksConfig.getSiteDomain());
//        Document parsed = Jsoup.parse(html, BlocksConfig.getSiteDomain(), Parser.htmlParser());
//        /*
//         * If only part of a html-file is being parsed (which starts f.i. with a <div>-tag), Jsoup will add <html>-, <head>- and <body>-tags, which is not what we want
//         * Thus if the head (or body) is empty, but the body (or head) is not, we only want the info in the body (or head).
//         */
//        if(parsed.head().childNodes().isEmpty() && !parsed.body().childNodes().isEmpty()){
//            for(Node child : parsed.body().childNodes()) {
//                doc.appendChild(child.clone());
//            }
//        }
//        else if(parsed.body().childNodes().isEmpty() && !parsed.head().childNodes().isEmpty()){
//            for(Node child : parsed.head().childNodes()) {
//                doc.appendChild(child.clone());
//            }
//        }
//        else if(parsed.body().childNodes().isEmpty() && parsed.body().childNodes().isEmpty()){
//            //add nothing to the retVal so an empty document will be returned
//        }
//        else{
//            doc = parsed;
//        }
//        if (doc.children().size() > 0) {
//            retVal = doc.children().first();
//        }
//
//        return retVal;
//    }


    public boolean isTemplateContent() {
        return this.templateContent;
    }

    public void isTemplateContent(boolean value) {
        this.templateContent = value;
    }

    public boolean isSingleton() {
        return this.singleton;
    }

    public String getSingleton() {
        return this.singletonName;
    }

    public Entity getEntity() {
        return this.entity;
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

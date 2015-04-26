package com.beligum.blocks.renderer;

import com.beligum.base.templating.velocity.VelocityTemplateEngine;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.models.Resource;
import com.beligum.blocks.models.rdf.OrderedMemGraph;
import com.beligum.blocks.usermanagement.Permissions;
import com.beligum.blocks.utils.PropertyFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.velocity.runtime.directive.Parse;

import java.util.*;

/**
 * Created by wouter on 8/04/15.
 */
public class VelocityBlocksRenderer implements BlocksTemplateRenderer
{
    private final String CONTENT = "CONTENT";
    private final String CAPTION = "CAPTION";
    private final Integer START_PROPERTY_LENGTH = ParserConstants.TEMPLATE_PROPERTY_START.length();
    private final Integer END_PROPERTY_LENGTH = ParserConstants.TEMPLATE_PROPERTY_END.length();
    private final Set<String> voidElements = new HashSet<String>(Arrays.asList("area", "base", "br", "col", "command", "embed", "hr", "img", "input", "keygen", "link", "meta", "param", "source", "track", "wbr"));
    private final Set<String> srcElements = new HashSet<String>(Arrays.asList("frame", "iframe", "img", "input", "layer", "script", "textarea", "video", "source", "embed", "track"));
    private final Set<String> hrefElements = new HashSet<String>(Arrays.asList("a", "area", "base", "link"));

    // Resource -> property -> language -> value
//    private HashMap<String, HashMap<String, HashMap<String, ArrayList<RDFNode>>>> rdfCache = new HashMap<>();
    private HashMap<String, HashMap<String, Integer>> rdfPropertyUsed = new HashMap<>();

    private class Field {
        public Field(String name) {this.name = name;};
        public String name;
        public boolean property = false;
        public boolean list = false;
    }

    private class FieldOverview {
        public ArrayList<Field> fields;
        public HashMap<String, Integer> fieldsLeft;
        public FieldOverview() {
            this.fields = new ArrayList<>();
            this.fieldsLeft = new HashMap<>();
        }
    }

    private Locale locale;
    private StringBuilder buffer;
    private boolean useOnlyEntity = false;
    private boolean showResource = true;
    private boolean fetchSingletons = true;
    private boolean fetchEntities = false;
    private boolean renderDynamicBlocks = true;
    private boolean readOnly = false;
    private boolean usesEntity = false; // is set by the renderer
    private LinkedHashSet<String> links = new LinkedHashSet();
    private LinkedHashSet<String> scripts = new LinkedHashSet();


    public void setUseOnlyEntity(boolean useOnlyEntity)
    {
        this.useOnlyEntity = useOnlyEntity;
    }
    public void setShowResource(boolean showResource)
    {
        this.showResource = showResource;
    }
    public void setFetchSingletons(boolean fetchSingletons)
    {
        this.fetchSingletons = fetchSingletons;
    }
    public void setFetchEntities(boolean fetchEntities)
    {
        this.fetchEntities = fetchEntities;
    }
    public void setRenderDynamicBlocks(boolean renderDynamicBlocks)
    {
        this.renderDynamicBlocks = renderDynamicBlocks;
    }
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public VelocityBlocksRenderer() {
        this.buffer = new StringBuilder();
    }



    public String render(StoredTemplate storedTemplate, Resource resource, String language) {
        this.locale = new Locale(language);
        this.buffer = new StringBuilder();
        if (resource != null) usesEntity = true;
        if (renderDynamicBlocks && Blocks.blockHandler().isDynamicBlock(storedTemplate.getBlueprintName())) {
            this.buffer.append(Blocks.blockHandler().getDynamicBlock(storedTemplate.getBlueprintName()).render(storedTemplate));
        } else {
            this.renderElement(storedTemplate, resource, null, this.readOnly);
        }
        return this.buffer.toString();
    }


    public String render(PageTemplate pageTemplate, StoredTemplate storedTemplate, Resource resource, String language) {
        this.locale = new Locale(language);
        this.buffer = new StringBuilder();

        if (resource != null) usesEntity = true;
        // Render everything in the page except main content
        this.scripts.addAll(pageTemplate.getScripts());
        this.links.addAll(pageTemplate.getLinks());

        this.renderTemplate(pageTemplate, null, this.readOnly);
        StringBuilder page = this.buffer;

        // Render main content
        render(storedTemplate, resource, language);

        page = replace(page, ParserConstants.TEMPLATE_CONTENT, this.buffer);


        // Add all links and scripts
        StringBuilder scriptsAndLinks = new StringBuilder();
        // Append Blocks client only if logged in
        if (SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            for (String link : Blocks.templateCache().getBlocksLinks()) {
                scriptsAndLinks.append(link).append(System.lineSeparator());
            }
        }

        for (String link: this.links) {
            scriptsAndLinks.append(link).append(System.lineSeparator());
        }

        // Append Blocks client only if logged in
        if (SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            for (String script : Blocks.templateCache().getBlocksScripts()) {
                scriptsAndLinks.append(script).append(System.lineSeparator());
            }
        }

        for (String script: this.scripts) {
            scriptsAndLinks.append(script).append(System.lineSeparator());
        }

        page = replace(page, ParserConstants.TEMPLATE_HEAD, scriptsAndLinks);

        this.buffer = new StringBuilder();

        return page.toString();
    }

    public StringBuilder replace(StringBuilder origin, String property, StringBuilder value) {
        StringBuilder retVal = new StringBuilder();
        int start = origin.indexOf(property);
        if (start > -1) {
            retVal.append(origin.substring(0, start));
            retVal.append(value);
            retVal.append(origin.substring(start+property.length()));
        }
        return retVal;
    }

    private void renderInsideElement(BasicTemplate template, Resource resource,  String property, boolean readOnly)
    {

        String stringTemplate = template.getValue();
        Blueprint blueprint = template.getBlueprint();

        if (blueprint == null) {
            // This is a basic block/property
            String value = null;
            if (resource != null) {
                value = resource.getProperty(property);
            }
            if (value != null) {
                this.buffer.append(value);
            } else if (template.getProperties().size() > 0) {
                this.renderTemplate(template, resource, true);
            } else if (!useOnlyEntity) {
                this.buffer.append(template.getValue());
            }

        } else if (blueprint.isWrapper() && !template.equals(blueprint)) {
            // render blueprint with value of this basicTemplate
            renderInsideWrapper(blueprint.getTemplate(), resource, property);
        } else {
            // Add links and scripts
            this.links.addAll(blueprint.getLinks());
            this.scripts.addAll(blueprint.getScripts());

            if (resource != null) {
                Resource newResource = resource.getResource(property);
                if (newResource != null) {
                    resource = newResource;
                }
            }

            if (readOnly) {
                if (blueprint.getProperties().size() > 0) {
                    this.renderTemplate(template, resource, true);
                } else {
                    this.buffer.append(blueprint.getValue());
                }
            } else if (blueprint.getProperties().size() > 0 || template.getProperties().size() > 0) {
                this.renderTemplate(template, resource, readOnly);
            } else {
                this.buffer.append(template.getValue());
            }

        }
        if (resource != null) resource.incrementPropertyIndex(property);

    }

    public void renderInsideWrapper(String templateToRender, Resource resource, String property) {
        FieldOverview fieldOverview = findNextPropertyInTemplate(templateToRender);
        for (Field nextProperty: fieldOverview.fields) {
            // this is not a property but a piece of the template, so add and forward
            if (!nextProperty.property) {
                this.buffer.append(nextProperty.name);
                continue;
            }
            String value = null;
            if (resource != null) value = resource.getProperty(property);
            if (value != null)
                this.buffer.append(value);
        }
    }

    protected void renderTemplate(BasicTemplate template, Resource resource, boolean readOnly)
    {
        // Part 1 render dynamic block, we don't care further
        Blueprint blueprint = template.getBlueprint();
        if (renderDynamicBlocks && Blocks.blockHandler().isDynamicBlock(template.getBlueprintName())) {
            this.buffer.append(Blocks.blockHandler().getDynamicBlock(template.getBlueprintName()).render(template));
            return;
        }

        // We have to render this thing. Decide on the string template
        String templateToRender = template.getValue();
        if (blueprint != null && (blueprint.isFixed() || readOnly)) {
            templateToRender = blueprint.getTemplate();
        }

        // List all properties in this template
        FieldOverview fieldOverview = findNextPropertyInTemplate(templateToRender);
        if (fieldOverview.fields.size() == 0) {
            // Nothing found so just render element
            return;
        }

        // Init all the property finders
        PropertyFinder propertyFinder = new PropertyFinder();


        for (Field nextProperty: fieldOverview.fields) {
            // this is not a property but a piece of the template, so add and forward
            if (!nextProperty.property) {
                this.buffer.append(nextProperty.name);
                continue;
            }

            /*
            * Get the right value for this property. We fetch 3 values.
            *   1. From the stored template
            *   2. From the blueprint
            *   3. From the entity
            * */

            BasicTemplate property = getProperty(template, nextProperty.name, propertyFinder.getPropertyIndex(nextProperty.name), readOnly);


            if (property != null) {




                renderElement(property, resource, nextProperty.name, readOnly);

                propertyFinder.propertyFound(nextProperty.name);

                // Check how many values are left for this property in entity
                Integer fieldsLeft = 0;
                if (usesEntity && resource != null) {
                    fieldsLeft = resource.getPropertyValueCount(nextProperty.name) - resource.getPropertyIndex(nextProperty.name);
                }

                /*
                * This property in the template is marked as in a list and there are more values available in the entity
                * then add the other values of the entity for this property as well. We keep in count if this property is used again further
                * in this template and make sure those get filled as well
                *
                * fieldsOverview.fieldsLeft keeps count how many times the property is going to be used in this template
                * fieldsLeft keeps count how many values the entity contains
                * */
                Integer renderExtraInList = 0;
                if (property.isInList() && fieldOverview.fieldsLeft.get(nextProperty.name) < fieldsLeft) {
                    renderExtraInList  = fieldsLeft - fieldOverview.fieldsLeft.get(nextProperty.name);
                }
                while (renderExtraInList > 0) {
                    //                        property = getProperty(template, nextProperty.name, propertyFinder.getPropertyIndex(nextProperty.name), readOnly);
                    this.renderElement(property, resource, nextProperty.name, readOnly);
                    propertyFinder.propertyFound(nextProperty.name); // update the counter to get the right entity value
                    renderExtraInList--;
                }




            } else {
                // Do nothing
            }

        }
    }


    protected FieldOverview findNextPropertyInTemplate(String template) {
        FieldOverview retVal = new FieldOverview();

        int templateLength = template.length();
        int oldStart = 0;
        int start = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_START, oldStart);
        while (start > -1) {
            if (start != oldStart) {
                retVal.fields.add(new Field(template.substring(oldStart, start)));
            }
            int end = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_END, start);
            if (end > -1 && end > start) {
                start += START_PROPERTY_LENGTH;
                Field field = new Field(template.substring(start, end));
                field.property = true;
                retVal.fields.add(field);
                if (retVal.fieldsLeft.containsKey(field.name)) {
                    retVal.fieldsLeft.put(field.name, retVal.fieldsLeft.get(field.name) + 1);
                } else {
                    retVal.fieldsLeft.put(field.name, 0);
                }
                oldStart = end + END_PROPERTY_LENGTH;
            }

            start = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_START, oldStart);
        }
        if (oldStart < templateLength) {
            retVal.fields.add(new Field(template.substring(oldStart, templateLength)));
        }

        return retVal;
    }


    public void renderElement(BasicTemplate propertyTemplate, Resource resource, String property, boolean readOnly) {
        // if element is src or href???
        //
        String propAttribute = null;
        Blueprint blueprint = propertyTemplate.getBlueprint();
        String tag = readOnly && blueprint != null ? blueprint.getElement().getTag() : propertyTemplate.getElement().getTag();
        if (srcElements.contains(tag)) {
            propAttribute = ParserConstants.SRC;
        } else if (hrefElements.contains(tag)) {
            propAttribute = ParserConstants.HREF;
        }
        HashMap<String, String> extraAttributes = new HashMap<>();

        //RDFNode entityProperty = getRDFValue(node, new PropertyImpl(Blocks.rdfFactory().ensureAbsoluteRdfValue(propertyTemplate.getName())), this.locale.getLanguage());


        if (propAttribute != null) {
            String value = null;

            if (resource != null) {
                if (property != null) {
                    value = resource.getProperty(property);
                } else {
//                    value = resource.getProperty(RDFS.Resource.toString());
                }
            }

            if (value == null && !useOnlyEntity) {
                value = propertyTemplate.getElement().getAttributes().get(propAttribute);
            } else {
                // TODO: only increment when in list, otherwise repeat
                resource.incrementPropertyIndex(property);
                property = null;
            }

            if (value != null && value.startsWith(Blocks.config().getDefaultRdfPrefix()+":")) {
                String id = null;
                try {id = value.split(":")[1]; } catch (Exception e) {}
                if (id.startsWith("#") || id.startsWith("/") || id.startsWith(":")) id = id.substring(1);
                value = Blocks.urlDispatcher().getUrlForId(id);
            }

            extraAttributes.put(propAttribute, value);
            String caption = getCaption(propertyTemplate.getName());

        }

        if (propertyTemplate != null) {
            this.renderStartElement(propertyTemplate, readOnly, extraAttributes);
            this.renderInsideElement(propertyTemplate, resource, property, readOnly);
            this.renderEndElement(tag);
        }

    }

    public void renderStartElement(BasicTemplate property, boolean readOnly, HashMap<String, String> extra)
    {
        HtmlElement templateElement = property.getElement();
        String tag = readOnly && property.getBlueprint() != null ? property.getBlueprint().getElement().getTag() : property.getElement().getTag();
        this.buffer.append("<").append(tag).append(" ");
        HashSet<String> attributes = new HashSet<String>(templateElement.getAttributes().keySet());
        Blueprint blueprint = property.getBlueprint();

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
        if (extra != null) {
            attributes.addAll(extra.keySet());
        } else {
            extra = new HashMap<>();
        }

        for (String key: attributes) {
            String value = extra.get(key);
            if (value == null) value = templateElement.getAttributes().get(key);
            if (value == null && blueprint != null)  value = blueprint.getElement().getAttributes().get(key);

            // property on blueprint without typeof is not a real rdfa property
            if (key.equals(ParserConstants.PROPERTY) && blueprint != null && blueprint.getRdfType() == null) {
                this.buffer.append(addAtribute(ParserConstants.BLUEPRINT_PROPERTY, property.getName()));
            } else if (key.equals("class") && blueprint != null && !blueprint.equals(property)) {
                LinkedHashSet<String> classes = new LinkedHashSet<>();
                classes.addAll(Arrays.asList((ParserConstants.CSS_CLASS_PREFIX + blueprint.getName() + " " + value).split(" ")));
                classes.addAll(Arrays.asList(blueprint.getElement().getAttributes().get(key).split(" ")));

                this.buffer.append(addAtribute(key, StringUtils.join(classes.toArray(), " ")));
            } else if (key.equals(ParserConstants.TYPE_OF)) {
                this.buffer.append(addAtribute(ParserConstants.TYPE_OF, blueprint.getRdfType()));
            } else if (key.equals(ParserConstants.LANGUAGE)) {
                this.buffer.append(addAtribute(ParserConstants.LANGUAGE, this.locale.getLanguage()));
            }
            else
            {
                this.buffer.append(addAtribute(key, value));
            }
        }
        this.buffer.append(" >");


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

    public BasicTemplate getProperty(BasicTemplate template, String propertyName, int index, boolean readOnly) {
        BasicTemplate retVal = PropertyFinder.findProperty(propertyName, template.getProperties(), index);
        BasicTemplate blueprintProperty = null;
        if (template.getBlueprint() != null) {
            blueprintProperty = PropertyFinder.findProperty(propertyName, template.getBlueprint().getProperties(), index);
        }

        if (retVal == null && blueprintProperty != null) retVal = blueprintProperty;

        if (retVal instanceof Singleton && this.fetchSingletons){
            BlockId singletonId = ((Singleton) retVal).getId();
            StoredTemplate singleton = Blocks.database().fetch(singletonId, this.locale.getLanguage(), Blocks.factory().getSingletonClass());
            if (singleton != null) {
                retVal = singleton;
            }
        }

        boolean propertyReadOnly = readOnly;
        if (blueprintProperty != null && (propertyReadOnly || blueprintProperty.isReadOnly()) || (retVal != null && retVal.isReadOnly())) {
            retVal = blueprintProperty;
        }

        return retVal;
    }



    public void renderEndElement(String tag) {
        if (!voidElements.contains(tag)) {
            this.buffer.append("</").append(tag).append(">");
        }
    }

    public VelocityBlocksRenderer append(String string) {
        this.buffer.append(string);
        return this;
    }

    public VelocityBlocksRenderer append(StringBuilder stringbuilder) {
        this.buffer.append(stringbuilder);
        return this;
    }

    public String toString() {
        return this.buffer.toString();
    }

    public StringBuilder toStringBuilder() {
        return this.buffer;
    }

    public static String getCaption(String name) {
        return name + "/" + ParserConstants.CAPTION;
    }

//    private List<RDFNode> getRDFValues(RDFNode resource, Property property, String language) {
//        String id = null;
//        List<RDFNode> retVal = new ArrayList<>();
//        if (language == null) language = Locale.ROOT.getLanguage();
//        if (resource != null && resource.isResource()) {
//            if (resource.isAnon()) {
//                id = resource.asResource().getId().toString();
//            } else {
//                id = resource.asResource().getURI();
//            }
//        }
//        if (id != null) {
//            if (!this.rdfCache.containsKey(id)) {
//                this.rdfCache.put(id, new HashMap());
//            }
//            HashMap<String, HashMap<String, ArrayList<RDFNode>>> properties = this.rdfCache.get(id);
//            if (!properties.containsKey(property.getURI())) {
//                properties.put(property.getURI(), new HashMap<String, ArrayList<RDFNode>>());
//
//                HashMap<String, ArrayList<RDFNode>> values = properties.get(property.getURI());
//                if (!values.containsKey(Locale.ROOT.getLanguage())) {
//                    values.put(Locale.ROOT.getLanguage(), new ArrayList<RDFNode>());
//                }
//                if (!values.containsKey(language)) {
//                    values.put(language, new ArrayList<RDFNode>());
//                }
//
//                NodeIterator it = model.listObjectsOfProperty(resource.asResource(), property);
//                while (it.hasNext()) {
//                    RDFNode value = it.next();
//                    if (value.isLiteral()) {
//                        if (!values.containsKey(value.asLiteral().getLanguage())) {
//                            values.put(value.asLiteral().getLanguage(), new ArrayList<RDFNode>());
//                        }
//                        values.get(value.asLiteral().getLanguage()).add(value);
//                    } else {
//                        values.get(Locale.ROOT.getLanguage()).add(value);
//                    }
//                }
//
//            }
//            HashMap<String, ArrayList<RDFNode>> values = properties.get(property.getURI());
//            if (values.get(Locale.ROOT.getLanguage()).size() > values.get(language).size()) {
//                for (int i = values.get(language).size(); i < values.get(Locale.ROOT.getLanguage()).size(); i++) {
//                    values.get(language).add(values.get(Locale.ROOT.getLanguage()).get(i));
//                }
//            }
//            retVal = values.get(language);
//        }
//        return retVal;
//    }

//
//    private RDFNode getRDFValue(RDFNode resource, Property property, String language) {
//        List<RDFNode> values = getRDFValues(resource, property, language);
//        Integer index= getPropertyIndex(resource, property);
//        RDFNode retVal = null;
//        if (values.size() > index) {
//            retVal = values.get(index);
//        }
//        return retVal;
//    }
//
//    private void rdfPropertyUsed(RDFNode resource, Property property) {
//        String id = null;
//        if (resource.isResource()) {
//            if (resource.isAnon()) {
//                id = resource.asResource().getId().toString();
//            } else {
//                id = resource.asResource().getURI();
//            }
//        }
//
//        if (id != null) {
//            if (!this.rdfPropertyUsed.containsKey(id)) {
//                this.rdfPropertyUsed.put(id, new HashMap());
//            }
//
//            HashMap<String, Integer> properties = this.rdfPropertyUsed.get(id);
//            if (!properties.containsKey(property.getURI())) {
//                properties.put(property.getURI(), 0);
//            }
//            properties.put(property.getURI(), properties.get(property.getURI() + 1));
//        }
//
//    }
//
//    private Integer getFieldsLeft(RDFNode resource, Property property, String language) {
//        return  getRDFValues(resource, property, language).size() - getPropertyIndex(resource, property);
//
//    }
//
//    private Integer getPropertyIndex(RDFNode resource, Property property) {
//        String id = null;
//        Integer retVal = 0;
//        if (resource != null && resource.isResource()) {
//            if (resource.isAnon()) {
//                id = resource.asResource().getId().toString();
//            } else {
//                id = resource.asResource().getURI();
//            }
//        }
//
//        if (id != null) {
//            if (!this.rdfPropertyUsed.containsKey(id)) {
//                this.rdfPropertyUsed.put(id, new HashMap());
//            }
//
//            HashMap<String, Integer> properties = this.rdfPropertyUsed.get(id);
//            if (!properties.containsKey(property.getURI())) {
//                properties.put(property.getURI(), 0);
//            }
//            retVal = properties.get(property.getURI());
//
//        }
//        return retVal;
//    }
}
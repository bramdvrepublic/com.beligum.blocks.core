package com.beligum.blocks.core.parsers.redis.visitors;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.urlmapping.redis.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.redis.BlueprintsCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.redis.RedisDatabase;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.redis.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.blocks.core.parsers.redis.TemplateParser;
import com.beligum.blocks.core.dynamic.DynamicBlockListener;
import com.beligum.blocks.core.dynamic.TranslationList;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.*;

/**
 * Created by wouter on 23/11/14.
 * Visitor holding all functionalities to go from a stored entity-templates to a html-page
 */
public class ToHtmlVisitor extends SuperVisitor
{
    /**the preferred language we want to render html in*/
    private final String language;
    /**the (javascript-)scripts that need to be injected*/
    private Set<String> scripts = new HashSet<>();
    private List<String> scriptsInOrder = new ArrayList<>();
    /**the (css-)linked files that need to be injected*/
    private Set<String> links = new HashSet<>();
    private List<String> linksInOrder = new ArrayList<>();
    /**the links needed to render all dynamic blocks encountered*/
    private List<Node> dynamicBlockLinks = new ArrayList<>();
    /**the scripts needed to render all dynamic blocks encountered*/
    private List<Node> dynamicBlockScripts = new ArrayList<>();

    /**
     *
     * @param language the preferred language we want to render html in
     * @param pageTemplateLinks the (css-)linked files of the page-template this page is being rendered in
     * @param pageTemplateScripts the (javascript-)scripts of the page-template this page is being rendered in
     * @throws ParseException if no known language was specified
     */
    public ToHtmlVisitor(URL pageUrl, String language, List<String> pageTemplateLinks, List<String> pageTemplateScripts) throws ParseException {
        this(pageUrl, language);
        addLinks(pageTemplateLinks);
        addScripts(pageTemplateScripts);
    }

    /**
     *
     * @param language the preferred language we want to render html in
     * @throws ParseException if no known language was specified
     */
    public ToHtmlVisitor(URL entityUrl, String language) throws ParseException {
        this.entityUrl = entityUrl;
        this.language = Languages.getStandardizedLanguage(language);
        if(!Languages.isNonEmptyLanguageCode(this.language)){
            throw new ParseException("Found unknown language '" + this.language + "'.");
        }
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        try {
            node = super.head(node, depth);
            Node retVal = node;

            if(isEntity(node) && node instanceof Element) {


                if (!StringUtils.isEmpty(getReferencedId(node))) {
                    // We make a distinctions between properties and properties with typeof
                    if (!hasBlueprintType(node)) {
                        retVal = getPropertyInstance((Element) retVal);
                    }
                    else {
                        retVal = getTypeInstance((Element) retVal);
                    }

                    // Copy attributes from property but do not overwrite attributes from instance
                    for (Attribute attribute: node.attributes()) {
                        if (!retVal.hasAttr(attribute.getKey())) {
                            retVal.attr(attribute.getKey(), attribute.getValue());
                        }
                    }
                    node.replaceWith(retVal);
                }
                else {

                }
                //if no modifications can be done to the class-template, we fill in the correct property-references coming from the instance
//                                removeInternalAttributes(renderedTemplateNode);
            }

            return retVal;

        }
        catch(Exception e){
            throw new ParseException("Error while parsing node '" + node.nodeName() + "' at tree depth '" + depth + "' to html.", e, node);
        }
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            node = super.tail(node, depth);
            if(isEntity(node) && node instanceof Element) {
                Element element = (Element) node;

//                if (hasBlueprintType(node) && isEditable((Element)node)) node.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);

                //TODO: here all dynamic blocks should be checked
                DynamicBlockListener translationList = new TranslationList(this.language, this.entityUrl);
                if (translationList.getType().equals(this.getBlueprintType(element))) {
                    element = translationList.onShow(element);
                    for(Element link : translationList.getLinks()) {
                        boolean added = this.links.add(link.outerHtml());
                        if(added){
                            this.dynamicBlockLinks.add(link);
                        }
                    }
                    for(Element script : translationList.getScripts()) {
                        boolean added = this.scripts.add(script.outerHtml());
                        if(added){
                            this.dynamicBlockScripts.add(script);
                        }
                    }
                }
                node = element;
            }
            if(node.hasAttr("href")){
                String url = node.attr("href");
                //the url "" does not need to be replaced
                if(!StringUtils.isEmpty(url)) {
                    //make relative urls absolute
                    URL absoluteUrl = new URL(Blocks.config().getSiteDomainUrl(), url);
                    //only urls from the sites domain need to be translated
                    if (absoluteUrl.toString().startsWith(Blocks.config().getSiteDomain())) {
                        BlocksID id = XMLUrlIdMapper.getInstance().getId(absoluteUrl);
                        id = new BlocksID(id, this.language);
                        URL translatedUrl = XMLUrlIdMapper.getInstance().getUrl(id);
                        translatedUrl = new URL(Languages.translateUrl(translatedUrl.toString(), this.language)[0]);
                        node.attr("href", translatedUrl.toString());
                    }
                }
            }
            return node;
        }
        catch(Exception e){
            throw new ParseException("Error while parsing to html.", node);
        }
    }


    /**
     * Note: Use this method only after traversing the html. If not, an empty set will be returned.
     * @return an ordered list of all link-nodes needed to render this page
     */
    public List<Node> getLinks(){
        List<Node> links = new ArrayList<>();
        for(String link : this.linksInOrder){
            Node linkNode = TemplateParser.parse(link).child(0);
            links.add(linkNode);
        }
        links.addAll(this.dynamicBlockLinks);
        return links;
    }

    /**
     * Note: Use this method only after traversing the html. If not, an empty set will be returned.
     * @return an ordered list of all script-nodes needed to render this page
     */
    public List<Node> getScripts(){
        List<Node> scripts = new ArrayList<>();
        for(String script : this.scriptsInOrder){
            Node scriptNode = TemplateParser.parse(script).child(0);
            scripts.add(scriptNode);
        }
        scripts.addAll(this.dynamicBlockScripts);
        return scripts;
    }

    /**
     * Add (unique) links to this visitor, in order
     * @param links
     */
    private void addLinks(List<String> links){
        if(links != null) {
            for (String link : links) {
                boolean added = this.links.add(link);
                //if this link wasn't present yet, add it to the list
                if (added) {
                    this.linksInOrder.add(link);
                }
            }
        }
    }

    /**
     * Add (unique) scirpts to this visitor, in order
     * @param scripts
     */
    private void addScripts(List<String> scripts){
        if(scripts != null) {
            for (String script : scripts) {
                boolean added = this.scripts.add(script);
                //if this script wasn't present yet, add it to the list
                if (added) {
                    this.scriptsInOrder.add(script);
                }
            }
        }
    }


    /**
     * Given a property node with a reference but a default type,
     * insert the correct value (instance, default value)
     * @param node
     */
    private Element getPropertyInstance(Element node) throws ParseException
    {
        Element retVal = null;
        Element propertyDefault = (Element)fetchReferencedInstance(getPropertyId(node));
        if (propertyDefault.hasAttr(ParserConstants.USE_DEFAULT)) {
            // fetch default value
            retVal = propertyDefault.clone();
            retVal.removeAttr(ParserConstants.USE_DEFAULT);
        } else {
            retVal = (Element)fetchReferencedInstance(getReferencedId(node));
        }
        if (!isNotEditable(node)) {
            retVal.attr(ParserConstants.CAN_EDIT_PROPERTY, "");

        } else {
            retVal.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
        }
        copyAttribute(ParserConstants.RESOURCE, node, retVal);
        return retVal;
    }


 /*
    # Editable pages
     - a page, when loaded, is automatically set to editable
     - All blocks on the first level in the page are editable
     - EXCEPT if the blueprint of this block defines it as not editable with the attribute CAN_NOT_EDIT
     - if a block is editable, then it's properties are editable too
     - EXCEPT if the property is marked with CAN-NOT-EDIT attribute
     - If a property is editable but the blueprint is marked as not editable, then the property becomes not editable
     - When a propertie is not editable, the all content (properties) on a deeper level becomes not editable

     If we say a block is editable, it has a double meaning. If available, plugins will be loaded to make
    the properies editable in the client. (e.g. a text editor). It also means that the content from the database is used
    to show in the fields. If a property of a block is marked as not editable, then the content from the blueprint will be used
    even if there is content available in the database.

    For a property that is a blueprint it self, the blueprint will be loaded and not the content from the database
    EXCEPT if this blueprint has the can-layout property. Then the html from the database will be loaded for this
    brueprint and the properties for this blueprint will be filled from DB (if property is editable) or from the blueprint
    (if property is not editable)

    In short: a blueprint is a template. This template has fields that are editable. A field can also  use a
    template that also conatins properties. A template is always recovered from disk (the template of the designer) exceopt
    if this template has the CAN_LAYOUT property. Then the template is (within limits) changeable and saved and
    restored from the database

     @param node
     */
    private Element getTypeInstance(Element node) throws Exception
    {
        // Find the class of this node
        Element retVal = null;
        Blueprint blueprint = BlueprintsCache.getInstance().get(getBlueprintType(node));

        this.addLinks(blueprint.getLinks());
        this.addScripts(blueprint.getScripts());

        String entityTemplateClassHtml = blueprint.getTemplate(language);
        //if no template could be found for the current language, fall back to the primary language
        if (entityTemplateClassHtml == null) {
            entityTemplateClassHtml = blueprint.getTemplate();
        }
        Element entityClassElement = TemplateParser.parse(entityTemplateClassHtml).child(0);

        // Default setting. First Type found is editable
        if (this.blueprintTypeStack.size() == 1) node.attr(ParserConstants.CAN_EDIT_PROPERTY, "");

        retVal = entityClassElement.clone();
        retVal.removeAttr(ParserConstants.BLUEPRINT);

        Element reference = (Element) fetchReferencedInstance(getReferencedId(node));
        HashMap<String, Element> classProperties = getProperties(entityClassElement, false);

        boolean propertyIsEditable = node.hasAttr(ParserConstants.CAN_EDIT_PROPERTY);

        // Use property from blueprint
        if (node.hasAttr(ParserConstants.USE_DEFAULT)) {
            retVal = (Element) fetchReferencedInstance(getPropertyId(node));
        // use template from database
        } else if (isLayoutable(entityClassElement)) {
            retVal = reference;
            if (propertyIsEditable) {
                copyAttribute(ParserConstants.CAN_LAYOUT, entityClassElement, retVal);
            } else {
                retVal.removeAttr(ParserConstants.CAN_LAYOUT);
            }
        // use blueprint as template
        } else {
            HashMap<String, Element> properties = getProperties(retVal, false);
            setReferences(getProperties(reference, false), properties);
        }

        // Set properties as editable based on properties in blueprint (class)
        setPropertiesEditable(entityClassElement, classProperties, getProperties(retVal, false), propertyIsEditable);



        return retVal;
    }


    /**
     * Copy attribute from one node to another
     * @param attribute
     * @param from
     * @param to
     */
    private void copyAttribute(String attribute, Element from, Element to)
    {
        if (to!=null && from!= null && from.hasAttr(attribute)) to.attr(attribute, from.attr(attribute));
    }

    /**
     * Replace the class-property with a new copy of the default-value's of the class, referencing to the specified entity
     * @param node
     * @param failOnMissingReference throw error if property without reference is found
     * @throws Exception
     */
    private HashMap<String, Element> getProperties(Element node, boolean failOnMissingReference) throws ParseException
    {
        Elements propertyList = node.select("[" + ParserConstants.REFERENCE_TO + "]");
        HashMap<String, Element> retVal = new HashMap<String, Element>();
        for (Element property: propertyList) {
            if (property.hasAttr(ParserConstants.PROPERTY)) {
                String name = property.attr(ParserConstants.PROPERTY);
                String uniqueName = name;
                int counter = 0;
                while (retVal.containsKey(uniqueName)) {
                    counter++;
                    uniqueName = name + counter;
                }
                retVal.put(uniqueName, property);
            } else if (failOnMissingReference) {
                throw new ParseException("Found entity which is not a property of class '" + node.attr(ParserConstants.BLUEPRINT) + "' as " + property.attr(ParserConstants.BLUEPRINT)+ "\n");
            } else {
                Logger.debug("Found class property which was not replaced by an instance property of class '" + node.attr(ParserConstants.BLUEPRINT) + "' as: " + property.attr(ParserConstants.BLUEPRINT));
            }
        }
        return retVal;
    }

    private String getUniquePropertyName(Element property) {
        String name=  property.hasAttr(ParserConstants.PROPERTY_NAME) ? property.attr(ParserConstants.PROPERTY_NAME) : null;
        return name == null ? property.attr(ParserConstants.PROPERTY) : property.attr(ParserConstants.PROPERTY) + "#" + property.attr(ParserConstants.PROPERTY_NAME);
    }

    /**
     * Make the new node editable based on current property and the classTemplate
     *
     * Set the properties in a block as editable
     * This block is loaded in a property inside another blueprint. If this property inside this parent blueprint
     * is not editable then our properties also become not editable. -> canEditParent
     *
     * Our blueprint defines if properties are editable
     *
     * In this method we compare the settings in the blueprint with the setting of the property where we are loaded (the current situation)
     *
     * @param entityClass: our blueprint. This defines if the properties are editable
     * @param fromProperties
     * @param toProperties
     * @param canEditParent Is the property where we are loaded editable?
     * @throws ParseException
     */
    private void setPropertiesEditable(Element entityClass, HashMap<String,Element> fromProperties, HashMap<String,Element>  toProperties, boolean canEditParent) throws ParseException
    {
        try {

            // Set all properties as editable/not editable
            for (Element toProperty : toProperties.values()) {

                Element fromProperty = fromProperties.get(toProperty.attr(ParserConstants.PROPERTY));

                Element element = null;

                // if instance set can-edit by class and parent for each property

                // Check if we find this property in the blueprint
                if (fromProperty != null) {
                    // Our blueprint is loaded in a property that is editable
                    if (canEditParent) {
                        // blueprint is not editable but property is then property is editable
                        if (isNotEditable(entityClass) && isEditable(fromProperty)) {
                            toProperty.attr(ParserConstants.CAN_EDIT_PROPERTY, "");
                        }
                        // blueprint is editable but property is not then property is not editable
                        else if (!isNotEditable(entityClass) && isNotEditable(fromProperty)) {
                            toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                            toProperty.removeAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY);
                        }
                        // class is not editable and property not explicitly set as editable then not editable
                        else if (isNotEditable(entityClass) && !isEditable(fromProperty)) {
                            toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                            toProperty.removeAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY);
                        }
                        // make editable
                        else {
                            toProperty.attr(ParserConstants.CAN_EDIT_PROPERTY);
                        }
                    // the parent property in the blueprint is not editable so no matter what,
                    // this property is also not editable
                    } else {
                        toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                        toProperty.removeAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY);
                    }

                    if (fromProperty.hasAttr(ParserConstants.USE_DEFAULT)) {
                        toProperty.attr(ParserConstants.USE_DEFAULT, "");
                        toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                    }
                // property is not found in blueprint and parent property is editable then this is editable
                } else if (canEditParent) {
                    toProperty.attr(ParserConstants.CAN_EDIT_PROPERTY, "");
                // property is not found in blueprint and parent property is not editable then this is not editable
                } else {
                    toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                    toProperty.removeAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY);
                }

            }

        }
        catch(Exception e){
            throw new ParseException("Could not set editability(!?) for properties of class.", e, entityClass);
        }
    }

    /**
     * Copy the (editable) references from the instance-template to the class-template
     * @param fromProperties
     * @param toProperties
     * @throws ParseException
     */
    private void setReferences(HashMap<String,Element> fromProperties, HashMap<String,Element>  toProperties) throws ParseException
    {
        try {

            //if referencing, editable properties are present in the class-template, they are proper properties and they should be filled in from the entity-instance we are parsing now

            //copy all properties of the instance to the class
            for (Element fromProperty : fromProperties.values()) {
                //                    for (Element instanceProperty : instancePropertiesList) {
                //                        if (getPropertyId(instanceProperty).equals(getPropertyId(classProperty))) {
                Element toProperty = toProperties.get(getUniquePropertyName(fromProperty));
                if (toProperty != null) {
                    copyAttribute(ParserConstants.REFERENCE_TO, fromProperty, toProperty);
                    copyAttribute(ParserConstants.RESOURCE, fromProperty, toProperty);
                }
                // TODO Wouter: What with properties we could not find in source? Create new resource and could/should/might this even hapen?

            }
        }
        catch(Exception e){
            throw new ParseException("Could not set reference for property  \n", e);
        }
    }





    private Node fetchReferencedInstance(String id) throws ParseException
    {
        Node retVal = null;
        try {
            if (!StringUtils.isEmpty(id)) {
                BlocksID referencedId = new BlocksID(id, BlocksID.LAST_VERSION, language);
                EntityTemplate instanceTemplate = (EntityTemplate) RedisDatabase.getInstance().fetch(referencedId, EntityTemplate.class);
                if(instanceTemplate == null){
                    //the specified language could not be found in db, fetch last version in primary langugae
                    instanceTemplate = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(referencedId, EntityTemplate.class);
                    if(instanceTemplate == null) {
                        throw new ParseException("Found bad reference. Not found in db: " + referencedId);
                    }
                }

                if(!instanceTemplate.getDeleted()) {
                    String instanceHtml = instanceTemplate.getTemplate(language);
                    //if no template could be found for the current language, fall back to the primary language
                    if (instanceHtml == null) {
                        instanceHtml = instanceTemplate.getTemplate();
                    }
                    Element instanceTemplateRoot = TemplateParser.parse(instanceHtml).child(0);
                    if (StringUtils.isEmpty(getResource(instanceTemplateRoot)) &&
                        //when referencing to a class-default, we don't want the resource to show up in the browser
                        StringUtils.isEmpty(referencedId.getUrl().toURI().getFragment())) {

                        URL url = XMLUrlIdMapper.getInstance().getUrl(referencedId);
                        instanceTemplateRoot.attr(ParserConstants.RESOURCE, url.toString());
                    }

                    retVal = instanceTemplateRoot;
                }
                else {
                    Element instanceTemplateRoot = TemplateParser.parse(instanceTemplate.getTemplate()).child(0);
                    instanceTemplateRoot.removeAttr(ParserConstants.REFERENCE_TO);
                    retVal = instanceTemplateRoot;
                }
            }
            return retVal;

        }catch(Exception e){
            if(e instanceof ParseException){
                throw (ParseException) e;
            }
            else{
                throw new ParseException("Could not replace node by referenced instance with id: \n \n" + id + "\n\n", e);
            }
        }
    }
}

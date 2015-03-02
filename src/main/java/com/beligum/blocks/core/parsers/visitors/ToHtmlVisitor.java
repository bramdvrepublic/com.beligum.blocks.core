package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.parsers.dynamicblocks.DynamicBlock;
import com.beligum.blocks.core.parsers.dynamicblocks.TranslationList;
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
    public ToHtmlVisitor(URL pageUrl, String language) throws ParseException {
        this.pageUrl = pageUrl;
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
                //if this is a referencing block, replace it

                if (!StringUtils.isEmpty(getReferencedId(node))) {
                    if (!hasTypeOf(node)) {
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
                //                removeInternalAttributes(renderedTemplateNode);
            }

            return retVal;

        }
        catch(Exception e){
            throw new ParseException("Error while parsing node '" + node.nodeName() + "' at tree depth '" + depth + "' to html: \n \n " + node + "\n \n", e);
        }
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            node = super.tail(node, depth);
            if(isEntity(node) && node instanceof Element) {
                Element element = (Element) node;

                if (hasTypeOf(node) && isEditable((Element)node)) node.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);

                //TODO BAS: here we should use a listener to check for all dynamic blocks
                DynamicBlock translationList = new TranslationList(this.language, this.pageUrl);
                if (translationList.getTypeOf().equals(this.getTypeOf(element))) {
                    element = translationList.generateBlock(element);
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
            return node;
        }
        catch(Exception e){
            throw new ParseException("Error while parsing to html at \n \n" + node + "\n \n");
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
        for(String link : links) {
            boolean added = this.links.add(link);
            //if this link wasn't present yet, add it to the list
            if(added){
                this.linksInOrder.add(link);
            }
        }
    }

    /**
     * Add (unique) scirpts to this visitor, in order
     * @param scripts
     */
    private void addScripts(List<String> scripts){
        for(String script : scripts) {
            boolean added = this.scripts.add(script);
            //if this script wasn't present yet, add it to the list
            if(added){
                this.scriptsInOrder.add(script);
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
        if (isEditable(node)) {
            retVal.attr(ParserConstants.CAN_EDIT_PROPERTY, "");

        } else {
            retVal.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
        }
        copyAttribute(ParserConstants.RESOURCE, node, retVal);
        return retVal;
    }


    /**
     * Given a reference node for a type, insert the correct entityTemplate (instance, class or defaulktvalue)
     * @param node
     */
    private Element getTypeInstance(Element node) throws CacheException, ParseException
    {
        // find class
        Element retVal = null;
        EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));

        this.addLinks(entityTemplateClass.getLinks());
        this.addScripts(entityTemplateClass.getScripts());

        String entityTemplateClassHtml = entityTemplateClass.getTemplate(language);
        //if no template could be found for the current language, fall back to the primary language
        if (entityTemplateClassHtml == null) {
            entityTemplateClassHtml = entityTemplateClass.getTemplate();
        }
        Element entityClassElement = TemplateParser.parse(entityTemplateClassHtml).child(0);

        // Default setting. First Type found is editable
        if (this.typeOfStack.size() == 1) node.attr(ParserConstants.CAN_EDIT_PROPERTY, "");

        retVal = entityClassElement.clone();
        Element reference = (Element) fetchReferencedInstance(getReferencedId(node));
        HashMap<String, Element> classProperties = getProperties(entityClassElement, false);
//        HashMap<String, Element> referenceProperties = ;
        boolean propertyIsEditable = node.hasAttr(ParserConstants.CAN_EDIT_PROPERTY);
        if (node.hasAttr(ParserConstants.USE_DEFAULT)) {
            retVal = (Element) fetchReferencedInstance(getPropertyId(node));
            setPropertiesEditable(entityClassElement, classProperties, getProperties(retVal, false), propertyIsEditable);
        } else if (isLayoutable(entityClassElement)) {
            retVal = reference;
            if (propertyIsEditable) {
                copyAttribute(ParserConstants.CAN_LAYOUT, entityClassElement, retVal);
            } else {
                retVal.removeAttr(ParserConstants.CAN_LAYOUT);
            }
            setPropertiesEditable(entityClassElement, classProperties, getProperties(reference, false), propertyIsEditable);

        } else {
            HashMap<String, Element> properties = getProperties(retVal, false);
            setPropertiesEditable(entityClassElement, classProperties, getProperties(retVal, false), propertyIsEditable);
            setReferences(getProperties(reference, false), properties);
        }



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
        if (from.hasAttr(attribute)) to.attr(attribute, from.attr(attribute));
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

                retVal.put(getUniquePropertyName(property), property);
            } else if (failOnMissingReference) {
                throw new ParseException("Found entity which is not a property of class '" + node.attr(ParserConstants.TYPE_OF) + "' as " + property.attr(ParserConstants.TYPE_OF)+ "\n");
            } else {
                Logger.debug("Found class property which was not replaced by an instance property of class '" + node.attr(ParserConstants.TYPE_OF) + "' as: " + property.attr(ParserConstants.TYPE_OF));
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
     * @param entityClass
     * @param fromProperties
     * @param toProperties
     * @throws ParseException
     */
    private void setPropertiesEditable(Element entityClass, HashMap<String,Element> fromProperties, HashMap<String,Element>  toProperties, boolean canEdit) throws ParseException
    {
        try {

            //if referencing, editable properties are present in the class-template, they are proper properties and they should be filled in from the entity-instance we are parsing now

            //copy all properties of the instance to the class
            for (Element fromProperty : fromProperties.values()) {
                //                    for (Element instanceProperty : instancePropertiesList) {
                //                        if (getPropertyId(instanceProperty).equals(getPropertyId(classProperty))) {
                Element toProperty = toProperties.get(getUniquePropertyName(fromProperty));

                Element element = null;

                // if instance set can-edit by class and parent for each property

                // if class set reference_to and resource and check if parent allows editing
                if (toProperty != null) {
                    if (canEdit) {
                        if (isEditable(fromProperty) || isEditable(entityClass)) {
                            toProperty.attr(ParserConstants.CAN_EDIT_PROPERTY, "");
                        }
                        else if (fromProperty.hasAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY)) {
                            toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                            toProperty.removeAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY);
                        }
                        else {
                            toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                        }
                    } else {
                        toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                        toProperty.removeAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY);
                    }

                    if (fromProperty.hasAttr(ParserConstants.USE_DEFAULT)) {
                        toProperty.attr(ParserConstants.USE_DEFAULT, "");
                        toProperty.removeAttr(ParserConstants.CAN_EDIT_PROPERTY);
                    }
                }

                // TODO Wouter: What with properties we could not find in source
            }

        }
        catch(Exception e){
            throw new ParseException("Could not set editability(!?) for property  \n", e);
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
                EntityTemplate instanceTemplate = (EntityTemplate) Redis.getInstance().fetch(referencedId, EntityTemplate.class);
                if(instanceTemplate == null){
                    //the specified language could not be found in db, fetch last version in primary langugae
                    instanceTemplate = (EntityTemplate) Redis.getInstance().fetchLastVersion(referencedId, EntityTemplate.class);
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
                        instanceTemplateRoot.attr(ParserConstants.RESOURCE, referencedId.getUrl().toString());
                    }

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

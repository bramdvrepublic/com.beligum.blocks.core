package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.parsers.dynamicblocks.DynamicBlock;
import com.beligum.blocks.core.parsers.dynamicblocks.TranslationList;
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
public class ToHtmlVisitor extends AbstractVisitor
{
    /**the preferred language we want to render html in*/
    private final String language;
    /**the (javascript-)scripts that need to be injected*/
    private Set<String> scripts = new HashSet<>();
    private List<String> scriptsInOrder = new ArrayList<>();
    /**the (css-)linked files that need to be injected*/
    private Set<String> links = new HashSet<>();
    private List<String> linksInOrder = new ArrayList<>();

    /**
     *
     * @param language the preferred language we want to render html in
     * @param pageTemplateLinks the (css-)linked files of the page-template this page is being rendered in
     * @param pageTemplateScripts the (javascript-)scripts of the page-template this page is being rendered in
     * @throws ParseException if no known language was specified
     */
    public ToHtmlVisitor(URL pageUrl, String language, List<String> pageTemplateLinks, List<String> pageTemplateScripts) throws ParseException {
        this(pageUrl, language);
        for(String link : pageTemplateLinks) {
            boolean added = this.links.add(link);
            //if this link wasn't present yet, add it to the list
            if(added){
                this.linksInOrder.add(link);
            }
        }
        for(String script : pageTemplateScripts) {
            boolean added = this.scripts.add(script);
            //if this script wasn't present yet, add it to the list
            if(added){
                this.scriptsInOrder.add(script);
            }
        }
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
            if(isEntity(node) && node instanceof Element) {
                Element entityRoot = (Element) node;
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                String entityTemplateClassHtml = entityTemplateClass.getTemplate(language);
                //if no template could be found for the current language, fall back to the primary language
                if (entityTemplateClassHtml == null) {
                    entityTemplateClassHtml = entityTemplateClass.getTemplate();
                }
                Element entityClassRoot = TemplateParser.parse(entityTemplateClassHtml).child(0);

                //if no modifications can be done, first we fill in the correct property-references, coming from the class
                if (useClass(entityRoot, entityClassRoot)) {
                    node = copyPropertiesToClassTemplate(entityRoot, entityClassRoot);
                }
                //if this is a referencing block, replace it
                node = replaceWithReferencedInstance(node);
            }
            return node;
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
                //TODO BAS: here we should use a listener to check for all dynamic blocks
                DynamicBlock translationList = new TranslationList(this.language, this.pageUrl);
                if (translationList.getTypeOf().equals(this.getTypeOf(node))) {
                    node = translationList.generateBlock((Element) node);
                }
            }
            else if(node.nodeName().equals("head")){
                Element head = (Element) node;
                for(String link : this.linksInOrder){
                    head.appendChild(TemplateParser.parse(link).child(0));
                }
                for(String script : this.scriptsInOrder){
                    head.appendChild(TemplateParser.parse(script).child(0));
                }
                node = head;
            }
            return node;
        }
        catch(Exception e){
            throw new ParseException("Error while parsing to html at \n \n" + node + "\n \n");
        }
    }



    /**
     * Determines wether or not the class-template should be used, or rather the instance itself. This is done using isModifiable(entityRoot) and isModifiable(entityClassRoot)
     * @param entityRoot
     * @param entityClassRoot
     * @return
     */
    private boolean useClass(Element entityRoot, Element entityClassRoot){
        boolean entityIsModifiable = isModifiable(entityRoot);
        boolean entityClassIsModifiable = isModifiable(entityClassRoot);
        if(entityClassIsModifiable){
            return false;
        }
        else{
            if(entityIsModifiable){
                return false;
            }
            else{
                return true;
            }
        }
    }



    /**
     * Copy the (editable) properties from the instance-template to the class-template
     * @param fromInstanceRoot
     * @param toClassRoot
     * @return
     * @throws ParseException
     */
    private Node copyPropertiesToClassTemplate(Element fromInstanceRoot, Element toClassRoot) throws ParseException
    {
        try {
            Elements instanceReferencingElements = fromInstanceRoot.select("[" + ParserConstants.REFERENCE_TO + "]");
            Elements instanceProperties =  instanceReferencingElements.select("[" + ParserConstants.PROPERTY + "]");
            Elements classReferencingElements = toClassRoot.select("[" + ParserConstants.REFERENCE_TO + "]");
            Elements classProperties = classReferencingElements.select("[" + ParserConstants.PROPERTY + "]");

            //if referencing, editable properties are present in the class-template, they are proper properties and they should be filled in from the entity-instance we are parsing now
            if (!instanceProperties.isEmpty() && !classProperties.isEmpty()) {
                for (Element classProperty : classProperties) {
                    for (Element instanceProperty : instanceProperties) {
                        if (getPropertyId(instanceProperty).contentEquals(getPropertyId(classProperty))) {
                            Element element = null;
                            //If the classproperty is modifiable, we replace it with the instance's property
                            if (isModifiable(classProperty)) {
                                Element instancePropertyCopy = instanceProperty.clone();
                                classProperty.replaceWith(instancePropertyCopy);
                                element = instancePropertyCopy;
                            }
                            //If the class-defaults should be used for this class-property, we fetch the default from db and add it, using the original instance's property's resource-id.
                            else {
                                element = replaceWithNewDefaultCopy(classProperty, getReferencedId(instanceProperty));
                            }
                            copyModificationLevel(classProperty, element);
                            instanceReferencingElements.remove(instanceProperty);
                            classReferencingElements.remove(classProperty);
                        }
                    }
                }
                for(Element remainingClassReferencingElement : classReferencingElements){
                    //when a typeof-child without a property is encountered, we can only render the default value, without showing it's resource, so it is not overwritten later
                    RedisID classDefaultId = new RedisID(getReferencedId(remainingClassReferencingElement), RedisID.LAST_VERSION, language);
                    EntityTemplate classDefault = Redis.getInstance().fetchEntityTemplate(classDefaultId);
                    if(classDefault == null){
                        classDefault = (EntityTemplate) Redis.getInstance().fetchLastVersion(classDefaultId, EntityTemplate.class);
                        if(classDefault == null) {
                            throw new ParseException("Found bad reference. Not present in db: " + getReferencedId(remainingClassReferencingElement));
                        }
                    }
                    String classDefaultHtml = classDefault.getTemplate(language);
                    //if the current language cannot be found, fall back to primary language
                    if(classDefaultHtml == null){
                        classDefaultHtml = classDefault.getTemplate();
                    }
                    Node classDefaultRoot = TemplateParser.parse(classDefaultHtml).child(0);
                    remainingClassReferencingElement.replaceWith(classDefaultRoot);
                }
                Node returnRoot = toClassRoot;
                for (Attribute attribute : fromInstanceRoot.attributes()) {
                    returnRoot.attr(attribute.getKey(), attribute.getValue());
                }
                returnRoot.removeAttr(ParserConstants.BLUEPRINT);
                fromInstanceRoot.replaceWith(returnRoot);
                return returnRoot;
            }
            else {
                return fromInstanceRoot;
            }
        }
        catch(ParseException e){
            throw e;
        }
        catch(Exception e){
            throw new ParseException("Couldn't deduce an entity-instance from it's entity-class at \n \n" + fromInstanceRoot + "\n \n", e);
        }
    }

    /**
     * Replace the class-property with a new copy of the default-value's of the class, referencing to the specified entity
     * @param classProperty
     * @param referenceId entity-id this default-copy should be a new version of
     * @throws Exception
     */
    private Element replaceWithNewDefaultCopy(Node classProperty, String referenceId) throws Exception
    {
        RedisID defaultClassPropertyId = new RedisID(getReferencedId(classProperty), RedisID.LAST_VERSION, language);
        EntityTemplate defaultClassPropertyTemplate = Redis.getInstance().fetchEntityTemplate(defaultClassPropertyId);
        if(defaultClassPropertyTemplate == null){
            defaultClassPropertyTemplate = (EntityTemplate) Redis.getInstance().fetchLastVersion(defaultClassPropertyId, EntityTemplate.class);
            if(defaultClassPropertyTemplate == null) {
                throw new ParseException("Couldn't find last version of class-default property '" + defaultClassPropertyId + "' in db.");
            }
        }
        String defaultClassPropertyHtml = defaultClassPropertyTemplate.getTemplate(language);
        //if no template could be found for the current language, fall back to the primary language
        if(defaultClassPropertyHtml == null){
            defaultClassPropertyHtml = defaultClassPropertyTemplate.getTemplate();
        }
        Element defaultClassPropertyRoot = TemplateParser.parse(defaultClassPropertyHtml).child(0);
        String referencedInstanceId = referenceId;
        RedisID id = new RedisID(referencedInstanceId, RedisID.LAST_VERSION, language);
        defaultClassPropertyRoot.attr(ParserConstants.RESOURCE, id.getUrl().toString());
        classProperty.replaceWith(defaultClassPropertyRoot);
        return defaultClassPropertyRoot;
    }


    private Node replaceWithReferencedInstance(Node instanceRootNode) throws ParseException
    {
        try {
            String id = getReferencedId(instanceRootNode);
            if (!StringUtils.isEmpty(id)) {
                RedisID referencedId = new RedisID(id, RedisID.LAST_VERSION, language);
                EntityTemplate instanceTemplate = Redis.getInstance().fetchEntityTemplate(referencedId);
                if(instanceTemplate == null){
                    //the specified language could not be found in db, fetch last version in primary langugae
                    instanceTemplate = (EntityTemplate) Redis.getInstance().fetchLastVersion(referencedId, EntityTemplate.class);
                    if(instanceTemplate == null) {
                        throw new ParseException("Found bad reference. Not found in db: " + referencedId);
                    }
                }
                this.scripts.addAll(instanceTemplate.getScripts());
                this.links.addAll(instanceTemplate.getLinks());
                String instanceHtml = instanceTemplate.getTemplate(language);
                //if no template could be found for the current language, fall back to the primary language
                if(instanceHtml == null){
                    instanceHtml = instanceTemplate.getTemplate();
                }
                Element instanceTemplateRoot = TemplateParser.parse(instanceHtml).child(0);
                if(StringUtils.isEmpty(getResource(instanceTemplateRoot)) &&
                   //when referencing to a class-default, we don't want the resource to show up in the browser
                   StringUtils.isEmpty(referencedId.getUrl().toURI().getFragment())){
                    instanceTemplateRoot.attr(ParserConstants.RESOURCE, referencedId.getUrl().toString());
                }
                instanceRootNode.replaceWith(instanceTemplateRoot);
                instanceRootNode.removeAttr(ParserConstants.REFERENCE_TO);
                return instanceTemplateRoot;
            }
            else{
                return instanceRootNode;
            }
        }catch(Exception e){
            if(e instanceof ParseException){
                throw (ParseException) e;
            }
            else{
                throw new ParseException("Could not replace node by referenced entity-instance: \n \n" + instanceRootNode + "\n\n", e);
            }
        }
    }
}

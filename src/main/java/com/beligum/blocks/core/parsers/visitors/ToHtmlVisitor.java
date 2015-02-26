package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
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
            if(isEntity(node) && node instanceof Element) {
                //if this is a referencing block, replace it
                node = replaceWithReferencedInstance(node);

                //now check if the properties found in the entity should be copied to the class-template
                Element entityRoot = (Element) node;
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                this.addLinks(entityTemplateClass.getLinks());
                this.addScripts(entityTemplateClass.getScripts());
                String entityTemplateClassHtml = entityTemplateClass.getTemplate(language);
                //if no template could be found for the current language, fall back to the primary language
                if (entityTemplateClassHtml == null) {
                    entityTemplateClassHtml = entityTemplateClass.getTemplate();
                }
                Element entityClassRoot = TemplateParser.parse(entityTemplateClassHtml).child(0);

                //if no modifications can be done to the class-template, we fill in the correct property-references coming from the instance
                if (useClass(entityRoot, entityClassRoot)) {
                    node = copyPropertiesToClassTemplate(entityRoot, entityClassRoot);
                }
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
                Element element = (Element) node;
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
     * Determines wether or not the class-template should be used, or rather the instance itself. This is done using isModifiable(entityRoot) and isModifiable(entityClassRoot)
     * @param entityRoot
     * @param entityClassRoot
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
                //copy all properties of the instance to the class
                for (Element classProperty : classProperties) {
                    for (Element instanceProperty : instanceProperties) {
                        if (getPropertyId(instanceProperty).equals(getPropertyId(classProperty))) {
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
                //all remaining class-properties are rendered, since we are starting from the class anyway
                for(Element remainingClassReferencingElement : classReferencingElements){
                    if(!remainingClassReferencingElement.hasAttr(ParserConstants.PROPERTY)) {
                        throw new ParseException("Found entity which is not a property in class '" + toClassRoot.attr(ParserConstants.TYPE_OF) + "' at: \n \n " + classReferencingElements+ "\n \n");
                    }
                    Logger.debug("Found class property which was not replaced by an instance property of class '" + toClassRoot.attr(ParserConstants.TYPE_OF) + "' at: " +
                                 remainingClassReferencingElement);
                }
                //all remaining instance-properties are reported in debug-mode, but are ignore for the rest
                for(Element remainingInstanceReferencingElement : instanceReferencingElements){
                    if(!remainingInstanceReferencingElement.hasAttr(ParserConstants.PROPERTY)) {
                        throw new ParseException("Found entity which is not a property of class '" + toClassRoot.attr(ParserConstants.TYPE_OF) + "' at \n \n " + classReferencingElements+ "\n \n");
                    }
                    Logger.debug("Found instance property which was not copied to the class of type '" + toClassRoot.attr(ParserConstants.TYPE_OF) + "' at: " + remainingInstanceReferencingElement);
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
        BlocksID defaultClassPropertyId = new BlocksID(getReferencedId(classProperty), BlocksID.LAST_VERSION, language);
        EntityTemplate defaultClassPropertyTemplate = (EntityTemplate) RedisDatabase.getInstance().fetch(defaultClassPropertyId, EntityTemplate.class);
        if(defaultClassPropertyTemplate == null){
            defaultClassPropertyTemplate = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(defaultClassPropertyId, EntityTemplate.class);
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
        BlocksID id = new BlocksID(referencedInstanceId, BlocksID.LAST_VERSION, language);

        /*
         * Check the url-id mapping if this id has an url.
         * If not, use the id-url: [site-domain]/[entity-id]
         */
        URL url = XMLUrlIdMapper.getInstance().getUrl(id);
        if(url == null) {
            url = id.getUrl();
        }
        defaultClassPropertyRoot.attr(ParserConstants.RESOURCE, url.toString());
        classProperty.replaceWith(defaultClassPropertyRoot);
        return defaultClassPropertyRoot;
    }


    private Node replaceWithReferencedInstance(Node instanceRootNode) throws ParseException
    {
        try {
            String id = getReferencedId(instanceRootNode);
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
                        /*
                         * Check the url-id mapping if this id has an url.
                         * If not, use the id-url: [site-domain]/[entity-id]
                         */
                        URL url = XMLUrlIdMapper.getInstance().getUrl(referencedId);
                        if(url == null) {
                            url = referencedId.getUrl();
                        }
                        instanceTemplateRoot.attr(ParserConstants.RESOURCE, url.toString());
                    }
                    instanceRootNode.replaceWith(instanceTemplateRoot);
                    instanceRootNode.removeAttr(ParserConstants.REFERENCE_TO);
                    return instanceTemplateRoot;
                }
                else{
                    instanceRootNode.removeAttr(ParserConstants.REFERENCE_TO);
                    return instanceRootNode;
                }
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

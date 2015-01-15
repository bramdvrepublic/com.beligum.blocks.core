package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.directive.Parse;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
* Created by wouter on 23/11/14.
 * Visitor holding all functionalities to go from a stored entity-templates to a html-page
*/
public class ToHtmlVisitor extends AbstractVisitor
{
    /**the preferred language we want to render html in*/
    private final String language;

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
                if(entityTemplateClassHtml == null){
                    entityTemplateClassHtml = entityTemplateClass.getTemplate();
                }
                Element entityClassRoot = TemplateParser.parse(entityTemplateClassHtml).child(0);

                //if no modifacations can be done, first we fill in the correct property-references, coming from the class
                if(useClass(entityRoot, entityClassRoot)){
                    node = copyPropertiesToClassTemplate(entityRoot, entityClassRoot);
                }
                //if this is a referencing block, replace it
                node = replaceWithReferencedInstance(node);
            }
            //translate all links into the current language
            else if (node instanceof  Element&& ((Element)node).tagName().equals("a")) {
                node = this.translateUrl(node);
            }
            return node;
        }
        catch(Exception e){
            throw new ParseException("Error while parsing node '" + node.nodeName() + "' at tree depth '" + depth + "' to html: \n \n " + node + "\n \n", e);
        }
    }

    /**
     * Translate the url found in the href-attribute into the current language
     * @param node
     * @return
     */
    private Node translateUrl(Node node) throws ParseException, URISyntaxException, MalformedURLException, LanguageException {
        String url = node.attr("href");
        String lang = this.language;
        //if we're dealing with a translation link, we simple want the link of this a-node to be the link of this page, translated into the specified language
        if (node.hasAttr(ParserConstants.TRANSLATE)) {
            lang = node.attr(ParserConstants.LANGUAGE);
            if(!Languages.isNonEmptyLanguageCode(lang)){
                throw new ParseException("A " + ParserConstants.TRANSLATE + "-node needs a " + ParserConstants.LANGUAGE + "-attribute specifying the language into which the entity should be translated. Not present at: \n \n " + node + "\n\n");
            }
            url = this.pageUrl.toString();
        }
        url = Languages.translateUrl(url, lang);
        node.attr("href", url);
        return node;
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

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        return super.tail(node, depth);
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

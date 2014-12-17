package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.Set;

/**
* Created by wouter on 23/11/14.
 * Visitor holding all functionalities to go from a stored entity-templates to a html-page
*/
public class ToHtmlVisitor extends AbstractVisitor
{


    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        try {
            node = super.head(node, depth);
            if(isEntity(node) && node instanceof Element) {
                Element entityRoot = (Element) node;
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                Element entityClassRoot = Jsoup.parse(entityTemplateClass.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser()).child(0);

                if(!(isModifiable(entityRoot) && isModifiable(entityClassRoot))){
                    node = copyProperties(entityRoot, entityClassRoot);
                }
                //if this is a referencing block, replace it
                node = replaceWithReferencedInstance(entityRoot);
            }
            return node;
        }
        catch(ParseException e){
            throw e;
        }
        catch(Exception e){
            throw new ParseException("Error while parsing node '" + node.nodeName() + "' at tree depth '" + depth + "' to html.", e);
        }
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        return super.tail(node, depth);
    }

    private Node copyProperties(Element fromInstanceRoot, Element toClassRoot) throws ParseException
    {
        try {
            Elements instanceProperties = fromInstanceRoot.select("[" + ParserConstants.REFERENCE_TO + "]" + "[" + ParserConstants.PROPERTY + "]");
            Elements classProperties = toClassRoot.select("[" + ParserConstants.REFERENCE_TO + "]" + "[" + ParserConstants.PROPERTY + "]");

            //if referencing, editable properties are present in the class-template, they are proper properties and they should be filled in from the entity-instance we are parsing now
            if (!instanceProperties.isEmpty() && !classProperties.isEmpty()) {
                for (Element classProperty : classProperties) {
                    for (Element instanceProperty : instanceProperties) {
                        if (getProperty(instanceProperty).contentEquals(getProperty(classProperty))) {
                            if (isModifiable(classProperty)) {
                                Element instancePropertyCopy = instanceProperty.clone();
                                classProperty.replaceWith(instancePropertyCopy);
                            }
//                            else {
//                            TODO BAS SH: how can we make copies of default values to be linked to a new instance, (This is relevant when a class-template has changed and now a certain property has been set to unmodifiable).
//                                /*
//                                 * If the class-defaults should be used for this entity-property, we check if the last stored version of the entity-property-instance is different from the class-default.
//                                 * If so, we save en new version of the entity-property-instance to db and then we set that reference to this new version to be parsed in a later parsing-stadium.
//                                 */
//                                RedisID classPropertyId = new RedisID(getReferencedId(classProperty), RedisID.LAST_VERSION);
//                                EntityTemplate classPropertyTemplate = Redis.getInstance().fetchEntityTemplate(classPropertyId);
//                                if(classPropertyTemplate == null){
//                                    throw new ParseException("Couldn't find last version of class-default property '" + classPropertyId + "' in db.");
//                                }
//
//
//                                RedisID referencedEntityPropertyId = new RedisID(getReferencedId(instanceProperty), RedisID.LAST_VERSION);
//                                EntityTemplate referencedEntityPropertyTemplate = Redis.getInstance().fetchEntityTemplate(referencedEntityPropertyId);
//                                //remove the resource-attribute from the referenced  entity-property, so we can compare it to the propertyDefault
//                                Element referencedEntityPropertyTemplateRoot = Jsoup.parse(referencedEntityPropertyTemplate.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser()).child(0);
//                                String entityPropertyResource = referencedEntityPropertyTemplateRoot.attr(ParserConstants.RESOURCE);
//                                referencedEntityPropertyTemplateRoot.removeAttr(ParserConstants.RESOURCE);
//                                referencedEntityPropertyTemplate = new EntityTemplate(referencedEntityPropertyId, referencedEntityPropertyTemplate.getEntityTemplateClass(), referencedEntityPropertyTemplateRoot.outerHtml());
//
//
//                                RedisID lastStoredEntityPropertyId = new RedisID(getReferencedId(instanceProperty), RedisID.LAST_VERSION);
//                                EntityTemplate lastStoredEntityProperty = Redis.getInstance().fetchEntityTemplate(lastStoredEntityPropertyId);
//                                if(lastStoredEntityProperty == null){
//                                    throw new ParseException("Couldn't find last version of template '" + lastStoredEntityPropertyId + "' in db.");
//                                }
//
//
//                                if(!referencedEntityPropertyTemplate.equals(classPropertyTemplate)) {
//                                    EntityTemplateClass entityPropertyClass = EntityTemplateClassCache.getInstance().get(getTypeOf(instanceProperty));
//                                    RedisID newVersionOfEntityPropertyId = new RedisID(referencedEntityPropertyId.getUnversionedId(), RedisID.NEW_VERSION);
//                                    EntityTemplate classPropertyTemplateCopy = new EntityTemplate(newVersionOfEntityPropertyId, entityPropertyClass, classPropertyTemplate.getTemplate());
//                                    if(!lastStoredEntityProperty.equals(referencedEntityPropertyTemplate)) {
//                                        Redis.getInstance().save(classPropertyTemplateCopy);
//                                    }
//                                }
//                                classProperty.attr(ParserConstants.RESOURCE, entityPropertyResource);
//                                classProperty.attr(ParserConstants.REFERENCE_TO, referencedEntityPropertyId.getUnversionedId());
//                            }
                        }
                    }
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
                //TODO BAS SH: what should happen when a block has no properties, but is a reference to a default-block? Is het een oplossing om 2x de "head"-methode op te roepen na elkaar, vooraleer verder te gaan?
                return fromInstanceRoot;
            }
        }catch(Exception e){
            throw new ParseException("Couldn't deduce an entity-instance from it's entity-class at:" + fromInstanceRoot);
        }
    }

    /**
     * If the specified node is a referencing node, replace it with the root-node of the template corresponding to that referencing node.
     * If it is not a referencing node, return the specified node.
     * @param instanceRootNode
     * @return
     * @throws ParseException
     */
    private Element replaceWithReferencedInstance(Element instanceRootNode) throws ParseException
    {
        try {
            String referencedId = getReferencedId(instanceRootNode);
            if (!StringUtils.isEmpty(referencedId)) {
                RedisID id = new RedisID(referencedId, RedisID.LAST_VERSION);
                EntityTemplate referencedEntityTemplate = Redis.getInstance().fetchEntityTemplate(id);
                return replaceReferenceWithEntity(instanceRootNode, referencedEntityTemplate);
            }
            else{
                return instanceRootNode;
            }
        }catch(Exception e){
            if(e instanceof ParseException){
                throw (ParseException) e;
            }
            else{
                throw new ParseException("Could not replace node by referenced entity-instance: \n \n" + instanceRootNode + "\n\n");
            }
        }
    }
}

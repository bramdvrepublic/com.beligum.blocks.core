package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

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
                Element entityClassRoot = TemplateParser.parse(entityTemplateClass.getTemplate()).child(0);

                //if no modifacations can be done, first we fill in the correct property-references, coming from the class
                if(!(isModifiable(entityRoot) && isModifiable(entityClassRoot))){
                    node = copyProperties(entityRoot, entityClassRoot);
                }
                //if this is a referencing block, replace it
                node = replaceWithReferencedInstance(node);
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
                    EntityTemplate classDefault = Redis.getInstance().fetchEntityTemplate(new RedisID(getReferencedId(remainingClassReferencingElement), RedisID.LAST_VERSION));
                    if(classDefault == null){
                        throw new ParseException("Found bad reference. Not present in db: " + getReferencedId(remainingClassReferencingElement));
                    }
                    Node classDefaultRoot = TemplateParser.parse(classDefault.getTemplate()).child(0);
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
        RedisID defaultClassPropertyId = new RedisID(getReferencedId(classProperty), RedisID.LAST_VERSION);
        EntityTemplate defaultClassPropertyTemplate = Redis.getInstance().fetchEntityTemplate(defaultClassPropertyId);
        if(defaultClassPropertyTemplate == null){
            throw new ParseException("Couldn't find last version of class-default property '" + defaultClassPropertyId + "' in db.");
        }
        Element defaultClassPropertyRoot = TemplateParser.parse(defaultClassPropertyTemplate.getTemplate()).child(0);
        String referencedInstanceId = referenceId;
        RedisID id = new RedisID(referencedInstanceId, RedisID.LAST_VERSION);
        defaultClassPropertyRoot.attr(ParserConstants.RESOURCE, id.getUrl().toString());
        classProperty.replaceWith(defaultClassPropertyRoot);
        return defaultClassPropertyRoot;
    }


    /**
     * If the specified node is a referencing node, replace it with the root-node of the template corresponding to that referencing node.
     * If it is not a referencing node, return the specified node.
     * @param instanceRootNode
     * @return
     * @throws ParseException
     */
    private Node replaceWithReferencedInstance(Node instanceRootNode) throws ParseException
    {
        try {
            String id = getReferencedId(instanceRootNode);
            if (!StringUtils.isEmpty(id)) {
                RedisID referencedId = new RedisID(id, RedisID.LAST_VERSION);
                EntityTemplate instanceTemplate = Redis.getInstance().fetchEntityTemplate(referencedId);
                if(instanceTemplate == null){
                    throw new ParseException("Found bad reference. Not found in db: " + referencedId);
                }
                Element instanceTemplateRoot = TemplateParser.parse(instanceTemplate.getTemplate()).child(0);
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

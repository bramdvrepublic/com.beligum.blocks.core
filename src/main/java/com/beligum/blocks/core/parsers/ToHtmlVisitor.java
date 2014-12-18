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
            Elements instanceProperties = fromInstanceRoot.select("[" + ParserConstants.REFERENCE_TO + "]" + "[" + ParserConstants.PROPERTY + "]");
            Elements classProperties = toClassRoot.select("[" + ParserConstants.REFERENCE_TO + "]" + "[" + ParserConstants.PROPERTY + "]");

            //if referencing, editable properties are present in the class-template, they are proper properties and they should be filled in from the entity-instance we are parsing now
            if (!instanceProperties.isEmpty() && !classProperties.isEmpty()) {
                for (Element classProperty : classProperties) {
                    for (Element instanceProperty : instanceProperties) {
                        if (getProperty(instanceProperty).contentEquals(getProperty(classProperty))) {
                            //If the classproperty is modifiable, we replace it with the instance's property
                            if (isModifiable(classProperty)) {
                                Element instancePropertyCopy = instanceProperty.clone();
                                classProperty.replaceWith(instancePropertyCopy);
                            }
                            //If the class-defaults should be used for this class-property, we fetch the default from db and add it, using the original instance's property's resource-id.
                            else {
                                RedisID defaultClassPropertyId = new RedisID(getReferencedId(classProperty), RedisID.LAST_VERSION);
                                EntityTemplate defaultClassPropertyTemplate = Redis.getInstance().fetchEntityTemplate(defaultClassPropertyId);
                                if(defaultClassPropertyTemplate == null){
                                    throw new ParseException("Couldn't find last version of class-default property '" + defaultClassPropertyId + "' in db.");
                                }
                                Element defaultClassPropertyRoot = TemplateParser.parse(defaultClassPropertyTemplate.getTemplate()).child(0);
                                String referencedInstanceId = getReferencedId(instanceProperty);
                                defaultClassPropertyRoot.attr(ParserConstants.RESOURCE, referencedInstanceId);
                                classProperty.replaceWith(defaultClassPropertyRoot);
                            }
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
    private Node replaceWithReferencedInstance(Node instanceRootNode) throws ParseException
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

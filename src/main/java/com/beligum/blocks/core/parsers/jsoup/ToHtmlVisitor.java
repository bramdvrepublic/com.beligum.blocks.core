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
    private enum ModifiacationLevel
    {
        NONE(0),
        FILLABLE(1),
        LAYOUTABLE(1),
        EDITABLE(2);

        private int permissionLevel;
        ModifiacationLevel(int permissionLevel){
            this.permissionLevel = permissionLevel;
        }
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        try {
            node = super.head(node, depth);
            if(isEntity(node) && node instanceof Element) {
                Element element = (Element) node;
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                Element entityClassRoot = Jsoup.parse(entityTemplateClass.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser()).child(0);

                ModifiacationLevel modificationLevel = getModificationLevel(element, entityClassRoot);
                switch(modificationLevel){
                    case EDITABLE:
                        node = replaceWithInstance(element);
                        break;
                    case LAYOUTABLE:
                        node = replaceWithWithLayoutedClass(element, entityClassRoot);
                    case FILLABLE:
                        node = replaceWithFilledInClass(element, entityClassRoot);
                        break;
                    case NONE:
                        node = replaceWithClassCopy(element, entityClassRoot);
                }
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

    /**
     * Inspects the css-classes of the entity-instance and entity-class and decides what level of modification is permitted
     * @param enityInstanceRoot
     * @param entityClassRoot
     * @return
     */
    private ModifiacationLevel getModificationLevel(Element enityInstanceRoot, Element entityClassRoot){
        Set<String> instanceCssClasses = enityInstanceRoot.classNames();
        Set<String> classCssClasses = entityClassRoot.classNames();
        if(classCssClasses.contains(ParserConstants.CAN_EDIT)){
            if(instanceCssClasses.contains(ParserConstants.CAN_EDIT)){
                return ModifiacationLevel.EDITABLE;
            }
            else if(instanceCssClasses.contains(ParserConstants.CAN_LAYOUT)){
                return ModifiacationLevel.LAYOUTABLE;
            }
            else{
                return ModifiacationLevel.NONE;
            }
        }
        else if(classCssClasses.contains(ParserConstants.CAN_LAYOUT)){
            if(instanceCssClasses.contains(ParserConstants.CAN_LAYOUT)){
                return ModifiacationLevel.LAYOUTABLE;
            }
            else{
                return ModifiacationLevel.NONE;
            }
        }
        else{
            return ModifiacationLevel.NONE;
        }
    }

    private Node replaceWithClassCopy(Node node, Element entityClassDOMRoot){
        //TODO BAS!: een kopie van de klasse moet aangemaakt worden en geschreven naar db voor de instance met url de resource te vinden op de gespecifieerde node
        node.replaceWith(entityClassDOMRoot);
        return entityClassDOMRoot;
    }

    private Node replaceWithWithLayoutedClass(Element entityInstanceRoot, Element entityClassRoot){
        //TODO BAS SH: de blokken in een klasse moeten kunnen verplaatst worden. Dat wil zeggen dat we de klasse-properties invullen in de instance. Hier moet zeker opgepast worden dat het hele "#propertyName"-systeem blijft werken!
        return entityInstanceRoot;
    }

    private Node replaceWithFilledInClass(Element element, Element entityClassDOMRoot){
        Elements referencingChildren = element.select("[" + ParserConstants.REFERENCE_TO + "]");
        Elements classReferencingChildren = entityClassDOMRoot.select("[" + ParserConstants.REFERENCE_TO + "]");

        Elements entityProperties = referencingChildren.select("[" + ParserConstants.PROPERTY + "]");
        Elements classProperties = classReferencingChildren.select("[" + ParserConstants.PROPERTY + /*"][class*=" + ParserConstants.CAN_EDIT +*/ "]");


        //if referencing, editable properties are present in the class-template, they are proper properties and they should be filled in from the entity-instance we are parsing now
        if (!entityProperties.isEmpty() && !classProperties.isEmpty()) {
            for (Element editableClassProperty : classProperties) {
                for (Element entityProperty : entityProperties) {
                    if (getProperty(entityProperty).contentEquals(getProperty(editableClassProperty))) {
                        Element entityPropertyCopy = entityProperty.clone();
                        editableClassProperty.replaceWith(entityPropertyCopy);
                    }
                }
            }
            Node classRoot = entityClassDOMRoot;
            for (Attribute attribute : element.attributes()) {
                classRoot.attr(attribute.getKey(), attribute.getValue());
            }
            element.replaceWith(classRoot);
            return classRoot;
        }
        else{
            return element;
        }
    }

    private Node replaceWithInstance(Node instanceRootNode) throws ParseException
    {
        try {
            String referencedId = getReferencedId(instanceRootNode);
            if (!StringUtils.isEmpty(referencedId)) {
                RedisID id = new RedisID(referencedId, RedisID.LAST_VERSION);
                EntityTemplate referencedEntityTemplate = Redis.getInstance().fetchEntityTemplate(id);
                return replaceReferenceWithEntity(instanceRootNode, referencedEntityTemplate);
            }
            else {
                throw new ParseException("Found not-referencing entity, this shouldn't happen: \n \n" + instanceRootNode + "\n \n");
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

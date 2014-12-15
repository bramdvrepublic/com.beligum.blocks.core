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
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
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
                //TODO BAS: here choices need to be made about can-layout, can-edit
                Element element = (Element) node;
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                Document entityClassDOM = Jsoup.parse(entityTemplateClass.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser());

                Elements referencingChildren = element.select("[" + ParserConstants.REFERENCE_TO + "]");
                Elements classReferencingChildren = entityClassDOM.select("[" + ParserConstants.REFERENCE_TO + "]");

                Elements entityProperties = referencingChildren.select("[" + ParserConstants.PROPERTY + "]");
                Elements classProperties = classReferencingChildren.select("[" + ParserConstants.PROPERTY + "]");


                //if referencnig properties are present in the class-template, they are properly properties and they should be filled in from the entity-instance we are parsing now
                if(!entityProperties.isEmpty() && !classProperties.isEmpty()) {
                    for (Element entityProperty : entityProperties) {
                        for (Element classProperty : classProperties) {
                            if (getProperty(entityProperty).contentEquals(getProperty(classProperty))) {
                                Element entityPropertyCopy = entityProperty.clone();
                                classProperty.replaceWith(entityPropertyCopy);
                            }
                        }
                    }
                    Node classRoot = entityClassDOM.child(0);
                    for(Attribute attribute : element.attributes()){
                        classRoot.attr(attribute.getKey(), attribute.getValue());
                    }
                    node.replaceWith(classRoot);
                    node = classRoot;
                }
                //if no referencing entities are present in the class-template or , we should just replace this tag with an entity-instance if it is a referencing-tag itself
                else {
                    String referencedId = getReferencedId(node);
                    if (!StringUtils.isEmpty(referencedId)) {
                        RedisID id = new RedisID(referencedId, RedisID.LAST_VERSION);
                        EntityTemplate referencedEntityTemplate = Redis.getInstance().fetchEntityTemplate(id);
                        node = replaceReferenceWithEntity(node, referencedEntityTemplate);
                    }
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
}

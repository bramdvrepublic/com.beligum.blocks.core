//package com.beligum.blocks.parsers.visitors.template;
//
//import com.beligum.blocks.base.Blocks;
//import com.beligum.blocks.config.ParserConstants;
//import com.beligum.blocks.exceptions.ParseException;
//import com.beligum.blocks.models.StoredTemplate;
//import com.beligum.blocks.parsers.ElementParser;
//import com.beligum.blocks.parsers.visitors.BasicVisitor;
//import com.beligum.blocks.utils.UrlTools;
//import org.jsoup.nodes.Element;
//import org.jsoup.nodes.Node;
//
//import java.net.URI;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Locale;
//import java.util.UUID;
//
///**
//* Created by wouter on 20/03/15.
//*/
//public class HtmlFromClientVisitor extends BasicVisitor
//{
//
//    private StoredTemplate content = null;
//    private ArrayList<StoredTemplate> other = new ArrayList<>();
//    private URI htmlUrl = null;
//
//    public HtmlFromClientVisitor(URI url)
//    {
//        this.htmlUrl = url;
//    }
//
//    @Override
//    public Node head(Node node, int depth) throws ParseException
//    {
//
//        Locale language = UrlTools.getLanguage(htmlUrl);
//        Node retVal = node;
//        if (node instanceof Element) {
//            if (ElementParser.getReferenceUrl((Element) node) != null || (ElementParser.isUseBlueprint((Element) node) && ElementParser.isSingleton((Element) node))) {
//                // This is or a singleton blueprint or an entity
//                this.other.add(Blocks.factory().createSingleton((Element) node, language));
//                this.addProperty((Element) node);
//            }
//            else if (ElementParser.isProperty((Element) node) || ElementParser.isUseBlueprint((Element) node)) {
//                // this is probably the content of the template
//                if (content != null) {
//                    throw new ParseException("Template can only contain 1 content property");
//                }
//                node.attr(ParserConstants.RESOURCE, this.htmlUrl.toString());
//                this.content = Blocks.factory().createStoredTemplate((Element) node, language);
//                this.addProperty((Element) node);
//            }
//            else if (ElementParser.isTypeOf((Element) node)) {
//                // save this entity but this should not happen
//                StoredTemplate storedTemplate = Blocks.factory().createStoredTemplate((Element) node, language);
//                if (this.content == null) {
//                    this.content = storedTemplate;
//                }
//                else {
//                    this.other.add(storedTemplate);
//                }
//                this.addProperty((Element) node);
//            }
//
//            if (ElementParser.isTypeOf((Element) node) && !ElementParser.isResource((Element) node)) {
//                UUID id = UUID.randomUUID();
//                node.attr(ParserConstants.RESOURCE, "/" + id.toString());
//            }
//
//        }
//        return retVal;
//    }
//
//    public Node tail(Node node, int depth) throws ParseException
//    {
//        return node;
//    }
//
//    public void addProperty(Element element)
//    {
//        if (!ElementParser.isProperty(element)) {
//            element.attr(ParserConstants.PROPERTY, "");
//        }
//    }
//
//    public StoredTemplate getContent()
//    {
//        return this.content;
//    }
//
//    public ArrayList<StoredTemplate> getOther()
//    {
//        return this.other;
//    }
//
//}

//package com.beligum.blocks.parsers.visitors.template;
//
//import com.beligum.blocks.exceptions.ParseException;
//import com.beligum.blocks.models.Blueprint;
//import com.beligum.blocks.parsers.ScriptsLinksParser;
//import org.jsoup.nodes.Element;
//import org.jsoup.nodes.Node;
//
///**
//* Created by wouter on 16/03/15.
//*/
//public class BlueprintVisitor extends PropertyVisitor
//{
//    private ScriptsLinksParser scriptsLinksParser;
//
//    public BlueprintVisitor()
//    {
//        this.scriptsLinksParser = new ScriptsLinksParser();
//    }
//
//
//    public BlueprintVisitor(Blueprint blueprint) {
//        super(blueprint);
//    }
//
//    public Blueprint getBlueprint() {
//        return (Blueprint)getTemplate();
//    }
//
//    @Override
//    public Node head(Node node, int depth) throws ParseException
//    {
//        try {
//            node = super.head(node, depth);
//            if (node instanceof Element) {
//
//                // if we find a property with use blueprint, add to properties with blueprint
//
//                // if just a property add with typeof
//
//                // add links and scripts to the stack and remove them from the html (to be re-injected later)
//                node = this.scriptsLinksParser.parse(node);
//            }
//            return node;
//        }
//        catch (Exception e) {
//            throw new ParseException("Could not parse tag-head while looking for blueprints and page-templates at \n\n" + node + "\n\n", e);
//        }
//    }
//
//    public ScriptsLinksParser getScriptsLinksParser()
//    {
//        return this.scriptsLinksParser;
//    }
//}

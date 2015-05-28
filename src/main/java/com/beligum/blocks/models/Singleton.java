//package com.beligum.blocks.models;
//
//import com.beligum.blocks.exceptions.ParseException;
//import com.beligum.blocks.parsers.ElementParser;
//import org.apache.commons.lang3.StringUtils;
//import org.jsoup.nodes.Element;
//
//import java.util.Locale;
//
///**
//* Created by wouter on 26/03/15.
//*/
//public class Singleton extends StoredTemplate
//{
//    private final String defaultName = "DEFAULT";
//    private String singletonName;
//
//    public Singleton()
//    {
//    }
//
//    public Singleton(Element node, Locale language) throws ParseException
//    {
//        super(node, language);
//        if (ElementParser.isSingleton(node)) {
//
//            String name = ElementParser.getSingletonName(node);
//            String[] parts = name.split("/");
//            if (parts.length > 0) {
//                name = parts[parts.length - 1];
//            }
//            if (StringUtils.isEmpty(name)) {
//                name = defaultName;
//            }
//            if (!StringUtils.isEmpty(this.getBlueprintName())) {
//                this.singletonName = this.getBlueprintName() + "/" + name;
//            }
//            else {
//                this.singletonName = name;
//            }
//
//        }
//        else {
//            throw new ParseException("Node is not a valid Singleton.");
//        }
//    }
//
//    public String getSingletonName()
//    {
//        return singletonName;
//    }
//    public void setSingletonName(String singletonName)
//    {
//        this.singletonName = singletonName;
//    }
//}

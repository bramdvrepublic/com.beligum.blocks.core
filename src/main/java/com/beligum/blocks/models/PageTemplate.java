//package com.beligum.blocks.models;
//
//import com.beligum.blocks.exceptions.ParseException;
//import com.beligum.blocks.parsers.ElementParser;
//import com.beligum.blocks.parsers.visitors.template.PagetemplateVisitor;
//import org.jsoup.nodes.Element;
//
//import java.util.Locale;
//
///**
//* Created by wouter on 16/03/15.
//*/
//public class PageTemplate extends Blueprint
//{
//
//    public PageTemplate()
//    {
//
//    }
//
//    public PageTemplate(Element element, Locale language) throws ParseException
//    {
//        super(element, language);
//        element.children().remove();
//        while (element.parent() != null) {
//            if (ElementParser.isPageTemplateRoot(element)) {
//                this.setName(ElementParser.getPagetemplateName(element));
//            }
//            element = element.parent();
//        }
//        this.transientElement = element;
//    }
//
//    @Override
//    protected PagetemplateVisitor getVisitor() {
//        return new PagetemplateVisitor(this, this.getName());
//    }
//
//    @Override
//    public PagetemplateVisitor parse() throws ParseException
//    {
//        PagetemplateVisitor pagetemplateVisitor = (PagetemplateVisitor) super.parse();
//        return pagetemplateVisitor;
//    }
//
//}

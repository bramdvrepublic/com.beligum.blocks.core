//package com.beligum.blocks.models.factory;
//
//import com.beligum.blocks.base.Blocks;
//import com.beligum.blocks.config.ParserConstants;
//import com.beligum.blocks.exceptions.ParseException;
//import com.beligum.blocks.identifiers.BlockId;
//import com.beligum.blocks.models.Blueprint;
//import com.beligum.blocks.models.PageTemplate;
//import com.beligum.blocks.models.Singleton;
//import com.beligum.blocks.models.StoredTemplate;
//import com.beligum.blocks.models.jsonld.*;
//import com.beligum.blocks.renderer.BlocksTemplateRenderer;
//import com.beligum.blocks.renderer.VelocityBlocksRenderer;
//import com.tinkerpop.blueprints.Graph;
//import com.tinkerpop.blueprints.Vertex;
//import com.tinkerpop.blueprints.impls.orient.OrientGraph;
//import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
//import com.tinkerpop.blueprints.impls.orient.OrientVertex;
//import org.jsoup.nodes.Element;
//
//import java.net.URI;
//import java.util.HashMap;
//import java.util.Locale;
//
///**
//* Created by wouter on 2/05/15.
//*/
//public class DefaultBlocksFactory implements  BlocksFactory
//{
//
//
//
//    @Override
//    public StoredTemplate createStoredTemplate(Element element, Locale language) throws ParseException
//    {
//        return new StoredTemplate(element, language);
//    }
//
//    @Override
//    public StoredTemplate createStoredTemplate(Blueprint blueprint, Locale language) throws ParseException
//    {
//        return new StoredTemplate(blueprint, language);
//    }
//
//    @Override
//    public Blueprint createBlueprint(Element element, Locale language) throws ParseException
//    {
//        return new Blueprint(element, language);
//    }
//    @Override
//    public PageTemplate createPageTemplate(Element element, Locale language) throws ParseException
//    {
//        return new PageTemplate(element, language);
//    }
//
//    @Override
//    public Singleton createSingleton(Element element, Locale language) throws ParseException
//    {
//        return new Singleton(element, language);
//    }
//
//
//    @Override
//    public BlocksTemplateRenderer createTemplateRenderer()
//    {
//        return new VelocityBlocksRenderer();
//    }
//}

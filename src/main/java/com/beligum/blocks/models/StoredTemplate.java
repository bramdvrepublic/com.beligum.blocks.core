//package com.beligum.blocks.models;
//
//import com.beligum.blocks.base.Blocks;
//import com.beligum.blocks.config.ParserConstants;
//import com.beligum.blocks.exceptions.ParseException;
//import com.beligum.blocks.models.jsonld.OrientResourceFactory;
//import com.beligum.blocks.models.jsonld.interfaces.Node;
//import com.beligum.blocks.renderer.BlocksTemplateRenderer;
//import com.beligum.blocks.utils.UrlTools;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import org.jsoup.nodes.Element;
//import org.jsoup.parser.Tag;
//
//import java.util.Locale;
//
///**
//* Created by wouter on 16/03/15.
//*/
//public class StoredTemplate extends BasicTemplate
//{
//
//    public static final String pageTemplateName = ParserConstants.BLOCKS_SCHEMA + "pageTemplateName";
//    public static final String pageTitle= ParserConstants.BLOCKS_SCHEMA + "pageTitle";
////
////    protected String pageTemplateName;
////    protected String pageTitle;
//    private Locale language;
//
//    public StoredTemplate()
//    {
//        super();
//        this.language = Blocks.config().getDefaultLanguage();
//    }
//
//    public StoredTemplate(Blueprint blueprint, Locale language) {
//        for (String key: blueprint.getFields()) {
//            Node property = blueprint.get(key);
//            this.set(key, property.copy());
//        }
//        this.language = language;
//        this.setBlockId(UrlTools.createLocalResourceId("Page"));
//        this.set(ParserConstants.JSONLD_TYPE, OrientResourceFactory.instance().asNode(ParserConstants.BLOCKS_PAGE_TYPE, null));
//    }
//
//    public StoredTemplate(Element node, Locale language) throws ParseException
//    {
//        super(node);
//        this.language = language;
//        this.setPageTemplateName(findPageTemplateName());
//    }
//
//
//    protected String findPageTemplateName() {
//        String retVal = ParserConstants.DEFAULT_PAGE_TEMPLATE;
//        Blueprint blueprint = this.getBlueprint();
//        if (this.getBlueprintName() != null && blueprint != null) {
//            retVal = this.getBlueprint().getPageTemplateName();
//        }
//        return retVal;
//    }
//
//    public String getPageTemplateName() {
//        String retVal = getString(StoredTemplate.pageTemplateName);
//        if (retVal == null) {
//            retVal = findPageTemplateName();
//        }
//        return retVal;
//    }
//
//    public void setPageTemplateName(String pageTemplateName) {
//        set(StoredTemplate.pageTemplateName, OrientResourceFactory.instance().asNode(pageTemplateName, this.language));
//    }
//
//    public String getPageTitle()
//    {
//        String retVal = Blocks.config().getDefaultPageTitle();
//        if (this.pageTitle != null) {
//            retVal = this.pageTitle;
//        }
//        return retVal;
//    }
//
//    public void setPageTitle(String pageTitle) {
//        set(StoredTemplate.pageTitle, OrientResourceFactory.instance().asNode(pageTitle, this.language));
//    }
//
//    @JsonIgnore
//    public Element getRenderedTemplateAsElement()
//    {
//        Element retVal = null;
//        if (this.renderedTransientElement == null) {
//            BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();
//            renderer.setFetchEntities(false);
//            renderer.setRenderDynamicBlocks(false);
//            String template = renderer.render(this, null, this.language);
//            this.renderedTransientElement = parse(template);
//        }
//        retVal = this.renderedTransientElement.clone();
//        if (retVal == null)
//            retVal = new Element(Tag.valueOf("div"), null);
//        return retVal;
//    }
//
//    public Locale getLanguage()
//    {
//        return language;
//    }
//}

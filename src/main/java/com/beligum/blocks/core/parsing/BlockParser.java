//package com.beligum.blocks.core.parsing;
//
//import com.beligum.blocks.core.config.BlocksConfig;
//import com.beligum.blocks.core.config.CSSClasses;
//import com.beligum.blocks.core.config.CacheConstants;
//import com.beligum.blocks.core.exceptions.ParserException;
//import com.beligum.blocks.core.models.classes.BlockClass;
//import com.beligum.blocks.core.models.classes.EntityClass;
//import com.beligum.core.framework.utils.toolkit.FileFunctions;
//
//import java.net.MalformedURLException;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.nio.file.Path;;
//import java.nio.file.Paths;
//
///**
//* Created by bas on 03.11.14.
//*/
//public class BlockParser extends AbstractViewableParser<BlockClass>
//{
//    /**
//     *
//     * @param blockClassName the name of the viewable-class this parser will be parsing
//     */
//    public BlockParser(String blockClassName)
//    {
//        super(blockClassName);
//    }
//    /**
//     * returns the base-url for the block-class
//     *
//     * @param blockClassName the name of the block-class (f.i. "default" for a pageClass filtered from the file "entities/default/index.html")
//     * @return
//     */
//    @Override
//    public URL getBaseUrl(String blockClassName) throws MalformedURLException
//    {
//        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.BLOCK_CLASS_ID_PREFIX + "/" + blockClassName);
//    }
//    /**
//     * Returns the block-class present in the internal parser-data. The parser needs to be filled, before you can use this method.
//     *
//     * @return get the block-class with which this parser is filled up
//     * @throws ParserException if no block-class could be constructed out of the internal parser-data
//     */
//    @Override
//    protected BlockClass getInternalViewableClass() throws ParserException
//    {
//
//        if(!this.isFilled()){
//            throw new ParserException("Cannot construct an entity-class out of the internal parser-data. Did you fill up the parser before calling this method?");
//        }
//        try {
//            return new BlockClass(this.viewableClassName, this.viewableTemplate);
//        }
//        catch(URISyntaxException e){
//            throw new ParserException("Cannot construct an entity-class out of the internal parser-data.", e);
//        }
//    }
//
//    /**
//     * returns the path to the html-template of a block-class
//     * @param blockClassName name of the block-class
//     */
//    protected Path getAbsoluteTemplatePath(String blockClassName){
//        String relativeBlockClassPath = this.getRelativeTemplatePath(blockClassName);
//        //we don't need the 'file://'-part of the returned URI, so we use 'getSchemeSpecificPart()'
//        URI blockClassUri = FileFunctions.searchClasspath(this.getClass(), relativeBlockClassPath);
//        return Paths.get(blockClassUri.getSchemeSpecificPart());
//    }
//
//    /**
//     * returns the relative path to the html-template of a viewable-class
//     *
//     * @param blockClassName name of the viewable-class
//     */
//    @Override
//    protected String getRelativeTemplatePath(String blockClassName)
//    {
//        return BlocksConfig.getTemplateFolder() + "/" + BlocksConfig.BLOCKS_FOLDER + "/" + blockClassName + "/" + BlocksConfig.INDEX_FILE_NAME;
//    }
//    /**
//     * @return the prefix used for a block-class in the class-attribute of the html-template (i.e. "block-")
//     */
//    @Override
//    public String getViewableCssClassPrefix()
//    {
//        return CSSClasses.BLOCK_CLASS_PREFIX;
//    }
//    /**
//     * @return the css class-name the parser must find in a certain <div class='classname'>-tag to know the html represents a viewable class
//     */
//    @Override
//    public String getViewableCssClass()
//    {
//        return CSSClasses.BLOCK;
//    }
//}

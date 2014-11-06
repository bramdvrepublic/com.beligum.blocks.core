package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.models.classes.BlockClass;
import com.beligum.core.framework.utils.toolkit.FileFunctions;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;;
import java.nio.file.Paths;

/**
* Created by bas on 03.11.14.
*/
public class BlockParser extends AbstractViewableClassParser<BlockClass>
{
    //TODO BAS: implement BlockParser
    /**
     * returns the base-url for the block-class
     *
     * @param blockClassName the name of the block-class (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return
     */
    @Override
    public URL getBaseUrl(String blockClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.BLOCK_CLASS_ID_PREFIX + "/" + blockClassName);
    }
    /**
     * Returns the cachable-class present in the internal parser-data. The parser needs to be filled, before you can use this method.
     *
     * @return get the cachable-class with which this parser is filled up
     * @throws com.beligum.blocks.core.exceptions.ParserException if no cachable-class could be constructed out of the internal parser-data
     */
    @Override
    protected BlockClass getInternalViewableClass() throws ParserException
    {
        return null;
    }

    /**
     * returns the path to the html-template of a block-class
     * @param blockClassName name of the block-class
     */
    protected Path getAbsoluteTemplatePath(String blockClassName){
        String relativeBlockClassPath = this.getRelativeTemplatePath(blockClassName);
        //we don't need the 'file://'-part of the returned URI, so we use 'getSchemeSpecificPart()'
        URI blockClassUri = FileFunctions.searchClasspath(this.getClass(), relativeBlockClassPath);
        return Paths.get(blockClassUri.getSchemeSpecificPart());
    }

    /**
     * returns the relative path to the html-template of a viewable-class
     *
     * @param blockClassName name of the viewable-class
     */
    @Override
    protected String getRelativeTemplatePath(String blockClassName)
    {
        return BlocksConfig.getTemplateFolder() + "/" + BlocksConfig.BLOCKS_FOLDER + "/" + blockClassName + "/" + BlocksConfig.INDEX_FILE_NAME;
    }
    /**
     * @return the prefix used for a block-class in the class-attribute of the html-template (i.e. "block-")
     */
    @Override
    public String getCssClassPrefix()
    {
        return CSSClasses.BLOCK_CLASS_PREFIX;
    }

//    /**
//     * Parse a document and fill in all parser-content-fields Parses a html-document, containing a viewable-class, to blocks and rows containing variables and fills this parser up with the found template.
//     * After the parse, a string containing the template of this viewable will be saved in the field 'viewableTemplate' TODO BAS: and the found blocks and rows will be stored in the field 'elements'
//     *
//     * @param DOM     the document to be parsed
//     * @param baseUrl the base-url which will be used to define the ids
//     * @return the documents template, with template-variable-names were actual rows used to be
//     */
//    @Override
//    protected Document parse(Document DOM, URL baseUrl) throws ParserException
//    {
//        return ;
//    }
}

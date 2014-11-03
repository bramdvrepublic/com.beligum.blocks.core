package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.models.BlockClass;

/**
* Created by bas on 03.11.14.
*/
public class BlockParser implements CachableClassParser<BlockClass>
{
    /**
     * Parse the default template-file of the block-class and return a CachableClass-object, filled with it's blocks, rows and the template of the block-class
     *
     * @param blockClassName the name of the block-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return a block-class parsed from file system
     * @throws com.beligum.blocks.core.exceptions.ParserException
     */
    @Override
    public BlockClass parseCachableClass(String blockClassName) throws ParserException
    {
        return null;
    }

    /**
     * returns the path to the html-template of a block-class
     * @param blockClassName name of the block-class
     */
    protected String getTemplatePath(String blockClassName){
        return BlocksConfig.getTemplateFolder() + "/" + BlocksConfig.BLOCKS_FOLDER + "/" + blockClassName + "/" + BlocksConfig.INDEX_FILE_NAME;
    }

    /**
     * @return the prefix used for a block-class in the class-attribute of the html-template (i.e. "block-")
     */
    @Override
    public String getCssClassPrefix()
    {
        return CacheConstants.BLOCK_CLASS_ID_PREFIX;
    }
}

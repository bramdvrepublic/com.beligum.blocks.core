package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.ifaces.CachableClass;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bas on 03.11.14.
 * A representation of a html block-template.
 */
public class BlockClass extends AbstractElement implements CachableClass
{
    /**string the name of this block-class*/
    private String name;

    /**
     * Constructor
     *
     * @param name    the (unique) name of this block-class
     * @param content the template-content of this block
     * @param isFinal boolean whether or not the content of this block-class can be changed by the client
     */
    public BlockClass(String name, String content, boolean isFinal) throws URISyntaxException
    {
        super(makeID(name), content, isFinal);
        this.name = name;
    }

    /**
     * @return the name of this BlockClass
     */
    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the prefix used for a block-class in the class-attribute of the html-template
     */
    @Override
    public String getCssClassPrefix()
    {
        return CacheConstants.BLOCK_CLASS_ID_PREFIX;
    }
    /**
     * Return an ID for the blockclass with a (unique) name, the id for all blockclasses will be "blocks/<blockClassName>"
     * @param blockClassName the unique name of the blockClass
     * @return an ID for the blockclass
     */
    private static ID makeID(String blockClassName) throws URISyntaxException
    {
        return new ID(new URI(CacheConstants.BLOCK_CLASS_ID_PREFIX + "/" + blockClassName));
    }
}

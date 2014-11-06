package com.beligum.blocks.core.models.classes;

import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.storables.Row;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public class BlockClass extends AbstractViewableClass
{
    /**string the name of this page-class*/
    private String name;

    /**
     *
     * @param name the name of this block-class
     * @param directChildren the direct children of this block-class
     * @param template the template-string corresponding to the most outer layer of the element-tree in this block
     */
    public BlockClass(String name, Set<Row> directChildren, String template) throws URISyntaxException
    {
        super(makeId(name), directChildren, template);
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
     * @return the prefix used for a block-class in the class-attribute of the html-template
     */
    @Override
    public String getCssClassPrefix()
    {
        return CSSClasses.BLOCK_CLASS_PREFIX;
    }

    /**
     * Return an ID for the blockclass with a (unique) name, the id for all blockclasses will be "blocks/<blockClassName>"
     * @param blockClassName the unique name of the blockClass
     * @return an ID for the blockclass
     */
    private static ID makeId(String blockClassName) throws URISyntaxException
    {
        return new ID(new URI(CacheConstants.BLOCK_CLASS_ID_PREFIX + "/" + blockClassName));
    }
}

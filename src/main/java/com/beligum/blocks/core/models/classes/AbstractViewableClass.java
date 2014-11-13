package com.beligum.blocks.core.models.classes;

import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.AbstractViewable;
import com.beligum.blocks.core.models.storables.Row;

import java.net.URISyntaxException;
import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public abstract class AbstractViewableClass extends AbstractViewable
{

    //TODO BAS: once you're parsing and caching again, this class should fill up with stuff equal for block-class and page-entity-class

    /**
     *
     * @param id id for this viewable-class
     * @param allChildren all children (and grand-children) of this viewable-class
     * @param template the template-string containing this viewables direct children as template-variables
     */
    protected AbstractViewableClass(ID id, Set<Row> allChildren, String template) throws URISyntaxException
    {
        //a page-class cannot be altered by the client, so it always is final
        super(id, template, allChildren);
    }

    /**
     *
     * @return the name of this viewable-class
     */
    abstract public String getName();

    /**
     * @return the prefix used for a viewable-class in the class-attribute of the html-template (f.i. return "block-" for a block-class
     */
    abstract public String getCssClassPrefix();


}

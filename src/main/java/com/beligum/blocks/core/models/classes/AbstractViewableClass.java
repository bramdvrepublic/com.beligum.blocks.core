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

    /** the template this viewable-class should be rendered in*/
    private String pageTemplate;
    /** the scripts this viewable-class needs*/
    private Set<String> scripts;
    /** the (css-)layout-files this viewable-class needs*/
    private Set<String> styles;

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
        //TODO BAS: here pageTemplate, scripts and styles should be added, could be extracted from the 'template' or defaults could be added
    }

    public String getPageTemplate()
    {
        return pageTemplate;
    }
    public Set<String> getScripts()
    {
        return scripts;
    }
    public Set<String> getStyles()
    {
        return styles;
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

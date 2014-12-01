package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.IdentifiableObject;
import com.beligum.blocks.core.models.templates.EntityTemplate;

import java.util.*;

/**
 * Created by bas on 05.11.14.
 */
public abstract class AbstractTemplate extends IdentifiableObject
{
    /**string representing the html-template of this element, once the template has been set, it cannot be changed*/
    protected String template;

    /**
     * Constructor taking a unique id.
     * @param id id for this template
     * @param template the template-string which represents the content of this viewable
     */
    public AbstractTemplate(ID id, String template)
    {
        super(id);
        this.template = template;
    }

    /**
     *
     * @return the template of this viewable
     */
    public String getTemplate()
    {
        return template;
    }

}

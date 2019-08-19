package com.beligum.blocks.endpoints.utils;

import com.beligum.base.database.models.AbstractJsonObject;
import com.beligum.base.server.R;
import com.beligum.blocks.templating.TagTemplate;

public class BlockInfo extends AbstractJsonObject
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private TagTemplate tagTemplate;

    //-----CONSTRUCTORS-----
    public BlockInfo(TagTemplate tagTemplate)
    {
        this.tagTemplate = tagTemplate;
    }

    //-----PUBLIC METHODS-----
    public String getName()
    {
        return R.resourceManager().getTemplateEngine().serializePropertyKey(tagTemplate.getTemplateName());
    }
    public String getTitle()
    {
        return R.resourceManager().getTemplateEngine().serializePropertyKey(tagTemplate.getTitle());
    }
    public String getDescription()
    {
        return R.resourceManager().getTemplateEngine().serializePropertyKey(tagTemplate.getDescription());
    }
    public String getIcon()
    {
        return R.resourceManager().getTemplateEngine().serializePropertyKey(tagTemplate.getIcon());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

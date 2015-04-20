package com.beligum.blocks.renderer;

import com.beligum.blocks.models.Entity;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.models.Resource;
import com.beligum.blocks.models.StoredTemplate;

/**
 * Created by wouter on 8/04/15.
 */
public interface BlocksTemplateRenderer
{

    public void setUseOnlyEntity(boolean useOnlyEntity);
    public void setShowResource(boolean showResource);
    public void setFetchSingletons(boolean fetchSingletons);
    public void setFetchEntities(boolean fetchEntities);
    public void setRenderDynamicBlocks(boolean renderDynamicBlocks);
    public void setReadOnly(boolean readOnly);

    public String render(StoredTemplate storedTemplate, Resource resource, String language);
    public String render(PageTemplate pageTemplate, StoredTemplate storedTemplate, Resource resource, String language);
}

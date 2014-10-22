package com.beligum.blocks.core.models.ifaces;

import java.util.Map;

/**
 * Created by bas on 14.10.14.
 */
public interface StorableElement extends Storable
{
    /**
     *
     * @return the velocity-content of this element
     */
    public String getContent();
    public void setVelocityContent(String content);

    /**
     *
     * @return the name of the velocity-variable containing the content of this element
     */
    public String getTemplateVariableName();
}

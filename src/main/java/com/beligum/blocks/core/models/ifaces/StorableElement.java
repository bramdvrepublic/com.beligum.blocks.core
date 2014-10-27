package com.beligum.blocks.core.models.ifaces;

import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.RedisID;

import java.util.Map;

/**
 * Created by bas on 14.10.14.
 */
public interface StorableElement extends Storable
{
    /**
     * Override of the getId-method of Storable interface. Here a ElementID is returned instead of a RedisID, which is more specific.
     * @return the id of this storable element
     */
    @Override
    public ElementID getId();

    /**
     *
     * @return the velocity-content of this element
     */
    public String getContent();

    /**
     *
     * @return the name of the f.i. velocity-variable containing the content of this element
     */
    public String getTemplateVariableName();

    /**
     * @return boolean whether or not this elements content can be changed by the client
     */
    public boolean isFinal();
}

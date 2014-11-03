package com.beligum.blocks.core.models.ifaces;

import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.storables.Block;

import java.util.Map;

/**
 * Created by bas on 14.10.14.
 */
public interface StorableElement extends Storable
{
    //TODO BAS: should this really be an interface? or should AbstractElement be the one to be an interface, since it seems to hold less code. We'll see, once we're running with block-classes as well

    /**
     * Override of the getId-method of Storable interface. Here a ElementID is returned instead of a RedisID, which is more specific.
     * @return the id of this storable element
     */
    @Override
    public ElementID getId();

    /**
     *
     * @return the content of this element
     */
    public String getContent();

    /**
     *
     * @return the name of the variable of this element in the template holding this element
     */
    public String getTemplateVariableName();

    /**
     *
     * @return the unique id of this element in the html-tree (html-file) it belongs to
     */
    public String getHtmlId();

    /**
     * @return boolean whether or not this elements content can be changed by the client
     */
    public boolean isFinal();

    /**
     *
     * @return the name of the page-class of the page this block belongs to
     */
    public String getPageClassName();

    /**
     * @return the name of the set of all these elements in the database
     */
    public String getDBSetName();
}

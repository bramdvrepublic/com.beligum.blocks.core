package com.beligum.blocks.core.models;

import com.beligum.blocks.core.identifiers.ID;

/**
 * Created by bas on 01.10.14.
 * An abstract node in a row- and block tree
 */
public abstract class AbstractElement extends IdentifiableObject
{
    /**string representing the (html- or template-)content of this element, once the content has been set, it cannot be changed due to it's use in an elements hashCode and equals-method*/
    protected final String content;
    /**boolean whether or not this elements content can be changed by the client*/
    protected boolean isFinal;

    /**
     * Constructor
      * @param content the content of this element
     * @param id the id of this element
     * @param isFinal boolean whether or not the content of this element can be changed by the client
     */
    public AbstractElement(ID id, String content, boolean isFinal)
    {
        super(id);
        this.content = content;
        this.isFinal = isFinal;
    }

    public String getContent()
    {
        return content;
    }

    /**
     * @return boolean whether or not this elements content can be changed by the client
     */
    public boolean isFinal()
    {
        return isFinal;
    }


    /**
     * @return the name of the set of all these elements in the database
     */
    abstract public String getDBSetName();

}

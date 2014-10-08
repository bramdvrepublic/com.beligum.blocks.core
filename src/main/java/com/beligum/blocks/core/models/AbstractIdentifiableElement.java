package com.beligum.blocks.core.models; /**
 * Created by bas on 01.10.14.
 * An abstract node in a row- and block tree
 */
public abstract class AbstractIdentifiableElement extends IdentifiableObject
{
    //string representing the (html- or velocity-)content of this element
    protected String content;

    public AbstractIdentifiableElement(String content, String uid){
        super(uid);
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }
    public void setContent(String content)
    {
        this.content = content;
    }

    /**
     * @return the name of the set of all these elements in the database
     */
    abstract public String getDBSetName();

}

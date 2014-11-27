package com.beligum.blocks.core.validation;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Created by bas on 12.11.14.
 * Class send by client, representing an Entity. Validation can be done in this object
 */
public class ValidationEntity
{
    /** the html-content of this entity*/
    @NotBlank
    private String html;
    /** the name of th class of this entity */
    @NotBlank
    private String entityClassName;

//    public ValidationEntity(String html, String entityClassName)
//    {
//        this.html = html;
//        this.entityClassName = entityClassName;
//    }

    public String getHtml()
    {
        return html;
    }
    public void setHtml(String html)
    {
        this.html = html;
    }
    public String getEntityClassName()
    {
        return entityClassName;
    }
    public void setEntityClassName(String entityClassName)
    {
        this.entityClassName = entityClassName;
    }
}

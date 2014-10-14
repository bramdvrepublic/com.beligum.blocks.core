package com.beligum.blocks.core.models.ifaces;

/**
 * Created by bas on 14.10.14.
 */
public interface StorableElement extends Storable
{
    public String getContent();
    public void setContent(String content);
}

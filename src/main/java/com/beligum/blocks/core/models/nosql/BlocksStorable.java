package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.identifiers.BlockId;

/**
 * Created by wouter on 24/03/15.
 */
public interface BlocksStorable
{
    public BlockId getId();
    public void setId(BlockId id);

    public META getMeta();
    public void setMeta(META meta);
}

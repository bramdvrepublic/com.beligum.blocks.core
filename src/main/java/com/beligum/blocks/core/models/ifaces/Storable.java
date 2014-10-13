package com.beligum.blocks.core.models.ifaces;

import com.beligum.blocks.core.identifiers.RedisID;

/**
 * Created by bas on 13.10.14.
 */
public interface Storable extends Identifiable
{
    @Override
    public RedisID getId();

    public long getVersion();
}

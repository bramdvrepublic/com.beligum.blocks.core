package com.beligum.blocks.filesystem.index.ifaces;

import java.util.Iterator;

/**
 * Created by bram on 19/04/17.
 */
public interface RdfResult<E extends RdfResult.RdfResultEntry> extends Iterator<E>, AutoCloseable
{
    interface RdfResultEntry
    {
    }

    interface Triple<S, P, O> extends RdfResultEntry
    {
        S getSubject();
        P getPredicate();
        O getObject();
    }

    interface Tuple<K, V> extends RdfResultEntry
    {
        K getKey();
        V getValue();
    }
}

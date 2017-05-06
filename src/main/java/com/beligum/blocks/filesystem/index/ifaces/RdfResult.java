package com.beligum.blocks.filesystem.index.ifaces;

import java.util.Iterator;

/**
 * An interface to wrap a data model, needed to render out a list (dropdown) of possible values
 * as the result of a SPARQL query. Note that the returned entries may be tuples or triples.
 * In case of tuples, they might be used as value/label pairs, eg. to build a dropdown-filter.
 *
 * This class is made to avoid having to parse the results twice
 * (once in the back-end, once while rendering the front-end).
 *
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
        K getLabel();
        V getValue();
    }
}

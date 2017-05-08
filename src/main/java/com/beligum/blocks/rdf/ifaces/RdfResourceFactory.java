package com.beligum.blocks.rdf.ifaces;

/**
 * Make sure static Rdf class factories (eg. classes that hold static instances of RdfClass's) implement this interface,
 * so we can instantiate them when building up the vocabulary cache.
 *
 * @see com.beligum.blocks.config.RdfFactory
 *
 * Created by bram on 2/26/16.
 */
public interface RdfResourceFactory
{
}

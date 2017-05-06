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
    /**
     * Implemente these specific types of factories instead of the general one, because it's used for ordering the classes upon startup:
     * First classes (because they can be used as datatype in the terms)
     * Then terms (because both of them are needed in the mappings)
     * Then mappings
     * Then general
     *
     * See RdfFactory.assertInitialized()
     */

    interface RdfClassFactory extends RdfResourceFactory
    {
    }

    interface RdfTermFactory extends RdfResourceFactory
    {
    }

    interface RdfMappingFactory extends RdfResourceFactory
    {
    }
}

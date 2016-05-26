package com.beligum.blocks.rdf.ontology.factories;

import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.google.common.collect.Sets;

/**
 * Created by bram on 3/23/16.
 */
public class Mappings implements RdfResourceFactory
{
    //-----ENTRIES-----
    static {
        Classes.Person.setProperties(Sets.newHashSet(Terms.givenName,
                                                     Terms.familyName,
                                                     Terms.name));


    }
}
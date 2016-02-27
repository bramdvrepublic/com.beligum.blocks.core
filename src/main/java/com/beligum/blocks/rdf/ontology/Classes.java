package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfClassCollection;
import gen.com.beligum.blocks.core.messages.blocks.ontology;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class Classes implements RdfClassCollection
{
    public static final RdfClass Person = new RdfClassImpl("Person",
                                                         Settings.instance().getRdfOntologyUri(),
                                                         Settings.instance().getRdfOntologyPrefix(),
                                                         ontology.Entries.classTitle_Person,
                                                         ontology.Entries.classTitle_Person,
                                                         new URI[] { URI.create("http://dbpedia.org/page/Person"),
                                                                     URI.create("http://schema.org/Person")
                                                         });

    public static final RdfClass Page = new RdfClassImpl("Page",
                                                         Settings.instance().getRdfOntologyUri(),
                                                         Settings.instance().getRdfOntologyPrefix(),
                                                         ontology.Entries.classTitle_Page,
                                                         ontology.Entries.classTitle_Page,
                                                         new URI[] { URI.create("http://dbpedia.org/page/Web_page"),
                                                                     URI.create("http://schema.org/WebPage")
                                                         });

    //### MAKE SURE YOU ADJUST THE LIST BELOW IF YOU ADD CONSTANTS ###

    private static final RdfClass[] CLASSES = {
                    Person,
                    Page
    };

    @Override
    public RdfClass[] getClasses()
    {
        return CLASSES;
    }
}

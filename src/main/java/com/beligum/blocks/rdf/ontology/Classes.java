package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ontology.vocabularies.SettingsVocabulary;
import gen.com.beligum.blocks.core.messages.blocks.ontology;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class Classes implements RdfResourceFactory
{
    public static final RdfClass Person = new RdfClassImpl("Person",
                                                           SettingsVocabulary.INSTANCE,
                                                           ontology.Entries.classTitle_Person,
                                                           ontology.Entries.classTitle_Person,
                                                           new URI[] { URI.create("http://dbpedia.org/page/Person"),
                                                                       URI.create("http://schema.org/Person")
                                                           },
                                                           true);

    public static final RdfClass Page = new RdfClassImpl("Page",
                                                         SettingsVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_Page,
                                                         ontology.Entries.classTitle_Page,
                                                         new URI[] { URI.create("http://dbpedia.org/page/Web_page"),
                                                                     URI.create("http://schema.org/WebPage")
                                                         },
                                                         true);

    public static final RdfClass Country = new RdfClassImpl("Country",
                                                            SettingsVocabulary.INSTANCE,
                                                            ontology.Entries.classTitle_Country,
                                                            ontology.Entries.classTitle_Country,
                                                            new URI[] { URI.create("http://dbpedia.org/page/Country"),
                                                                        URI.create("http://schema.org/Country")
                                                            },
                                                            true);

    public static final RdfClass City = new RdfClassImpl("City",
                                                         SettingsVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_City,
                                                         ontology.Entries.classTitle_City,
                                                         new URI[] { URI.create("http://dbpedia.org/page/City"),
                                                                     URI.create("http://schema.org/City")
                                                         },
                                                         true);

    public static final RdfClass Borough = new RdfClassImpl("Borough",
                                                            SettingsVocabulary.INSTANCE,
                                                            ontology.Entries.classTitle_Borough,
                                                            ontology.Entries.classTitle_Borough,
                                                            new URI[] { URI.create("http://dbpedia.org/page/Borough"),
                                                                        },
                                                            true);
}

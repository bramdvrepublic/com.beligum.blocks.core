package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ontology.vocabularies.SettingsVocabulary;
import com.beligum.blocks.rdf.ontology.vocabularies.endpoints.GeonameQueryEndpoint;
import com.beligum.blocks.rdf.ontology.vocabularies.endpoints.SettingsQueryEndpoint;
import com.beligum.blocks.rdf.ontology.vocabularies.geo.AbstractGeoname;
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
                                                           true,
                                                           new SettingsQueryEndpoint());

    public static final RdfClass Page = new RdfClassImpl("Page",
                                                         SettingsVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_Page,
                                                         ontology.Entries.classTitle_Page,
                                                         new URI[] { URI.create("http://dbpedia.org/page/Web_page"),
                                                                     URI.create("http://schema.org/WebPage")
                                                         },
                                                         true,
                                                         new SettingsQueryEndpoint());

    public static final RdfClass Country = new RdfClassImpl("Country",
                                                            SettingsVocabulary.INSTANCE,
                                                            ontology.Entries.classTitle_Country,
                                                            ontology.Entries.classTitle_Country,
                                                            new URI[] { URI.create("http://dbpedia.org/page/Country"),
                                                                        URI.create("http://schema.org/Country")
                                                            },
                                                            //note: because we use a fixed-value ontology (geonames), we don't make this public (so users can select it as a type for their page)
                                                            false,
                                                            new GeonameQueryEndpoint(AbstractGeoname.Type.COUNTRY));

    public static final RdfClass City = new RdfClassImpl("City",
                                                         SettingsVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_City,
                                                         ontology.Entries.classTitle_City,
                                                         new URI[] { URI.create("http://dbpedia.org/page/City"),
                                                                     URI.create("http://schema.org/City")
                                                         },
                                                         //note: because we use a fixed-value ontology (geonames), we don't make this public (so users can select it as a type for their page)
                                                         false,
                                                         new GeonameQueryEndpoint(AbstractGeoname.Type.CITY));
}

package com.beligum.blocks.rdf.ontology.factories;

import com.beligum.blocks.config.InputTypeConfig;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ontology.RdfClassImpl;
import com.beligum.blocks.rdf.ontology.vocabularies.DBR;
import com.beligum.blocks.rdf.ontology.vocabularies.FOAF;
import com.beligum.blocks.rdf.ontology.vocabularies.SCHEMA;
import com.beligum.blocks.rdf.ontology.vocabularies.SettingsVocabulary;
import com.beligum.blocks.rdf.ontology.vocabularies.endpoints.GeonameQueryEndpoint;
import com.beligum.blocks.rdf.ontology.vocabularies.endpoints.SettingsQueryEndpoint;
import com.beligum.blocks.rdf.ontology.vocabularies.geo.AbstractGeoname;
import gen.com.beligum.blocks.core.constants.blocks.core;
import gen.com.beligum.blocks.core.messages.blocks.ontology;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class Classes implements RdfResourceFactory
{
    //-----CONSTANTS-----
    public static final int AUTOCOMPLETE_MAX_RESULTS = 10;

    //-----ENTRIES-----
    public static final RdfClass Person = new RdfClassImpl("Person",
                                                           SettingsVocabulary.INSTANCE,
                                                           ontology.Entries.classTitle_Person,
                                                           ontology.Entries.classLabel_Person,
                                                           new URI[] { DBR.INSTANCE.resolve("Person"),
                                                                       SCHEMA.INSTANCE.resolve("Person"),
                                                                       FOAF.INSTANCE.resolve("Person")
                                                           },
                                                           true,
                                                           new SettingsQueryEndpoint()
    );

    public static final RdfClass Organization = new RdfClassImpl("Organization",
                                                           SettingsVocabulary.INSTANCE,
                                                           ontology.Entries.classTitle_Organization,
                                                           ontology.Entries.classLabel_Organization,
                                                           new URI[] { DBR.INSTANCE.resolve("Organization"),
                                                                       SCHEMA.INSTANCE.resolve("Organization"),
                                                                       FOAF.INSTANCE.resolve("Organization")
                                                           },
                                                           true,
                                                           new SettingsQueryEndpoint()
    );

    public static final RdfClass Page = new RdfClassImpl("Page",
                                                         SettingsVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_Page,
                                                         ontology.Entries.classLabel_Page,
                                                         new URI[] { DBR.INSTANCE.resolve("Web_page"),
                                                                     SCHEMA.INSTANCE.resolve("WebPage")
                                                         },
                                                         true,
                                                         new SettingsQueryEndpoint());

    public static final RdfClass Country = new RdfClassImpl("Country",
                                                            SettingsVocabulary.INSTANCE,
                                                            ontology.Entries.classTitle_Country,
                                                            ontology.Entries.classLabel_Country,
                                                            new URI[] { DBR.INSTANCE.resolve("Country"),
                                                                        SCHEMA.INSTANCE.resolve("Country")
                                                            },
                                                            //note: because we use a fixed-value ontology (geonames), we don't make this public (so users can select it as a type for their page)
                                                            false,
                                                            new GeonameQueryEndpoint(AbstractGeoname.Type.COUNTRY));

    public static final RdfClass City = new RdfClassImpl("City",
                                                         SettingsVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_City,
                                                         ontology.Entries.classLabel_City,
                                                         new URI[] { DBR.INSTANCE.resolve("City"),
                                                                     SCHEMA.INSTANCE.resolve("City")
                                                         },
                                                         //note: because we use a fixed-value ontology (geonames), we don't make this public (so users can select it as a type for their page)
                                                         false,
                                                         new GeonameQueryEndpoint(AbstractGeoname.Type.CITY));

    public static final RdfClass LogEntry = new RdfClassImpl("LogEntry",
                                                         SettingsVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_LogEntry,
                                                         ontology.Entries.classLabel_LogEntry,
                                                         new URI[] { //RLOG Entry?
                                                         },
                                                         false,
                                                         null);

    //-----CONFIGS-----
    public static final InputTypeConfig DEFAULT_PERSON_ENDPOINT_CONFIG = new InputTypeConfig(new String[][] {
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResources(Classes.Person.getCurieName(), AUTOCOMPLETE_MAX_RESULTS, true, "").getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResource(Classes.Person.getCurieName(), URI.create("")).getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS.getValue(), "" + AUTOCOMPLETE_MAX_RESULTS
                    }
    });

    public static final InputTypeConfig DEFAULT_ORGANIZATION_ENDPOINT_CONFIG = new InputTypeConfig(new String[][] {
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResources(Classes.Organization.getCurieName(), AUTOCOMPLETE_MAX_RESULTS, true, "").getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResource(Classes.Organization.getCurieName(), URI.create("")).getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS.getValue(), "" + AUTOCOMPLETE_MAX_RESULTS
                    }
    });

    public static final InputTypeConfig DEFAULT_CITY_ENDPOINT_CONFIG = new InputTypeConfig(new String[][] {
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResources(Classes.City.getCurieName(), AUTOCOMPLETE_MAX_RESULTS, true, "").getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResource(Classes.City.getCurieName(), URI.create("")).getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS.getValue(),
                      "" + AUTOCOMPLETE_MAX_RESULTS
                    }
    });

    public static final InputTypeConfig DEFAULT_COUNTRY_ENDPOINT_CONFIG = new InputTypeConfig(new String[][] {
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResources(Classes.Country.getCurieName(), AUTOCOMPLETE_MAX_RESULTS, true, "").getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT.getValue(),
                      gen.com.beligum.blocks.endpoints.RdfEndpointRoutes
                                      .getResource(Classes.Country.getCurieName(),
                                                   URI.create("")).getAbsoluteUrl()
                    },
                    { core.Entries.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS.getValue(),
                      "" + AUTOCOMPLETE_MAX_RESULTS
                    }
    });
}

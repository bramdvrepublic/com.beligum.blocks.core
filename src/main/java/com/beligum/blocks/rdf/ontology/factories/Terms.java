package com.beligum.blocks.rdf.ontology.factories;

import com.beligum.blocks.config.InputType;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ontology.RdfPropertyImpl;
import com.beligum.blocks.rdf.ontology.vocabularies.*;
import gen.com.beligum.blocks.core.messages.blocks.ontology;

import java.net.URI;

import static com.beligum.blocks.rdf.ontology.factories.Classes.*;

/**
 * Created by bram on 3/22/16.
 */
public class Terms implements RdfResourceFactory
{
    //-----CONSTANTS-----
    private static final int AUTOCOMPLETE_MAX_RESULTS = 10;

    //-----ENTRIES-----
    public static final RdfProperty givenName = new RdfPropertyImpl("givenName",
                                                                    SettingsVocabulary.INSTANCE,
                                                                    ontology.Entries.propertyTitle_givenName,
                                                                    ontology.Entries.propertyLabel_givenName,
                                                                    XSD.STRING,
                                                                    InputType.InlineEditor,
                                                                    null,
                                                                    new URI[] { FOAF.INSTANCE.resolve("givenName"),
                                                                                VCARD.INSTANCE.resolve("given-name")
                                                                    },
                                                                    true);

    public static final RdfProperty familyName = new RdfPropertyImpl("familyName",
                                                                     SettingsVocabulary.INSTANCE,
                                                                     ontology.Entries.propertyTitle_familyName,
                                                                     ontology.Entries.propertyLabel_familyName,
                                                                     XSD.STRING,
                                                                     InputType.InlineEditor,
                                                                     null,
                                                                     new URI[] { FOAF.INSTANCE.resolve("familyName"),
                                                                                 VCARD.INSTANCE.resolve("family-name")
                                                                     },
                                                                     true);

    public static final RdfProperty name = new RdfPropertyImpl("name",
                                                               SettingsVocabulary.INSTANCE,
                                                               ontology.Entries.propertyTitle_name,
                                                               ontology.Entries.propertyLabel_name,
                                                               XSD.STRING,
                                                               InputType.InlineEditor,
                                                               null,
                                                               new URI[] { FOAF.INSTANCE.resolve("name"),
                                                                           VCARD.INSTANCE.resolve("fn")
                                                               },
                                                               true);

    public static final RdfProperty email = new RdfPropertyImpl("email",
                                                                SettingsVocabulary.INSTANCE,
                                                                ontology.Entries.propertyTitle_email,
                                                                ontology.Entries.propertyLabel_email,
                                                                //TODO is this right? Eg. found this: https://github.com/FacultadInformatica-LinkedData/Curso2014-2015/blob/master/Assignment3/rdf%20examples/example.rdf
                                                                XSD.STRING,
                                                                InputType.InlineEditor,
                                                                null,
                                                                new URI[] { FOAF.INSTANCE.resolve("email"),
                                                                            VCARD.INSTANCE.resolve("email")
                                                                },
                                                                true);

    public static final RdfProperty streetName = new RdfPropertyImpl("streetName",
                                                                     SettingsVocabulary.INSTANCE,
                                                                     ontology.Entries.propertyTitle_streetName,
                                                                     ontology.Entries.propertyLabel_streetName,
                                                                     XSD.STRING,
                                                                     InputType.InlineEditor,
                                                                     null,
                                                                     new URI[] { XV.INSTANCE.resolve("streetName")
                                                                     },
                                                                     true);

    public static final RdfProperty streetNumber = new RdfPropertyImpl("streetNumber",
                                                                       SettingsVocabulary.INSTANCE,
                                                                       ontology.Entries.propertyTitle_streetNumber,
                                                                       ontology.Entries.propertyLabel_streetNumber,
                                                                       XSD.STRING,
                                                                       InputType.InlineEditor,
                                                                       null,
                                                                       new URI[] { XV.INSTANCE.resolve("streetNumber")
                                                                       },
                                                                       true);

    public static final RdfProperty streetAddress = new RdfPropertyImpl("streetAddress",
                                                                        SettingsVocabulary.INSTANCE,
                                                                        ontology.Entries.propertyTitle_streetAddress,
                                                                        ontology.Entries.propertyLabel_streetAddress,
                                                                        XSD.STRING,
                                                                        InputType.InlineEditor,
                                                                        null,
                                                                        new URI[] { VCARD.INSTANCE.resolve("street-address"),
                                                                                    XV.INSTANCE.resolve("streetAddress")
                                                                        },
                                                                        true);

    public static final RdfProperty postalCode = new RdfPropertyImpl("postalCode",
                                                                     SettingsVocabulary.INSTANCE,
                                                                     ontology.Entries.propertyTitle_postalCode,
                                                                     ontology.Entries.propertyLabel_postalCode,
                                                                     XSD.STRING,
                                                                     InputType.InlineEditor,
                                                                     null,
                                                                     new URI[] { VCARD.INSTANCE.resolve("street-address"),
                                                                                 DBO.INSTANCE.resolve("postal-code")
                                                                     },
                                                                     true);

    public static final RdfProperty city = new RdfPropertyImpl("city",
                                                               SettingsVocabulary.INSTANCE,
                                                               ontology.Entries.propertyTitle_city,
                                                               ontology.Entries.propertyLabel_city,
                                                               Classes.City,
                                                               InputType.Resource,
                                                               DEFAULT_CITY_ENDPOINT_CONFIG,
                                                               new URI[] { DBO.INSTANCE.resolve("city")
                                                               },
                                                               true);

    public static final RdfProperty country = new RdfPropertyImpl("country",
                                                                  SettingsVocabulary.INSTANCE,
                                                                  ontology.Entries.propertyTitle_country,
                                                                  ontology.Entries.propertyLabel_country,
                                                                  Classes.Country,
                                                                  InputType.Resource,
                                                                  DEFAULT_COUNTRY_ENDPOINT_CONFIG,
                                                                  new URI[] { DBO.INSTANCE.resolve("country")
                                                                  },
                                                                  true);

    public static final RdfProperty createdAt = new RdfPropertyImpl("createdAt",
                                                                    SettingsVocabulary.INSTANCE,
                                                                    ontology.Entries.propertyTitle_createdAt,
                                                                    ontology.Entries.propertyLabel_createdAt,
                                                                    XSD.DATE_TIME,
                                                                    InputType.DateTime,
                                                                    null,
                                                                    new URI[] { DC.INSTANCE.resolve("created")
                                                                    },
                                                                    true);

    public static final RdfProperty createdBy = new RdfPropertyImpl("createdBy",
                                                                    SettingsVocabulary.INSTANCE,
                                                                    ontology.Entries.propertyTitle_createdBy,
                                                                    ontology.Entries.propertyLabel_createdBy,
                                                                    Classes.Person,
                                                                    InputType.Resource,
                                                                    DEFAULT_PERSON_ENDPOINT_CONFIG,
                                                                    new URI[] { DC.INSTANCE.resolve("creator")
                                                                    },
                                                                    true);

    public static final RdfProperty comment = new RdfPropertyImpl("comment",
                                                                  SettingsVocabulary.INSTANCE,
                                                                  ontology.Entries.propertyTitle_comment,
                                                                  ontology.Entries.propertyLabel_comment,
                                                                  RDF.HTML,
                                                                  InputType.Editor,
                                                                  null,
                                                                  new URI[] { DBO.INSTANCE.resolve("comment")
                                                                  },
                                                                  true);

    //TODO integrate this in the blocks-image block
    public static final RdfProperty image = new RdfPropertyImpl("image",
                                                                SettingsVocabulary.INSTANCE,
                                                                ontology.Entries.propertyTitle_image,
                                                                ontology.Entries.propertyLabel_image,
                                                                XSD.ANY_URI,
                                                                InputType.Uri,
                                                                null,
                                                                new URI[] {
                                                                },
                                                                false);

    //TODO integrate this in the blocks-text block
    public static final RdfProperty text = new RdfPropertyImpl("text",
                                                                SettingsVocabulary.INSTANCE,
                                                                ontology.Entries.propertyTitle_text,
                                                                ontology.Entries.propertyLabel_text,
                                                                RDF.HTML,
                                                                InputType.Editor,
                                                                null,
                                                                new URI[] {
                                                                },
                                                                false);

    //TODO integrate this in the ... (page?) block
    public static final RdfProperty title = new RdfPropertyImpl("title",
                                                               SettingsVocabulary.INSTANCE,
                                                               ontology.Entries.propertyTitle_title,
                                                               ontology.Entries.propertyLabel_title,
                                                               XSD.STRING,
                                                               InputType.InlineEditor,
                                                               null,
                                                               new URI[] {
                                                               },
                                                               false);
}
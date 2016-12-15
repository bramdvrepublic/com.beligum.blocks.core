package com.beligum.blocks.rdf.ontology.factories;

import com.beligum.blocks.config.InputType;
import com.beligum.blocks.config.InputTypeConfig;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ontology.RdfPropertyImpl;
import com.beligum.blocks.rdf.ontology.vocabularies.*;
import com.beligum.blocks.rdf.ontology.vocabularies.endpoints.LanguageEnumQueryEndpoint;
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
    public static final RdfProperty identifier = new RdfPropertyImpl("identifier",
                                                                     SettingsVocabulary.INSTANCE,
                                                                     ontology.Entries.propertyTitle_identifier,
                                                                     ontology.Entries.propertyLabel_identifier,
                                                                     XSD.STRING,
                                                                     InputType.InlineEditor,
                                                                     null,
                                                                     new URI[] { DC.INSTANCE.resolve("identifier")
                                                                     },
                                                                     true);

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

    public static final RdfProperty role = new RdfPropertyImpl("role",
                                                               SettingsVocabulary.INSTANCE,
                                                               ontology.Entries.propertyTitle_role,
                                                               ontology.Entries.propertyLabel_role,
                                                               XSD.STRING,
                                                               InputType.InlineEditor,
                                                               null,
                                                               new URI[] { VCARD.INSTANCE.resolve("role")
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

    //TODO integrate this in the blocks-video block
    public static final RdfProperty video = new RdfPropertyImpl("video",
                                                                SettingsVocabulary.INSTANCE,
                                                                ontology.Entries.propertyTitle_video,
                                                                ontology.Entries.propertyLabel_video,
                                                                XSD.ANY_URI,
                                                                InputType.Uri,
                                                                null,
                                                                new URI[] {
                                                                },
                                                                false);

    public static final RdfProperty description = new RdfPropertyImpl("description",
                                                                      SettingsVocabulary.INSTANCE,
                                                                      ontology.Entries.propertyTitle_description,
                                                                      ontology.Entries.propertyLabel_description,
                                                                      XSD.STRING,
                                                                      InputType.Editor,
                                                                      null,
                                                                      new URI[] {
                                                                      },
                                                                      false);

    public static final RdfProperty type = new RdfPropertyImpl("type",
                                                               SettingsVocabulary.INSTANCE,
                                                               ontology.Entries.propertyTitle_type,
                                                               ontology.Entries.propertyLabel_type,
                                                               XSD.STRING,
                                                               //TODO maybe a drop down instead?
                                                               InputType.InlineEditor,
                                                               null,
                                                               new URI[] {
                                                                               DC.INSTANCE.resolve("type")
                                                               },
                                                               false);

    public static final RdfProperty subject = new RdfPropertyImpl("subject",
                                                                  SettingsVocabulary.INSTANCE,
                                                                  ontology.Entries.propertyTitle_subject,
                                                                  ontology.Entries.propertyLabel_subject,
                                                                  XSD.ANY_URI,
                                                                  InputType.Resource,
                                                                  //TODO implement a general 'all' endpoint?
                                                                  null,
                                                                  new URI[] {
                                                                                  DC.INSTANCE.resolve("subject")
                                                                  },
                                                                  false);

    public static final RdfProperty software = new RdfPropertyImpl("software",
                                                                   SettingsVocabulary.INSTANCE,
                                                                   ontology.Entries.propertyTitle_software,
                                                                   ontology.Entries.propertyLabel_software,
                                                                   XSD.STRING,
                                                                   InputType.InlineEditor,
                                                                   null,
                                                                   new URI[] {
                                                                   },
                                                                   false);

    public static final RdfProperty softwareVersion = new RdfPropertyImpl("softwareVersion",
                                                                          SettingsVocabulary.INSTANCE,
                                                                          ontology.Entries.propertyTitle_softwareVersion,
                                                                          ontology.Entries.propertyLabel_softwareVersion,
                                                                          XSD.STRING,
                                                                          InputType.InlineEditor,
                                                                          null,
                                                                          new URI[] {
                                                                          },
                                                                          false);

    public static final RdfProperty language = new RdfPropertyImpl("language",
                                                                   SettingsVocabulary.INSTANCE,
                                                                   ontology.Entries.propertyTitle_language,
                                                                   ontology.Entries.propertyLabel_language,
                                                                   XSD.LANGUAGE,
                                                                   InputType.Enum,
                                                                   null,
                                                                   new URI[] { DC.INSTANCE.resolve("language")
                                                                   },
                                                                   true);

    public static final RdfProperty file = new RdfPropertyImpl("file",
                                                               SettingsVocabulary.INSTANCE,
                                                               ontology.Entries.propertyTitle_file,
                                                               ontology.Entries.propertyLabel_file,
                                                               XSD.ANY_URI,
                                                               InputType.Uri,
                                                               null,
                                                               new URI[] {
                                                               },
                                                               false);

    public static final RdfProperty organization = new RdfPropertyImpl("organization",
                                                                       SettingsVocabulary.INSTANCE,
                                                                       ontology.Entries.propertyTitle_organization,
                                                                       ontology.Entries.propertyLabel_organization,
                                                                       Classes.Organization,
                                                                       InputType.Resource,
                                                                       DEFAULT_ORGANIZATION_ENDPOINT_CONFIG,
                                                                       new URI[] {
                                                                       },
                                                                       false);

    public static final RdfProperty sameAs = new RdfPropertyImpl("sameAs",
                                                                 SettingsVocabulary.INSTANCE,
                                                                 ontology.Entries.propertyTitle_sameAs,
                                                                 ontology.Entries.propertyLabel_sameAs,
                                                                 XSD.ANY_URI,
                                                                 InputType.InlineEditor,
                                                                 null,
                                                                 new URI[] { OWL.SAMEAS.getFullName()
                                                                 },
                                                                 false);

    //-----CONFIGS-----
    /**
     * Need to come here, because we have a cyclic reference otherwise (we would be using the property during it's static initialization)
     */
    static {
        Terms.language.setEndpoint(new LanguageEnumQueryEndpoint(Terms.language));

        Terms.language.setWidgetConfig(new InputTypeConfig(new String[][] {
                        { gen.com.beligum.blocks.core.constants.blocks.core.Entries.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT.getValue(),
                          //let's re-use the same endpoint for the enum as for the resources so we can re-use it's backend code
                          gen.com.beligum.blocks.endpoints.RdfEndpointRoutes.getResources(Terms.language.getCurieName(), -1, false, "").getAbsoluteUrl()
                        },
                        }));
    }
}

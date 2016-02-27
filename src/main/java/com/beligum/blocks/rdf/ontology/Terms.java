package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.SidebarWidget;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfPropertyCollection;
import org.semarglproject.vocab.XSD;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class Terms implements RdfPropertyCollection
{
    public static final RdfPropertyImpl postalCode = new RdfPropertyImpl("postalCode",
                                                                         Settings.instance().getRdfOntologyUri(),
                                                                         gen.com.beligum.blocks.core.messages.blocks.core.Entries.ontologyPropertyTitle_postalCode,
                                                                         gen.com.beligum.blocks.core.messages.blocks.core.Entries.ontologyPropertyLabel_postalCode,
                                                                         URI.create(XSD.STRING),
                                                                         SidebarWidget.InlineEditor,
                                                                         new URI[] { URI.create("http://schema.org/postalCode"),
                                                                                     URI.create("http://dbpedia.org/ontology/postalCode")
                                                   });

    public static final RdfPropertyImpl isVerified = new RdfPropertyImpl("isVerified",
                                                                         Settings.instance().getRdfOntologyUri(),
                                                                         gen.com.beligum.blocks.core.messages.blocks.core.Entries.ontologyPropertyTitle_isVerified,
                                                                         gen.com.beligum.blocks.core.messages.blocks.core.Entries.ontologyPropertyLabel_isVerified,
                                                                         URI.create(XSD.BOOLEAN),
                                                                         SidebarWidget.ToggleButton,
                                                                         null);

    private static final RdfProperty[] COLLECTION = {
                    postalCode,
                    isVerified
    };

    @Override
    public RdfProperty[] getProperties()
    {
        return COLLECTION;
    }
}

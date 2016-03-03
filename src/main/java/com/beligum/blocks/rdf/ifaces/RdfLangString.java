package com.beligum.blocks.rdf.ifaces;

import java.util.Locale;

/**
 * Created by bram on 3/2/16.
 */
public interface RdfLangString extends RdfLiteral
{
    /**
     * Returns the (parsed) language-tag of this literal value or null if no such language tag exists.
     */
    Locale getLanguage();
}

package com.beligum.blocks.config;

import com.beligum.base.server.R;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Locale;

/**
 * This adapter sends out the language of the locale and only accepts registered languages back.
 *
 * Created by bram on May 02, 2019
 */
public class LanguageAdapter extends XmlAdapter<String, Locale>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public String marshal(Locale lang)
    {
        return lang == null ? null : lang.getLanguage();
    }
    @Override
    public Locale unmarshal(String val)
    {
        return R.configuration().getLocaleForLanguage(val);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

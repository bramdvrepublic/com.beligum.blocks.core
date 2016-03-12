package com.beligum.blocks.endpoints.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by bram on 3/12/16.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
                getterVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE)
public class GeonameLangValue
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String lang;
    private String value;

    //-----CONSTRUCTORS-----
    public GeonameLangValue()
    {
    }
    public GeonameLangValue(String value)
    {
        this.value = value;
    }

    //-----PUBLIC METHODS-----
    public String getLang()
    {
        return lang;
    }
    public String getValue()
    {
        return value;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

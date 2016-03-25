package com.beligum.blocks.config;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import static com.beligum.blocks.config.InputType.valueOfConstant;

/**
 * Created by bram on 3/25/16.
 */
public class InputTypeAdapter extends XmlAdapter<String, InputType>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public String marshal(InputType inputType)
    {
        return inputType == null ? null : inputType.getConstant();
    }
    @Override
    public InputType unmarshal(String val)
    {
        return StringUtils.isEmpty(val) ? null : valueOfConstant(val);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

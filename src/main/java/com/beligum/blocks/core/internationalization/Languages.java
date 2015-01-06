package com.beligum.blocks.core.internationalization;

import com.beligum.blocks.core.config.BlocksConfig;

import java.util.Locale;

/**
 * Created by bas on 06.01.15.
 */
public class Languages
{
    /**
     *
     * @param languageCode
     * @return a standardized language-code for the language-code specified, or the default-language if null was specified
     */
    static public String getStandardizedLanguage(String languageCode){
        if(languageCode != null && !languageCode.equals(BlocksConfig.getDefaultLanguage())){
            return new Locale(languageCode).getLanguage();
        }
        else{
            return BlocksConfig.getDefaultLanguage();
        }
    }
}

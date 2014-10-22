package com.beligum.blocks.core.config;

import com.beligum.core.framework.base.R;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{
    public static String getTemplateFolder()
    {
        return getConfiguration("blocks.template-folder");
    }

    public static String getSiteDomain()
    {
        return getConfiguration("blocks.site.domain");
    }

    public static String getSiteDBAlias()
    {
        return getConfiguration("blocks.site.db-alias");
    }

    /**
     * return the text from the applications configuration file in specified tag
     * @param configTag the configuration-tag
     * @return the value present in the configuration-tag, if '/' is the last character, it is removed
     */
    private static String getConfiguration(String configTag){
        String retVal = R.configuration().getString(configTag);
        if (retVal != null) {
            if (retVal.charAt(retVal.length() - 1) == '/') {
                retVal = retVal.substring(0, retVal.length()-1);
            }
        }
        return retVal;
    }
}

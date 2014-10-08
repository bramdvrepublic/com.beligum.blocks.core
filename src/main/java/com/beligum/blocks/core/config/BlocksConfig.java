package com.beligum.blocks.core.config;

import com.beligum.core.framework.base.R;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{
    public static String getTemplateFolder()
    {
        String retVal = R.configuration().getString("files.template-path");
        if (retVal != null) {
            if (retVal.charAt(retVal.length() - 1) != '/') {
                retVal += "/";
            }
        }
        return retVal;
    }
}

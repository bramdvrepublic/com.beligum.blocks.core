package com.beligum.blocks.fs.pages;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.pages.ifaces.Page;

import java.io.IOException;

/**
 * Created by bram on 5/2/16.
 */
public class ReadWritePage extends DefaultPageImpl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadWritePage(Page readOnlyPage) throws IOException
    {
        super(readOnlyPage.getRepository(), readOnlyPage.getUri(), readOnlyPage.getLanguage(), readOnlyPage.getRegisteredMimeType(), readOnlyPage.allowEternalCaching(), Settings.instance().getPagesStorePath(), StorageFactory.getPageStoreFileSystem(), readOnlyPage.getLocalStoragePath());
    }
//    public ReadWritePage(Path relativeLocalFile) throws IOException
//    {
//        super(relativeLocalFile, Settings.instance().getPagesStorePath(), StorageFactory.getPageStoreFileSystem());
//    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

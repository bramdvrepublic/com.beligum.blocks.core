package com.beligum.blocks.fs.pages;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 5/2/16.
 */
public class ReadWritePage extends DefaultPageImpl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadWritePage(URI publicUri) throws IOException
    {
        super(publicUri, Settings.instance().getPagesStorePath(), StorageFactory.getPageStoreFileSystem());
    }
    public ReadWritePage(Path relativeLocalFile) throws IOException
    {
        super(relativeLocalFile, Settings.instance().getPagesStorePath(), StorageFactory.getPageStoreFileSystem());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

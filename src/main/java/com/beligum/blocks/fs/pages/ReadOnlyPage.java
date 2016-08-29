package com.beligum.blocks.fs.pages;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 5/2/16.
 */
public class ReadOnlyPage extends DefaultPageImpl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadOnlyPage(URI publicUri) throws IOException
    {
        super(publicUri, Settings.instance().getPagesViewPath(), StorageFactory.getPageViewFileSystem());
    }
    public ReadOnlyPage(Path relativeLocalFile) throws IOException
    {
        super(relativeLocalFile, Settings.instance().getPagesViewPath(), StorageFactory.getPageViewFileSystem());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

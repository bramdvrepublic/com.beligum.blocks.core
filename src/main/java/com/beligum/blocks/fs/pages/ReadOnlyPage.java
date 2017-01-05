package com.beligum.blocks.fs.pages;

import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;

import java.io.IOException;

/**
 * Created by bram on 5/2/16.
 */
public class ReadOnlyPage extends DefaultPageImpl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadOnlyPage(ResourceRequest request, ResourceRepository repository) throws IOException
    {
        super(repository, request, Settings.instance().getPagesViewPath(), StorageFactory.getPageViewFileSystem());
    }
    //    public ReadOnlyPage(Path relativeLocalFile) throws IOException
//    {
//        super(relativeLocalFile, Settings.instance().getPagesViewPath(), StorageFactory.getPageViewFileSystem());
//    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

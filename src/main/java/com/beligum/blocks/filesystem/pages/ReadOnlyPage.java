package com.beligum.blocks.filesystem.pages;

import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;

import java.io.IOException;

/**
 * Created by bram on 5/2/16.
 */
public class ReadOnlyPage extends DefaultPage
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadOnlyPage(ResourceRequest request) throws IOException
    {
        super(request, Settings.instance().getPagesViewPath(), StorageFactory.getPageViewFileSystem());
    }
    //    public ReadOnlyPage(Path relativeLocalFile) throws IOException
//    {
//        super(relativeLocalFile, Settings.instance().getPagesViewPath(), StorageFactory.getPageViewFileSystem());
//    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceInputStream newInputStream() throws IOException
    {
        return new ResourceInputStream(this.fileContext.open(this.getNormalizedPageProxyPath()), this.fileContext.getFileStatus(this.getNormalizedPageProxyPath()).getLen());
    }
    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

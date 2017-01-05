package com.beligum.blocks.fs;

import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.resources.ifaces.ResourceRepository;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * This holds all details about the local storage of a Page.
 *
 * Created by bram on 12/29/16.
 */
public class PageResource extends AbstractBlocksResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public PageResource(ResourceRequest request, ResourceRepository resolver, FileContext fileContext,
                        Path localPath) throws IOException
    {
        super(request, resolver, fileContext, localPath);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

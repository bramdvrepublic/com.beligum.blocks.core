package com.beligum.blocks.filesystem.index.reindex;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;

import java.io.IOException;

/**
 * Created by bram on 11/05/17.
 */
public class PageReindexTask extends ReindexTask
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    protected void runTaskFor(Resource resource, ResourceRepository.IndexOption indexConnectionsOption) throws IOException
    {
        //effectively reindex the page
        resource.getRepository().reindex(resource, indexConnectionsOption);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

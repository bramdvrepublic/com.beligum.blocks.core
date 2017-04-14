package com.beligum.blocks.config;

import com.beligum.base.utils.Logger;
import com.github.dexecutor.core.task.Task;
import com.github.dexecutor.core.task.TaskProvider;

/**
 * Created by bram on 14/04/17.
 */
public class RdfClassIndexer implements TaskProvider<RdfClassNode, RdfClassNode>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public RdfClassIndexer()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public Task<RdfClassNode, RdfClassNode> provideTask(RdfClassNode node)
    {
        return new Task<RdfClassNode, RdfClassNode>()
        {
            private static final long serialVersionUID = 1L;

            public RdfClassNode execute()
            {
                Logger.info("Indexing "+node.getRdfClass());

                return node;
            }
        };
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

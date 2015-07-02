package com.beligum.blocks.resources.dummy;

import com.beligum.blocks.database.DummyBlocksController;
import com.beligum.blocks.database.interfaces.BlocksController;
import com.beligum.blocks.resources.AbstractNode;

import java.util.Locale;

/**
 * Created by wouter on 22/06/15.
 */
public class DummyNode extends AbstractNode
{

    public DummyNode(Object value, Locale lang) {
        super(value, lang);
    }

    @Override
    public BlocksController getDatabase()
    {
        return DummyBlocksController.instance();
    }
}

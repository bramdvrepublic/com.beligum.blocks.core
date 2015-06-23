package com.beligum.blocks.resources.dummy;

import com.beligum.blocks.database.DummyBlocksDatabase;
import com.beligum.blocks.database.interfaces.BlocksDatabase;
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
    public BlocksDatabase getDatabase()
    {
        return DummyBlocksDatabase.instance();
    }
}

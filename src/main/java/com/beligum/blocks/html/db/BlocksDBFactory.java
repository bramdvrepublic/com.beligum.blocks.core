package com.beligum.blocks.html.db;

/**
 * Created by wouter on 21/11/14.
 */
public class BlocksDBFactory
{
    private static BlocksDBFactory instance;

    private BlocksDB db;

    private BlocksDBFactory() {
        this.db = new BlocksMemDB();
    }

    public  static BlocksDBFactory instance() {
        if (BlocksDBFactory.instance == null) {
            BlocksDBFactory.instance = new BlocksDBFactory();
        }
        return BlocksDBFactory.instance;
    }

    public BlocksDB db() {
        return this.db;
    }
}

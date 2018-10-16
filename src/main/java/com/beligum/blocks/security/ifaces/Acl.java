package com.beligum.blocks.security.ifaces;

public interface Acl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----

    /**
     * This main representation of this ACL as the minimum role level (inclusive) a principal needs to have to be part of it
     */
    int getLevel();

    /**
     * The human-readable label of this role, to be used in UI
     */
    String getLabel();

}

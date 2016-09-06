package com.beligum.blocks.fs.hdfs.bitronix;

import javax.transaction.xa.XAResource;

/**
 * Created by bram on 9/6/16.
 */
public interface CustomBitronixResourceProducer extends bitronix.tm.resource.common.XAResourceProducer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    void registerResource(XAResource resource);
    void unregisterResource(XAResource resource);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

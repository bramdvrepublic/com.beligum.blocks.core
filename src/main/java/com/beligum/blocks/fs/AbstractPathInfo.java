package com.beligum.blocks.fs;

import com.beligum.blocks.fs.ifaces.PathInfo;

/**
 * Created by bram on 1/19/16.
 */
abstract class AbstractPathInfo<T> implements PathInfo<T>
{
    //-----CONSTANTS-----
    //sync with eg. HDFS meta files (eg. hidden .crc files) (and also less chance on conflicts)
    protected static final String LOCK_FILE_PREFIX = ".";
    protected static final long DEFAULT_LOCK_BACK_OFF = 100;
    protected static final long DEFAULT_LOCK_TIMEOUT = 5000;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    protected abstract T getLockFile();

    //-----PRIVATE METHODS-----

}

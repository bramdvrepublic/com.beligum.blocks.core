package com.beligum.blocks.fs.atomic.hdfs.exceptions;

import java.io.IOException;

/**
 * Created by bram on 2/1/16.
 */
public class TransactionException extends IOException
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public TransactionException()
    {
    }
    public TransactionException(String message)
    {
        super(message);
    }
    public TransactionException(String message, Throwable cause)
    {
        super(message, cause);
    }
    public TransactionException(Throwable cause)
    {
        super(cause);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

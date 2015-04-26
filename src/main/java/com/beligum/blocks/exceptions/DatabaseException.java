package com.beligum.blocks.exceptions;

/**
 * Created by bas on 23.10.14.
 */
public class DatabaseException extends Exception
{
    public DatabaseException()
    {
    }
    public DatabaseException(String message)
    {
        super(message);
    }
    public DatabaseException(String message, Throwable cause)
    {
        super(message, cause);
    }
    public DatabaseException(Throwable cause)
    {
        super(cause);
    }
    public DatabaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
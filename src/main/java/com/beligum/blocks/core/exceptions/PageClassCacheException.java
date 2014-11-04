package com.beligum.blocks.core.exceptions;

/**
 * Created by bas on 23.10.14.
 */
public class PageClassCacheException extends Exception
{
    public PageClassCacheException()
    {
    }
    public PageClassCacheException(String message)
    {
        super(message);
    }
    public PageClassCacheException(String message, Throwable cause)
    {
        super(message, cause);
    }
    public PageClassCacheException(Throwable cause)
    {
        super(cause);
    }
    public PageClassCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

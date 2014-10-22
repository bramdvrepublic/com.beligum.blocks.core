package com.beligum.blocks.core.exceptions;
/**
 * Created by bas on 30.09.14.
 */
public class PageParserException extends RuntimeException
{
    public PageParserException()
    {
        super();
    }
    public PageParserException(String message)
    {
        super(message);
    }
    public PageParserException(String message, Throwable cause)
    {
        super(message, cause);
    }
    public PageParserException(Throwable cause)
    {
        super(cause);
    }
    public PageParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

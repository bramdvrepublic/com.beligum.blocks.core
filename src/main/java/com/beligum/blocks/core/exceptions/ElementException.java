package com.beligum.blocks.core.exceptions;

/**
 * Created by bas on 27.10.14.
 * Exception thrown when something is wrong with an (storable) element of the html-element-tree
 */
public class ElementException extends Exception
{
    public ElementException()
    {
    }
    public ElementException(String message)
    {
        super(message);
    }
    public ElementException(String message, Throwable cause)
    {
        super(message, cause);
    }
    public ElementException(Throwable cause)
    {
        super(cause);
    }
    public ElementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package com.beligum.blocks.exceptions;

/**
 * Created by wouter on 3/06/15.
 */
public class RdfException extends Exception
{
    public RdfException(String message) {
        super(message);
    }

    public RdfException(String message, Throwable throwable) {
        super(message, throwable);
    }

}

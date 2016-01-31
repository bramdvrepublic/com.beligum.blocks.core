package com.beligum.blocks.fs.atomic.exceptions;

public class InconsistentStateException extends NestedException
{
    public InconsistentStateException()
    {
    }
    public InconsistentStateException(Exception e)
    {
        super(e);
    }
}

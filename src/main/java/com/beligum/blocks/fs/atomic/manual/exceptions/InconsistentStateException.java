package com.beligum.blocks.fs.atomic.manual.exceptions;

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

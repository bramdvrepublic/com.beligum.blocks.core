package com.beligum.blocks.fs.atomic.orig;

public class InconsistentStateException extends NestedException
{
    InconsistentStateException()
    {
    }
    InconsistentStateException(Exception e)
    {
        super(e);
    }
}

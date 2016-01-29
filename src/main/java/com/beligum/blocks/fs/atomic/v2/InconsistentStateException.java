package com.beligum.blocks.fs.atomic.v2;

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

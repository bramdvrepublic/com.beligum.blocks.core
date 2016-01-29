package com.beligum.blocks.fs.atomic.orig;

public class TransactionException extends NestedException
{
    Exception nestedException2;

    TransactionException()
    {
    }
    TransactionException(Exception e)
    {
        super(e);
    }
    TransactionException(Exception e1, Exception e2)
    {
        super(e1);
        nestedException2 = e2;
    }

}

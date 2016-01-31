package com.beligum.blocks.fs.atomic.exceptions;

public class TransactionException extends NestedException
{
    Exception nestedException2;

    public TransactionException()
    {
    }
    public TransactionException(Exception e)
    {
        super(e);
    }
    public TransactionException(Exception e1, Exception e2)
    {
        super(e1);
        nestedException2 = e2;
    }

}

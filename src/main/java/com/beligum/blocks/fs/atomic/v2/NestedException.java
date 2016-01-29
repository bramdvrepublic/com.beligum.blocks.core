package com.beligum.blocks.fs.atomic.v2;

import java.io.PrintStream;
import java.io.PrintWriter;

class NestedException extends Exception
{
    Exception nestedException;

    NestedException()
    {
    }

    NestedException(String message)
    {
        super(message);
    }

    NestedException(Exception e)
    {
        nestedException = e;
    }

    public String toString()
    {
        if (nestedException != null)
            return super.toString() + " [" + nestedException.toString() + "]";
        else
            return super.toString();
    }

    public void printStackTrace()
    {
        if (nestedException == null)
            super.printStackTrace();
        else {
            System.err.println("nested exception: ");
            nestedException.printStackTrace();
        }
    }

    public void printStackTrace(PrintStream out)
    {
        printStackTrace(new PrintWriter(out, true));
    }

    public void printStackTrace(PrintWriter out)
    {
        synchronized (out) {
            if (nestedException == null)
                super.printStackTrace(out);
            else {
                out.println("nested exception: ");
                nestedException.printStackTrace(out);
            }
        }
    }
}



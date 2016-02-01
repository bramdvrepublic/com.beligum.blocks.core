package com.beligum.blocks.fs.atomic.manual.exceptions;

import java.io.*;

public class NestedException extends Exception
{
    Exception nestedException;
    public NestedException()
    {
    }
    public NestedException(String message)
    {
        super(message);
    }
    public NestedException(Exception e)
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



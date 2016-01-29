//package com.beligum.blocks.fs.atomic.orig;
//
//import java.io.*;
//
//class RenameAction extends Action
//{
//    private File source, dest;
//
//    RenameAction(File s, File d) throws IOException
//    {
//        source = s;
//        if (!source.exists())
//            throw new FileNotFoundException(source.toString());
//        dest = d;
//    }
//
//    protected Object run() throws IOException
//    {
//        // (b) original was: Tools.rename(source, dest);
//        rename(dest, source);
//
//        return null;
//    }
//
//    protected boolean succeeded()
//    {
//        return !source.exists();
//    }
//
//    protected void undo() throws IOException
//    {
//        if (!dest.exists()) // If the destination file doesn't exist,
//            return;                        // an earlier recovery attempt undid this already.
//
//        // Forcibly delete the source file if it exists.  It could exist
//        // as a result of an earlier, failed recovery.
//        if (source.exists()) {
//            // (b) original was: Tools.delete(source);
//            delete(source);
//        }
//
//        // (b) original was: Tools.rename(dest, source);
//        rename(dest, source);
//    }
//
//    public String toString()
//    {
//        return "[RENAME source=" + source + ", dest=" + dest + "]";
//    }
//
//}

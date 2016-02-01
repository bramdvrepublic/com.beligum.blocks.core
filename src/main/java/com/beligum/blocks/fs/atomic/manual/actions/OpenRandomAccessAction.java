//package com.beligum.blocks.fs.atomic.actions;
//
//import org.apache.hadoop.fs.FileContext;
//
//import java.io.IOException;
//import java.io.RandomAccessFile;
//
//public class OpenRandomAccessAction extends OpenFileAction
//{
//    private transient RandomAccessFile out;
//
//    public OpenRandomAccessAction(FileContext fileContext, File original)
//    {
//        super(fileContext, original, false);
//    }
//
//    protected Object execute() throws IOException
//    {
//        out = new RandomAccessFile(original, "rw");
//        return out;
//    }
//
//    protected void close() throws IOException
//    {
//        out.close();
//    }
//}

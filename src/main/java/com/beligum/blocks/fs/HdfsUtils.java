package com.beligum.blocks.fs;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bram on 1/19/16.
 */
public class HdfsUtils
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static String readFile(FileSystem fs, Path path) throws IOException
    {
        String retVal = null;

        try (InputStream is = fs.open(path)) {
            IOUtils.toString(is, Charsets.UTF_8);
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.fs.pages;

import org.apache.hadoop.fs.GlobPattern;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import java.io.IOException;

/**
 * Simple version of org.apache.hadoop.fs.GlobFilter but using the full path string instead of just the name
 *
 * Created by bram on 8/31/16.
 */
public class FullPathGlobFilter implements PathFilter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private GlobPattern pattern;

    //-----CONSTRUCTORS-----
    public FullPathGlobFilter(String filePattern) throws IOException
    {
        this.pattern = new GlobPattern(filePattern);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean accept(Path path)
    {
        return this.pattern.matches(path.toString());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.filesystem.pages;

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
    private String pattern;
    private GlobPattern globPattern;

    //-----CONSTRUCTORS-----
    public FullPathGlobFilter(String pattern) throws IOException
    {
        this.pattern = pattern;
        this.globPattern = new GlobPattern(pattern);
    }

    //-----PUBLIC METHODS-----
    public String getPattern()
    {
        return pattern;
    }
    @Override
    public boolean accept(Path path)
    {
        return this.globPattern.matches(path.toString());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

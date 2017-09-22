/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

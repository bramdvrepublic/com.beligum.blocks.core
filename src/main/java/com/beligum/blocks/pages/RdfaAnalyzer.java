package com.beligum.blocks.pages;

import com.beligum.blocks.exceptions.RdfException;
import com.beligum.blocks.utils.RdfTools;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.TextExtractor;

import java.net.URI;
import java.util.HashMap;

/**
 * Created by wouter on 20/06/15.
 */
public class RdfaAnalyzer
{
    /*
   * Add on show page for admin data-index = index of property
   *
   * On save put new value in correct property with correct index value
   *
   * property without data-index is newly added || (could be exitsing not rendered in template)
   *
   * deleted property is property inside previous template but not inside this one
   *
   * */
    public RdfaAnalyzer(String html) {
        // Add data-index attribute to each property
        // fetch objects from db to check attributes

    }

}

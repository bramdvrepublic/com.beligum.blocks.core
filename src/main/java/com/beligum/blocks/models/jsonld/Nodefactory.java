package com.beligum.blocks.models.jsonld;

import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.jsondb.BooleanNode;
import com.beligum.blocks.models.jsonld.jsondb.DoubleNode;
import com.beligum.blocks.models.jsonld.jsondb.LongNode;
import com.beligum.blocks.models.jsonld.jsondb.StringNode;

/**
 * Created by wouter on 23/04/15.
 */
public class Nodefactory
{

    public static Node createAndGuess(String value) {

        return Nodefactory.createAndGuess(value, null);
    }

    public static Node createAndGuess(String value, String language) {
        Node retVal = null;

        if (value.toLowerCase().equals("false") || value.toLowerCase().equals("true")) {
            try {
                retVal = new BooleanNode(Boolean.parseBoolean(value));
            }
            catch (Exception e) {
            }
        }

        if (retVal == null) {
            try {
                retVal = new LongNode(Long.parseLong(value));
            }
            catch (Exception e) {
            }
        }
        if (retVal == null) {
            try {
                retVal = new DoubleNode(Double.parseDouble(value));
            }
            catch (Exception e) {
            }
        }
        if (retVal == null) {
            retVal = new StringNode(value, language);
        }
        return retVal;
    }

}

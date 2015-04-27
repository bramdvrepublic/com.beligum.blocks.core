package com.beligum.blocks.models.jsonld;

import com.beligum.base.utils.Logger;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by wouter on 23/04/15.
 */
public class NodeFactory
{

    public static Node createAndGuess(String value) {

        return NodeFactory.createAndGuess(value, null);
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

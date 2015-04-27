package com.beligum.blocks.models.jsonld;

import com.beligum.base.utils.Logger;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by wouter on 23/04/15.
 */
public class JsonLDContext
{

    HashMap<String, String> prefix = new HashMap<String, String>();
    HashMap<String, String> expanded = new HashMap<String, String>();
    HashMap<String, String> inverseKeys = new HashMap<String, String>();

    JsonNode oldContext = null;

    public JsonLDContext() {

    }

    public JsonLDContext(JsonNode contextNode) {
        oldContext = contextNode;
        Iterator<Map.Entry<String, JsonNode>> iterator = oldContext.fields();
        while(iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                if (!expanded.containsKey(key)) expanded.put(key, expand(key, value.asText()));
                inverseKeys.put(value.asText(), key);
            } else if (value.isObject()) {
                if (value.get("@id").isTextual()) {
                    expand(null, value.get("@id").asText());
                    inverseKeys.put(value.get("@id").asText(), key);
                }
            }
        }
    }


    public String expand(String shortUri, String longUri) {
        String retVal = longUri;
        if (!longUri.startsWith("http://")) {
            if (longUri.contains(":")) {
                String[] paths = longUri.split(":");
                if (this.expanded.containsKey(paths[0])) {
                    retVal = this.expanded.get(paths[0]) + paths[1];
                } else if (this.oldContext.has(paths[0]) && this.oldContext.get(paths[0]).isTextual()) {
                    this.expand(paths[0], this.oldContext.get(paths[0]).asText());
                    retVal = this.expanded.get(paths[0]) + paths[1];
                } else {
                    Logger.debug("Could not expand url:" + longUri);
                }
            }
        }
        if (shortUri != null) this.expanded.put(shortUri, retVal);
        return retVal;
    }

    public void put(String key, String value) {
        this.expanded.put(key, value);
    }

    public String get(String key) {
        return this.expanded.get(key);
    }

    public void write(StringWriter writer, boolean expanded) {
        int count = 0;
        writer.append("{");
        if (this.expanded.values().size() > 0) {
            for (String key : this.expanded.keySet()) {
                if (count > 0) {
                    writer.append(", ");
                }
                writer.append(key).append(": ").append(this.expanded.get(key));
            }
        }
        writer.append("}");
    }

}

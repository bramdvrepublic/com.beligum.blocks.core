package com.beligum.blocks.wiki;

import com.beligum.base.utils.Logger;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wouter on 2/04/15.
 */
public class WikiItem
{
    private final String ID = "name";
    private final String DEFAULT_LANGUAGE = "nl";

    private HashMap<String, HashMap<String, String>> properties = new HashMap<>();
    private String other;
    private boolean valid = true;

    private int fieldType;

    public WikiItem() {

    }

    public boolean isValid() {
        return this.valid && this.hasId();
    }

    public boolean hasId() {
        return this.properties.containsKey(ID) && this.properties.get(ID) != null;
    }


    public String getId()
    {
        if (hasId()) {
            return this.properties.get(ID).get(this.properties.get(ID).keySet().toArray()[0]);
        } else {
            return "NO_ID";
        }
    }

    public void addField(String name, String value)
    {
        ArrayList<String> field = this.cleanFieldName(name);
        String fieldName = field.get(0);
        String fieldLang = field.get(1);
        if (!fieldName.equals("text")) {
            if (!properties.containsKey(fieldName)) properties.put(fieldName, new HashMap<String, String>());
            properties.get(fieldName).put(fieldLang, value);

        } else {
            findFields(value);
        }
    }


    public ArrayList<String> cleanFieldName(String name) {
        ArrayList<String> retVal = new ArrayList<String>();
        String lang = DEFAULT_LANGUAGE;
        if (name.length()> 2) {
            String lang_test = name.substring(name.length() - 2);
            if (lang_test.toLowerCase().equals("fr") || lang_test.toLowerCase().equals("en") || lang_test.toLowerCase().equals("nl")) {
                name = name.substring(0, name.length() - 2);
                lang = lang_test.toLowerCase();
            } else if (name.toLowerCase().contains("_fr")) {
                name = name.replaceAll("Fr", "");
                lang = "fr";
            } else if (name.toLowerCase().contains("_nl")) {
                name = name.replaceAll("Nl", "");
                lang = "nl";
            } else if (name.toLowerCase().contains("_en")) {
                name = name.replaceAll("En", "");
                lang = "en";
            }
        }



        Integer dp = name.indexOf(":");
        if (dp > -1) {
            name = name.substring(0, dp);
        }

        retVal.add(name);
        retVal.add(lang);
        return retVal;
    }

    public void findFields(String text)
    {

        boolean found = true;
        int found_vars = 0;
        Integer start_key = findStartField(text);
        while (start_key > 0) {
            // #search start of key.If not found then all fields are found and exit

            //            #search for text between fields, just to check that we don 't miss unknown important info

            this.other += " " + text.substring(0, start_key);
            text = text.substring(start_key);


            Integer end_key = findEndField(text);


            if (end_key > -1) {
                //            #search end of key.If not found this page is invalid

                String fullField = text.substring(0, end_key);
                if (fullField.contains("_end")) {
                    int x= 0;
                }
                end_key += 2;
                text = text.substring(end_key);

                Integer seperatorIndex = findKeyField(fullField);

                //#search end of value.If not found this page is invalid

                if (seperatorIndex > -1) {
                    String key = fullField.substring(0, seperatorIndex);
                    if (fieldType == 1) {
                        seperatorIndex += 1;
                    } else {
                        seperatorIndex += 2;
                    }
                    String value = fullField.substring(seperatorIndex);
                    this.addField(key, value);
                    found_vars += 1;
                }
                else {
                    Logger.debug("Could not find value for in this field " + fullField);
                }
            }
            else {
                this.valid = false;
                Logger.debug("Could not find the end of the field!");
            }

            start_key = findStartField(text);
        }

    }



    public Integer findStartField(String text)
    {
        Integer retVal = -1;
        Integer val1 = text.indexOf("(:");
        Integer val2 = text.indexOf("[[#");

        if (val2 < val1 && val2 > -1) {
            fieldType = 2;
            retVal = val2 + 3;
        } else if (val1 > -1) {
            fieldType = 1;
            retVal = val1 + 2;
        }

        return retVal;
    }

    public Integer findKeyField(String text) {
        if (fieldType == 1) {
            return text.indexOf(":");
        } else {
            return text.indexOf("]]");
        }
    }


    public Integer findEndField(String text)
    {
        if (fieldType == 1) {
            return text.indexOf(":)");
        } else {
            Integer val2 = text.indexOf("[[#");
            Integer val3 = text.indexOf("[[#een");

            if (val2.equals(val3)) {
                val2 = text.indexOf("[[#", val2 + 1);
            }
            return val2;
        }
    }

    public HashMap<String, HashMap<String, String>> addToData(HashMap<String, HashMap<String, String>> stored)
    {
        for (String key: this.properties.keySet()) {
            if (!stored.containsKey(key))
                stored.put(key, new HashMap<String, String>());

            for (String lang : this.properties.get(key).keySet()) {
                if (lang != null) {
                    if (stored.get(key).containsKey(lang)) {
                        Logger.warn("Overwriting field for language " + lang + " in item " + this.getId().toString());
                    }
                    stored.get(key).put(lang, this.cleanData(this.properties.get(key).get(lang)));
                }
            }
        }

        return stored;
    }
    //
    public String cleanData(String data)
    {
        data = StringUtils.chomp(data);
        data = data.trim();
        data = data.replace("%0a", "\n");
        return data;
    }


    public HashMap<String, HashMap<String, String>> getFields()
    {
        return properties;
    }
    public void setFields(HashMap<String, HashMap<String, String>> fields)
    {
        this.properties = fields;
    }
    public String getOther()
    {
        return other;
    }
    public void setOther(String other)
    {
        this.other = other;
    }
    public void setValid(boolean valid)
    {
        this.valid = valid;
    }
}

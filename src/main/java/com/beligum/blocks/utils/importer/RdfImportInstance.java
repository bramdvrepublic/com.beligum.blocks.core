package com.beligum.blocks.utils.importer;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by bram on 3/22/16.
 */
public class RdfImportInstance
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfClass rdfClass;
    private Map<RdfClassProperty, RdfPropertyValue> properties;

    //-----CONSTRUCTORS-----
    public RdfImportInstance(RdfClass rdfClass)
    {
        this.rdfClass = rdfClass;
        //TreeMap is a map sorted by its keys.
        //The comparator is used to sort the TreeMap by keys.
        this.properties = new TreeMap<>(new RdfClassProperty.MapComparator());
    }

    //-----PUBLIC STATIC METHODS-----

    //-----PUBLIC METHODS-----
    public RdfClass getRdfClass()
    {
        return rdfClass;
    }
    public void addProperty(RdfClassProperty property, RdfPropertyValue value)
    {
        this.properties.put(property, value);
    }
    public RdfPropertyValue getValue(RdfClassProperty property)
    {
        return this.properties.get(property);
    }
    public RdfPropertyValue findValue(RdfProperty rdfProperty)
    {
        for (Map.Entry<RdfClassProperty, RdfPropertyValue> e : this.properties.entrySet()) {
            if (e.getKey().getRdfProperty().equals(rdfProperty)) {
                return e.getValue();
            }
        }

        return null;
    }
    public Set<Map.Entry<RdfClassProperty, RdfPropertyValue>> getProperties()
    {
        return this.properties.entrySet();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    public interface FieldGenerator
    {
        String generateId(RdfImportInstance instance);
        String generateId(RdfImportInstance instance, Set<String> importedIds, String previousId);
        String generateTitle(RdfImportInstance instance);
    }

    public static class TitleProperty
    {
        private RdfProperty rdfProperty;
        private boolean prependComma;

        public TitleProperty(RdfProperty rdfProperty, boolean prependComma)
        {
            this.rdfProperty = rdfProperty;
            this.prependComma = prependComma;
        }

        public RdfProperty getRdfProperty()
        {
            return rdfProperty;
        }
        public boolean isPrependComma()
        {
            return prependComma;
        }
    }

    public static abstract class AbstractFieldGenerator implements FieldGenerator
    {
        protected List<RdfProperty> idFields;
        protected List<TitleProperty> titleFields;
        public AbstractFieldGenerator(List<RdfProperty> idFields, List<TitleProperty> titleFields)
        {
            this.idFields = idFields;
            this.titleFields = titleFields;
        }

        @Override
        public String generateId(RdfImportInstance instance)
        {
            StringBuilder idBuilder = new StringBuilder();

            for (RdfProperty property : this.idFields) {
                RdfPropertyValue p = instance.findValue(property);
                if (p != null) {
                    String val = p.getValue();
                    if (!StringUtils.isEmpty(val)) {
                        if (idBuilder.length() != 0) {
                            idBuilder.append("-");
                        }

                        val = this.preparseCity(property, val);

                        idBuilder.append(val);
                    }
                }
            }

            if (idBuilder.length() > 0) {
                String retVal = StringFunctions.prepareSeoValue(idBuilder.toString());
                retVal = StringUtils.strip(retVal, "-");

                return retVal;
            }
            else {
                //signals we didn't find a real ID based on the field components
                return null;
            }
        }
        @Override
        public String generateId(RdfImportInstance instance, Set<String> importedIds, String previousId)
        {
            String retVal = this.generateId(instance);

            if (retVal!=null) {
                retVal = this.checkDoubleIds(retVal, importedIds, previousId);
            }

            return retVal;
        }
        @Override
        public String generateTitle(RdfImportInstance instance)
        {
            StringBuilder idBuilder = new StringBuilder();

            for (TitleProperty field : this.titleFields) {
                RdfPropertyValue p = instance.findValue(field.getRdfProperty());
                if (p != null) {
                    String val = p.getValue();
                    if (!StringUtils.isEmpty(val)) {
                        if (idBuilder.length() != 0) {
                            if (field.isPrependComma()) {
                                idBuilder.append(",");
                            }
                            idBuilder.append(" ");
                        }

                        val = this.preparseCity(field.getRdfProperty(), val);

                        idBuilder.append(val);
                    }
                }
            }

            if (idBuilder.length() > 0) {
                String retVal = idBuilder.toString();
                retVal = StringUtils.strip(retVal, " ,");
                return retVal;
            }
            else {
                return null;
            }
        }

        private String preparseCity(RdfProperty property, String val)
        {
            //we format the city value as eg; Bredene,8450,Belgium, but we want it to generate 'Bredene Belgium'
            if (property.equals(com.beligum.blocks.rdf.ontology.factories.Terms.city)) {

                String city = val;
                String country = null;
                if (val.matches("[^,]*,\\d+,.*")) {
                    city = val.replaceAll("([^,]*),(\\d+),(.*)", "$1");
                    country = val.replaceAll("([^,]*),(\\d+),(.*)", "$3");
                }
                else if (val.matches("[^,]*,.*")) {
                    city = val.replaceAll("([^,]*),(.*)", "$1,$2");
                    country = val.replaceAll("([^,]*),(.*)", "$1,$2");
                }

                val = city;
                if (!StringUtils.isEmpty(country)) {
                    val += ", " + country;
                }
            }

            return val;
        }

        private String checkDoubleIds(String importIdBase, Set<String> importedIds, String previousId)
        {
            String retVal = importIdBase;
            if (importedIds.contains(retVal)) {
                //this will try to unify titles across all languages
                if (previousId==null) {
                    //since it's a double, we start at 2
                    int counter = 2;
                    while (importedIds.contains(retVal)) {
                        retVal = importIdBase + "-" + (counter++);
                    }
                }
                else {
                    retVal = previousId;
                }
            }

            if (!importedIds.contains(retVal)) {
                importedIds.add(retVal);
            }

            return retVal;
        }
    }

    public static class PersonFieldGenerator extends AbstractFieldGenerator
    {
        public PersonFieldGenerator()
        {
            super(null, null);
        }

        @Override
        public String generateId(RdfImportInstance instance)
        {
            return StringFunctions.prepareSeoValue(this.buildTitle(instance, false, false));
        }
        @Override
        public String generateTitle(RdfImportInstance instance)
        {
            //we don't include the email anymore...
            return this.buildTitle(instance, false, false);
        }

        private String buildTitle(RdfImportInstance instance, boolean addEmail, boolean emailBraces)
        {
            String retVal = null;

            RdfPropertyValue name = instance.findValue(Terms.name);
            if (name != null && !StringUtils.isEmpty(name.getValue())) {
                retVal = name.getValue();
            }
            //if we don't have a full name, build the name from the parts
            else {
                RdfPropertyValue givenName = instance.findValue(Terms.givenName);
                if (givenName != null && !StringUtils.isEmpty(givenName.getValue())) {
                    retVal = givenName.getValue();
                }
                RdfPropertyValue familyName = instance.findValue(Terms.familyName);
                if (familyName != null && !StringUtils.isEmpty(familyName.getValue())) {
                    retVal = familyName.getValue();
                }

                //if we still didn't get anything, try the username as name
//                if (retVal == null) {
//                    RdfPropertyValue username = instance.findValue(com.beligum.mot.commons.rdf.Terms.username);
//                    if (username != null && !StringUtils.isEmpty(username.getValue())) {
//                        retVal = username.getValue();
//                    }
//                }
            }

            //little heuristic parsing: instead of "florence.vandriel", it's better to return "Florence Vandriel"
            if (retVal != null) {
                retVal = prettyfyUsername(retVal);
            }

            //we allow addEmail to be overrided when nothing was found by now; last try with the email address
            if (addEmail || retVal == null) {
                RdfPropertyValue email = instance.findValue(Terms.email);
                if (email != null && !StringUtils.isEmpty(email.getValue())) {
                    String value = email.getValue();
                    //if we ended up here because retVal was still null, strip off the domain from the email and hope the address part makes sense
                    if (retVal == null && value.contains("@")) {
                        value = prettyfyUsername(value.substring(0, value.indexOf("@")));
                    }

                    if (retVal != null) {
                        if (emailBraces) {
                            retVal += " <" + value + ">";
                        }
                        else {
                            retVal += " " + value;
                        }
                    }
                    else {
                        retVal = value;
                    }
                }
            }

            //additional heuristic: if really nothing was found, resort to the street name (eg. for "Domein Bokrijk") and city
            if (retVal == null) {
                RdfPropertyValue streetName = instance.findValue(Terms.streetName);
                if (streetName != null && !StringUtils.isEmpty(streetName.getValue())) {
                    retVal = streetName.getValue();
                }
                RdfPropertyValue city = instance.findValue(Terms.city);
                if (city != null && !StringUtils.isEmpty(city.getValue())) {
                    if (retVal != null) {
                        retVal += ", " + city.getValue();
                    }
                    else {
                        retVal = city.getValue();
                    }
                }
            }

            return retVal;
        }

        public static String prettyfyUsername(String value)
        {
            //if we encounter a dot in the middle of two words; activate the optimization
            if (value.matches(".*.\\...*")) {
                StringBuilder builder = new StringBuilder();
                boolean capitalizeNextChar = false;
                for (char c : value.toCharArray()) {
                    if (c == '.') {
                        builder.append(" ");
                        capitalizeNextChar = true;
                        continue;
                    }
                    if (capitalizeNextChar) {
                        builder.append(Character.toUpperCase(c));
                    }
                    else {
                        builder.append(c);
                    }
                    capitalizeNextChar = false;
                }

                value = builder.toString();
            }

            value = StringUtils.capitalize(value);

            return value;
        }
    }

    public static class TitleGenerator extends AbstractFieldGenerator
    {
        public TitleGenerator()
        {
            super(Lists.newArrayList(
                            Terms.title
            ),
                  Lists.newArrayList(
                                  new TitleProperty(Terms.title, true)
                  ));
        }
    }
}

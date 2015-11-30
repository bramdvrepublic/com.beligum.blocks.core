package com.beligum.blocks.search.fields;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 9/09/15.
 */
public class JoinField extends CustomField
{

    protected List<Field> joinedFields = new ArrayList<>();

    public JoinField(URI field)
    {
        super(field);
    }

    public JoinField(URI field, Locale locale)
    {
        super(field, locale);
    }

    public JoinField join(URI uri)
    {
        return join(uri, Locale.ROOT);
    }

    public JoinField join(URI uri, Locale locale)
    {
        joinedFields.add(new CustomField(uri, locale));
        return this;
    }

    public Field get(URI uri)
    {
        return get(uri, Locale.ROOT);
    }

    public Field get(URI uri, Locale locale)
    {
        joinedFields.add(new CustomField(uri, locale));
        return this;
    }

    public Field id()
    {
        IdField retVal = new IdField();
        joinedFields.add(retVal);
        return this;
    }

    public Field type()
    {
        TypeField retVal = new TypeField();
        joinedFields.add(retVal);
        return this;
    }

    @Override
    public String getField()
    {
        int counter = 1;
        StringBuilder localizedField = new StringBuilder();
        List<Field> tempJoinedFields = new ArrayList();
        tempJoinedFields.add(this);
        tempJoinedFields.addAll(this.joinedFields);
        int total = tempJoinedFields.size();
        for (Field f : tempJoinedFields) {
            if (counter < total) {
                localizedField.append(getRootFieldName()).append(".");
            }
            else if (f == this) {
                // This is the last field in the join (probably no join, so print teh full value)
                localizedField.append(super.getField());
            }
            else {
                localizedField.append(f.getField());
            }
            counter++;
        }

        return localizedField.toString();

    }

    @Override
    public String getRawField()
    {
        int counter = 1;
        StringBuilder localizedField = new StringBuilder();
        List<Field> tempJoinedFields = new ArrayList();
        tempJoinedFields.add(this);
        tempJoinedFields.addAll(this.joinedFields);
        int total = tempJoinedFields.size();
        // We loop through all the joined fields, starting with ourself
        // The last field gets a special treatment
        // Fields.get("http://schema.org/address").join("http://schema.org/street")
        // Should give us schema_org_address._schema_org_street.@value
        for (Field f : tempJoinedFields) {
            if (counter < total) {
                localizedField.append(getRootFieldName()).append(".");
            }
            else if (f == this) {
                // This is the last field in the join (probably no join at all, so print the full value)
                localizedField.append(super.getRawField());
            }
            else {
                localizedField.append(f.getRawField());
            }
            counter++;
        }

        return localizedField.toString();

    }

}

package com.beligum.blocks.models.sql;

/**
 * Created by wouter on 19/05/15.
 */
public class Property
{
    public static final Integer INT = 0;
    public static final Integer LONG = 1;
    public static final Integer DOUBLE = 2;
    public static final Integer STRING = 3;  // nvarchar(512)
    public static final Integer TEXT = 4;  // TEXT
    public static final Integer BOOLEAN = 5;
    public static final Integer LIST = 6;
    public static final Integer RESOURCE_LIST = 7;
    public static final Integer RESOURCE = 8;

    private String sqlName;
    private Integer sqlType;
    private Integer sqlSubType;
    private boolean localized;


    public String getSqlName()
    {
        return sqlName;
    }
    public void setSqlName(String sqlName)
    {
        this.sqlName = sqlName;
    }
    public boolean isLocalized()
    {
        return localized;
    }
    public void setLocalized(boolean localized)
    {
        this.localized = localized;
    }
    public Integer getSqlType()
    {
        return sqlType;
    }
    public void setSqlType(Integer sqlType)
    {
        this.sqlType = sqlType;
    }
    public Integer getSqlSubType()
    {
        return sqlSubType;
    }
    public void setSqlSubType(Integer sqlSubType)
    {
        this.sqlSubType = sqlSubType;
    }
}

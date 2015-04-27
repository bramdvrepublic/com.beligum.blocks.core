package com.beligum.blocks.models.jsonld.hibernate;

import org.hibernate.dialect.PostgreSQL9Dialect;

import java.sql.Types;

/**
 * Created by wouter on 26/04/15.
 */
public class JsonPostgreSQLDialect extends PostgreSQL9Dialect
{

    public JsonPostgreSQLDialect() {

        super();

        this.registerColumnType(Types.JAVA_OBJECT, "jsonb");
    }
}
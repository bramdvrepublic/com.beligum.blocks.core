package com.beligum.blocks.models.jsonld.hibernate;

import com.beligum.blocks.models.jsonld.Resource;
import com.beligum.blocks.models.jsonld.ResourceImpl;
import com.beligum.blocks.models.jsonld.jackson.ResourceDeserializer;
import com.beligum.blocks.models.jsonld.jackson.ResourceSerializer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * Created by wouter on 27/04/15.
 */
public class ResourceUserType implements UserType, ParameterizedType
{

    private ObjectMapper getMapper() {
        if (ResourceUserType.MAPPER == null) {
            final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
            module.addSerializer(Resource.class, new ResourceSerializer());
            module.addDeserializer(Resource.class, new ResourceDeserializer());

            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(module);
            ResourceUserType.MAPPER = objectMapper;
        }
        return ResourceUserType.MAPPER;
    }

    public static final String CLASS = "ResourceUserType";
    public static final String JSONB_TYPE = "jsonb";
    private static  ObjectMapper MAPPER = null;
    private Class returnedClass;



    /**
     * Return the SQL type codes for the columns mapped by this type. The
     * codes are defined on <tt>java.sql.Types</tt>.
     *
     * @return int[] the typecodes
     * @see java.sql.Types
     */
    @Override
    public int[] sqlTypes() {

        return new int[] { Types.JAVA_OBJECT};
    }

    /**
     * The class returned by <tt>nullSafeGet()</tt>.
     *
     * @return Class
     */
    @Override
    public Class returnedClass() {
        return Resource.class;
    }

    /**
     * Compare two instances of the class mapped by this type for persistence "equality".
     * Equality of the persistent state.
     *
     * @param x
     * @param y
     * @return boolean
     */
    @Override
    public boolean equals(Object x, Object y) throws HibernateException
    {

        if( x== null){

            return y== null;
        }

        return x.equals( y);
    }

    /**
     * Get a hashcode for the instance, consistent with persistence "equality"
     */
    @Override
    public int hashCode(Object x) throws HibernateException {

        return x.hashCode();
    }

    /**
     * Retrieve an instance of the mapped class from a JDBC resultset. Implementors
     * should handle possibility of null values.
     *
     * @param rs      a JDBC result set
     * @param names   the column names
     * @param session
     * @param owner   the containing entity  @return Object
     * @throws org.hibernate.HibernateException
     *
     * @throws java.sql.SQLException
     */
    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException
    {
        if(rs.getString(names[0]) == null){
            return null;
        }

        Resource json = null;
        try {
            json = getMapper().readValue(rs.getBytes(names[0]), Resource.class);
        } catch (Exception e) {
            throw new HibernateException(e);
        }

        return json;
    }

    /**
     * Write an instance of the mapped class to a prepared statement. Implementors
     * should handle possibility of null values. A multi-column type should be written
     * to parameters starting from <tt>index</tt>.
     *
     * @param st      a JDBC prepared statement
     * @param value   the object to write
     * @param index   statement parameter index
     * @param session
     * @throws org.hibernate.HibernateException
     *
     * @throws java.sql.SQLException
     */
    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        String sqlJson = null;
        try {
            sqlJson = getMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new HibernateException(e);
        }
        PGobject pgo = new PGobject();
        pgo.setType(JSONB_TYPE);
        pgo.setValue(sqlJson);
        st.setObject(index, pgo);

        st.setObject(index, sqlJson, Types.OTHER);
    }

    /**
     * Return a deep copy of the persistent state, stopping at entities and at
     * collections. It is not necessary to copy immutable objects, or null
     * values, in which case it is safe to simply return the argument.
     *
     * @param value the object to be cloned, which may be null
     * @return Object a copy
     */
    @Override
    public Object deepCopy(Object value) throws HibernateException {
        Resource retVal = null;
        Resource resource = (Resource)value;
        if (resource != null) {
            retVal = resource.copy();
        }
        return retVal;
    }

    /**
     * Are objects of this type mutable?
     *
     * @return boolean
     */
    @Override
    public boolean isMutable() {
        return true;
    }

    /**
     * Transform the object into its cacheable representation. At the very least this
     * method should perform a deep copy if the type is mutable. That may not be enough
     * for some implementations, however; for example, associations must be cached as
     * identifier values. (optional operation)
     *
     * @param value the object to be cached
     * @return a cachable representation of the object
     * @throws org.hibernate.HibernateException
     *
     */
    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (String)this.deepCopy( value);
    }

    /**
     * Reconstruct an object from the cacheable representation. At the very least this
     * method should perform a deep copy if the type is mutable. (optional operation)
     *
     * @param cached the object to be cached
     * @param owner  the owner of the cached object
     * @return a reconstructed object from the cachable representation
     * @throws org.hibernate.HibernateException
     *
     */
    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return this.deepCopy( cached);
    }

    /**
     * During merge, replace the existing (target) value in the entity we are merging to
     * with a new (original) value from the detached entity we are merging. For immutable
     * objects, or null values, it is safe to simply return the first parameter. For
     * mutable objects, it is safe to return a copy of the first parameter. For objects
     * with component values, it might make sense to recursively replace component values.
     *
     * @param original the value from the detached entity being merged
     * @param target   the value in the managed entity
     * @return the value to be merged
     */
    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        final String clazz = (String) parameters.get(CLASS);
        try {
            returnedClass = ReflectHelper.classForName(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class: " + clazz
                                               + " is not a known class type.");
        }
    }
}


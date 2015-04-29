package com.beligum.blocks.utils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 10/13/14.
 */
public class Utils
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static void autowireDaoToModel(Object dao, Object model) throws Exception
    {
        Field[] fields = dao.getClass().getFields();
        for (Field daoField : fields) {
            if ((daoField.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC) {
                Field modelField = null;
                try {
                    modelField = model.getClass().getField(daoField.getName());
                }
                catch (Exception e) {
                }
                if (modelField != null && (modelField.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC) {
                    modelField.set(model, daoField.get(dao));
                }
                //give it another shot with the getter
                else {
                    if (PropertyUtils.isWriteable(model, daoField.getName())) {
                        PropertyUtils.setProperty(model, daoField.getName(), daoField.get(dao));
                    }
                }
            }
        }

        PropertyDescriptor[] daoProperties = PropertyUtils.getPropertyDescriptors(dao);
        for (PropertyDescriptor daoProperty : daoProperties) {
            //first, try to find a public field in the model
            Field modelField = null;
            try {
                modelField = model.getClass().getField(daoProperty.getName());
            }
            catch (Exception e) {
            }
            if (modelField != null && (modelField.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC) {
                modelField.set(model, PropertyUtils.getProperty(dao, daoProperty.getName()));
            }
            //give it another shot with the getter
            else {
                if (PropertyUtils.isWriteable(model, daoProperty.getName())) {
                    PropertyUtils.setProperty(model, daoProperty.getName(), PropertyUtils.getProperty(dao, daoProperty.getName()));
                }
            }
        }
    }

    /**
     * @param obj
     * @return a map of all accessible field names mapped to the toString() values of its fields
     */
    public static Map<String, String> toHash(Object obj)
    {
        Map<String, String> hash = new HashMap<>();
        Field[] fields = getAllFields(obj);
        for (Field field : fields) {
            try {
                Object fieldValue = field.get(obj);
                hash.put(field.getName(), fieldValue.toString());
            }
            catch (Exception e) {
                try {
                    Object fieldValue = PropertyUtils.getProperty(obj, field.getName());
                    hash.put(field.getName(), fieldValue.toString());
                }
                catch (Exception ex) {
                    //the field cannot be added to the hash, since it is not accessible, do nothing
                }
            }
        }
        return hash;
    }

    /**
     * Method wiring all string value's corresponding to field names of a model object into the fields of that model.
     * Only accessible fields which can be written to will be changed.
     *
     * @param hash  map of pairs (fieldName -> fieldValue)
     * @param model
     */
    public static void autowireDaoToModel(Map<String, String> hash, Object model)
    {
        Field[] fields = getAllFields(model);
        Map<String, Field> fieldsMap = new HashMap<>();
        for (Field field : fields) {
            fieldsMap.put(field.getName(), field);
        }
        for (String key : hash.keySet()) {
            if (fieldsMap.containsKey(key)) {
                Field field = fieldsMap.get(key);
                try {
                    field.set(model, field.get(model));
                }
                catch (Exception e) {
                    try {
                        String fieldValueString = hash.get(field.getName());
                        Object fieldValue = null;
                        Class type = field.getType();
                        if (type.isPrimitive()) {
                            if (type == Boolean.TYPE)
                                fieldValue = Boolean.parseBoolean(fieldValueString);
                            else if (type == Byte.TYPE)
                                Byte.parseByte(fieldValueString);
                            else if (type == Short.TYPE)
                                fieldValue = Short.parseShort(fieldValueString);
                            else if (type == Integer.TYPE)
                                fieldValue = Integer.parseInt(fieldValueString);
                            else if (type == Long.TYPE)
                                fieldValue = Long.parseLong(fieldValueString);
                            else if (type == Float.TYPE)
                                fieldValue = Float.parseFloat(fieldValueString);
                            else if (type == Double.TYPE)
                                fieldValue = Double.parseDouble(fieldValueString);
                            else
                                throw new Exception("Unknown primitive type?");
                        }
                        else {
                            fieldValue = type.getConstructor(String.class).newInstance(fieldValueString);
                        }
                        PropertyUtils.setProperty(model, field.getName(), fieldValue);
                    }
                    catch (Exception ex) {
                        //no getter or setter found for the property, so cannot wire it
                    }
                }
            }
        }
    }

    public static Field[] getAllFields(Object obj)
    {
        Field[] fields = obj.getClass().getDeclaredFields();
        Class superClass = obj.getClass().getSuperclass();
        while (!superClass.equals(Object.class)) {
            Field[] superFields = superClass.getDeclaredFields();
            fields = ArrayUtils.addAll(fields, superFields);
            superClass = superClass.getSuperclass();
        }
        return fields;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

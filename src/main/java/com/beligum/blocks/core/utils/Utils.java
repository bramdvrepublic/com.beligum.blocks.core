package com.beligum.blocks.core.utils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
     *
     * @param obj
     * @return a map of all accessible field names mapped to the toString() values of its fields
     */
    public static Map<String, String> toHash(Object obj){
        Map<String, String> hash = new HashMap<>();
        Field[] fields = obj.getClass().getDeclaredFields();
        Class superClass = obj.getClass().getSuperclass();
        while(!superClass.equals(Object.class)){
            Field[] superFields = superClass.getDeclaredFields();
            fields = ArrayUtils.addAll(fields, superFields);
            superClass = superClass.getSuperclass();
        }
        for(Field field : fields) {
            try {
                Object fieldValue = field.get(obj);
                hash.put(field.getName(), fieldValue.toString());
            }
            catch(Exception e) {
                try {
                    Object fieldValue = PropertyUtils.getProperty(obj, field.getName());
                    hash.put(field.getName(), fieldValue.toString());
                }
                catch (Exception ex){
                    //the field cannot be added to the hash, since it is not accessible, do nothing
                }
            }
        }
        return hash;
    }



    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

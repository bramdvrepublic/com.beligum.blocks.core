package com.beligum.blocks.core.utils;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

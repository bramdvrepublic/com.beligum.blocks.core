package com.beligum.blocks.fs.index;

import com.beligum.base.utils.toolkit.ReflectionFunctions;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;

import java.util.*;

/**
 * Created by bram on 2/13/16.
 */
public class LuceneSearchConfiguration extends SearchConfigurationBase
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Properties properties;
    private Map<String, Class<?>> entities = new HashMap<>();
    private ReflectionManager reflectionManager;
    private ClassLoaderService classLoaderService;

    //-----CONSTRUCTORS-----
    public LuceneSearchConfiguration()
    {
        this.properties = new Properties();
        //this.properties.setProperty("hibernate.search.default.directory_provider", "filesystem");
        //this.properties.setProperty("hibernate.search.default.indexBase", Settings.instance().getPageMainIndexFolder().getAbsolutePath());

        // hibernate search initializes the directory while creating documents; since we use raw Lucene (but Hibernate to do the class->document mapping),
        // we don't want hibernate to create any directories for us, so use the ram-only provider
        this.properties.setProperty("hibernate.search.default.directory_provider", "ram");

        //TODO maybe sync these two?
        Set<Class<?>> indexedClasses = ReflectionFunctions.searchAllAnnotatedClasses(Indexed.class);
        for (Class<?> c : indexedClasses) {
            this.entities.put(c.getCanonicalName(), c);
        }
        this.reflectionManager = new JavaReflectionManager();

        //this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
        this.classLoaderService = new DefaultClassLoaderService();
    }

    //-----PUBLIC METHODS-----
    @Override
    public Iterator<Class<?>> getClassMappings()
    {
        return entities.values().iterator();
    }
    @Override
    public Class<?> getClassMapping(String name)
    {
        return entities.get(name);
    }
    @Override
    public String getProperty(String propertyName)
    {
        return this.properties.getProperty(propertyName);
    }
    @Override
    public Properties getProperties()
    {
        return this.properties;
    }
    @Override
    public ReflectionManager getReflectionManager()
    {
        return this.reflectionManager;
    }
    @Override
    public SearchMapping getProgrammaticMapping()
    {
        return null;
    }
    @Override
    public Map<Class<? extends Service>, Object> getProvidedServices()
    {
        return Collections.emptyMap();
    }
    @Override
    public ClassLoaderService getClassLoaderService()
    {
        return this.classLoaderService;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

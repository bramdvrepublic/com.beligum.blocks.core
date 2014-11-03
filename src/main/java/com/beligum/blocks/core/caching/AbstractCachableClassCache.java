package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.models.ifaces.CachableClass;
import com.beligum.blocks.core.parsing.CachableClassParser;
import com.beligum.core.framework.utils.toolkit.FileFunctions;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
* Created by bas on 03.11.14.
* Super class for BlockClasCache and PageClassCache
*/
public abstract class AbstractCachableClassCache<T extends CachableClass>
{
    /**
     * protected constructor for singleton-use of extending classes
     */
    protected AbstractCachableClassCache(){

    }

    /**
     * This method returns a map with all present Cachables (value) by name (key)
     * @returns a map of all the currently cached Cachables from the application cache
     */
    abstract public Map<String,T> getCache();

    /**
     * Get the page-class with a certain name from the application cache
     * @param cachableClassName the name of the page-class to get
     * @return a Cachable-object from the application cache
     */
    public T get(String cachableClassName){
        return this.getCache().get(cachableClassName);
    }

    /**
     *
     * @param cachableClass the page-class to be added to the applications page-cache, the key will be the page-class-name
     */
    public void add(T cachableClass) throws CacheException
    {
        if(!getCache().containsKey(cachableClass.getName())) {
            getCache().put(cachableClass.getName(), cachableClass);
        }
        else{
            throw new CacheException("Cannot add cachable-class '" + cachableClass.getName() + "' to cache, since it is already present.");
        }
    }

    /**
     * Fill up the page-cache with all page-class-templates found in file-system
     * @return A full AbstractCachableClassCache
     * @throws com.beligum.blocks.core.exceptions.CacheException
     */
    protected AbstractCachableClassCache fillCache() throws CacheException
    {
        try {
            URI classesRootFolderUri = FileFunctions.searchClasspath(this.getClass(),this.getClassRootFolder());
            //we don't need the 'file://'-part of the returned URI, so we use 'getSchemeSpecificPart()'
            Path classesRootFolder = Paths.get(classesRootFolderUri.getSchemeSpecificPart());
            //look for all subfolders in the pagesFolder, using a filter checking if a child of the folder is a directory or not
            DirectoryStream<Path> classFolders = Files.newDirectoryStream(classesRootFolder,
                                                                              new DirectoryStream.Filter<Path>(){
                                                                                  @Override
                                                                                  public boolean accept(Path file) throws IOException {
                                                                                      return (Files.isDirectory(file));
                                                                                  }
                                                                              });
            for(Path classFolder : classFolders){
                String className = classFolder.getFileName().toString();
                add(className);
            }
            return this;
        }
        catch(IOException e){
            throw new CacheException("Problem while reading page-classes from directory '" + BlocksConfig.getPagesFolder() + "'.", e);
        }
    }

    /**
     * Add a template page (starting from the pageclass-name) to the page-cache
     * @param cachableClassName the page-class-name (f.i. "default" for a pageClass filtered from the file "pages/default/index.html") of the page-class-template to be parsed and added to the cache as a couple (pageClassName, pageClass)
     */
    private void  add(String cachableClassName) throws CacheException
    {
        try {
            /*
             * Get the default rows and blocks out of the template and write them to the application cache
             * We get the local file representing the template, using the files.template-path in the configuration-xml-file of the server
             */
            CachableClassParser<T> parser = this.getParser();
            T cachableClass = parser.parseCachableClass(cachableClassName);

            /*
             * Put the filled page in the cache
             */
            this.add(cachableClass);
        }catch(Exception e){
            throw new CacheException("Could not add cachable-class '" + cachableClassName + "' to the cache.", e);
        }
    }

    abstract protected String getClassRootFolder();

    abstract protected CachableClassParser<T> getParser();

}

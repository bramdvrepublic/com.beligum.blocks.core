package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.models.IdentifiableObject;
import com.beligum.blocks.core.parsers.AbstractParser;
import com.beligum.core.framework.utils.Logger;
import com.beligum.core.framework.utils.toolkit.FileFunctions;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
* Created by bas on 03.11.14.
*/
public abstract class AbstractIdentifiableObjectCache<T extends IdentifiableObject>
{
    /** boolean telling us whether or not one of the inheriting classes is already running through the template-html-files*/
    private static boolean runningTroughHtmlTemplates = false;

    /**
     * protected constructor for singleton-use of extending classes
     */
    protected AbstractIdentifiableObjectCache(){

    }

    /**
     * This method returns a map with all present Cachables (value) by name (key)
     * @returns a map of all the currently cached Cachables from the application cache
     */
    abstract public Map<String, T> getCache();

    /**
     * Get the identifiable-object with a certain name from the application cache
     * @param name the unique name of the identifiable object to get
     * @return a identifiable-object from the application cache
     */
    public T get(String name) throws CacheException
    {
        return this.getCache().get(name);
    }

    /**
     *
     * @param identifiableObject the identifiable object to be added to the applications cache, the key will be the object's id
     */
    public void add(T identifiableObject) throws CacheException
    {
        if(!getCache().containsKey(identifiableObject.getId().toString())) {
            getCache().put(identifiableObject.getId().toString(), identifiableObject);
        }
        else{
            throw new CacheException("Cannot add identifiable object '" + identifiableObject.getId() + "' to cache, since it is already present.");
        }
    }

    /**
     * Fill up the page-cache with all identifiable object-templates found in file-system
     * @return A full AbstractCachableClassCache
     * @throws com.beligum.blocks.core.exceptions.CacheException
     */
    protected void fillCache() throws CacheException
    {
        if(!runningTroughHtmlTemplates) {
            runningTroughHtmlTemplates = true;
            URI rootFolderUri = FileFunctions.searchClasspath(this.getClass(), BlocksConfig.getTemplateFolder());
            Path rootFolder = Paths.get(rootFolderUri.getSchemeSpecificPart());

            try {
                Files.walkFileTree(rootFolder, new SimpleFileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                                    throws IOException
                    {
                        if (filePath.getFileName().toString().endsWith("html")) {
                            AbstractParser.cacheEntity(new URL(BlocksConfig.getSiteDomain()), new String(Files.readAllBytes(filePath)));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch (Exception e) {
                Logger.error("Error while filling cache: " + this, e);
            }
            finally {
                runningTroughHtmlTemplates = false;
            }
        }

    }

//
//    /**
//     * Add a template page (starting from the pageclass-name) to the page-cache
//     * @param viewableClassName the identifiable object-name (f.i. "default" for a pageClass filtered from the file "entities/default/index.html") of the identifiable object-template to be parsed and added to the cache as a couple (pageClassName, pageClass)
//     */
//    private void  add(String viewableClassName) throws CacheException
//    {
//        try {
//            /*
//             * Get the default rows and blocks out of the template and write them to the application cache
//             * We get the local file representing the template, using the files.template-path in the configuration-xml-file of the server
//             */
//            AbstractViewableParser<T> parser = this.getParser(viewableClassName);
//            T viewableClass = parser.parseViewableClass();
//
//            /*
//             * Put the filled page in the cache
//             */
//            this.add(viewableClass);
//        }catch(Exception e){
//            throw new CacheException("Could not add viewable-class '" + viewableClassName + "' to the " + this.getParser(viewableClassName).getViewableCssClass() +  "-cache.", e);
//        }
//    }
//
//    /**
//     *
//     * @param pageTemplateName
//     * @return a valid ID-object constructed from the (unique) name of this identifiable object
//     * @throws CacheException if no valid ID can be constructed from the specified name
//     */
//    abstract public ID getNewId(String pageTemplateName) throws CacheException;

//
//    /**
//     *
//     * @param viewableClassName the name of the viewable-class to get a parser for
//     * @return a parser for parsing the viewable with a certain viewable-class name
//     */
//    abstract protected AbstractViewableParser<T> getParser(String viewableClassName);

}

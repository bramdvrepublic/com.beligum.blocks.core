package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.models.classes.AbstractViewableClass;
import com.beligum.blocks.html.parsers.AbstractParser;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
* Created by bas on 03.11.14.
* Super class for BlockClasCache and PageClassCache
*/
public abstract class AbstractViewableClassCache<T extends AbstractViewableClass>
{
    /**
     * protected constructor for singleton-use of extending classes
     */
    protected AbstractViewableClassCache(){

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
    protected void fillCache() throws CacheException
    {
//        try {
//            URI classesRootFolderUri = FileFunctions.searchClasspath(this.getClass(),this.getClassRootFolder());
//            Path classesRootFolder = Paths.get(classesRootFolderUri.getSchemeSpecificPart());
//            //look for all subfolders in the pagesFolder, using a filter checking if a child of the folder is a directory or not
//            DirectoryStream<Path> classFolders = Files.newDirectoryStream(classesRootFolder,
//                                                                              new DirectoryStream.Filter<Path>(){
//                                                                                  @Override
//                                                                                  public boolean accept(Path file) throws IOException {
//                                                                                      return (Files.isDirectory(file));
//                                                                                  }
//                                                                              });
//            for(Path classFolder : classFolders){
//                String className = classFolder.getFileName().toString();
//                add(className);
//            }
//            return this;
//        }
//        catch(IOException e){
//            throw new CacheException("Problem while reading page-classes from directory '" + BlocksConfig.getEntitiesFolder() + "'.", e);
//        }
        Path classesRootFolder = Paths.get("/Users/wouter/git/com.beligum.blocks.core/src/main/resources/templates");

        try {
            Files.walkFileTree(classesRootFolder, new SimpleFileVisitor<Path>()
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
        } catch (Exception e) {
            int x = 0;
        }

    }

//
//    /**
//     * Add a template page (starting from the pageclass-name) to the page-cache
//     * @param viewableClassName the page-class-name (f.i. "default" for a pageClass filtered from the file "entities/default/index.html") of the page-class-template to be parsed and added to the cache as a couple (pageClassName, pageClass)
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
//    abstract protected String getClassRootFolder();
//
//    /**
//     *
//     * @param viewableClassName the name of the viewable-class to get a parser for
//     * @return a parser for parsing the viewable with a certain viewable-class name
//     */
//    abstract protected AbstractViewableParser<T> getParser(String viewableClassName);

}

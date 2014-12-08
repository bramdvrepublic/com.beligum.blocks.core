package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.utils.Logger;
import com.beligum.core.framework.utils.toolkit.FileFunctions;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;

/**
* Created by bas on 03.11.14.
*/
public abstract class AbstractTemplatesCache<T extends AbstractTemplate>
{
    /** boolean telling us whether or not one of the inheriting classes is already running through the template-html-files*/
    private static boolean runningTroughHtmlTemplates = false;

    /**
     * protected constructor for singleton-use of extending classes
     */
    protected AbstractTemplatesCache(){

    }

    /**
     * This method returns a map with all present Cachables (value) by name (key)
     * @returns a map of all the currently cached Cachables from the application cache
     */
    abstract protected Map<String, T> getCache();

    /**
     * Get the template with a certain name from the application cache
     * @param name the unique name of the template to get
     * @return a template from the application cache
     */
     abstract public T get(String name) throws CacheException;

    /**
     * Try to add this template to the cache. If a template with the same id is present, the template will not be added and false will be returned.
     * A version will also be stored in the redis-db if the template has be added.
     * @param template the template to be added to the applications cache, the key will be the object's unversioned id
     * @return true if the template has been added, false if not
     */
    public boolean add(T template) throws CacheException
    {
        try{
            if(!getCache().containsKey(template.getUnversionedId())) {
                RedisID lastVersion = new RedisID(template.getId().getUrl(), RedisID.LAST_VERSION);
                AbstractTemplate storedTemplate = Redis.getInstance().fetchTemplate(lastVersion, this.getCachedClass());
                if(!template.equals(storedTemplate)){
                    Redis.getInstance().save(template);
                }
                else{
                    //if this template was already stored in db, we should cache the db-version, since it has the correct time-stamp
                    template = (T) storedTemplate;
                }
                getCache().put(template.getUnversionedId(), template);
                return true;
            }
            else{
                return false;
            }
        }catch (Exception e){
            throw new CacheException("Error while trying to add template with id '" + template.getId() + "'.", e);
        }
    }

    /**
     * Add a template to the cache. If a template with the same id is already present in the cache, it is replaced with the one specified.
     * A new version will be stored in the redis-db.
     * @param template the template to be added to the applications cache, the key will be the object's unversioned id
     * @return the template with the same unversioned id which was in the cache before, or null if no template with that id was present in cache before
     */
    public AbstractTemplate replace(T template) throws CacheException
    {
        try{
            boolean added = this.add(template);
            if(added) {
                return null;
            }
            else{
                AbstractTemplate cachedTemplate = getCache().get(template.getUnversionedId());
                if(!template.equals(cachedTemplate)){
                    //TODO: last version should be fetched from db and when the template has changed a new version should be created and saved to db
                    RedisID lastVersion = new RedisID(template.getId().getUrl(), RedisID.LAST_VERSION);
                    AbstractTemplate storedTemplate = Redis.getInstance().fetchTemplate(lastVersion, this.getCachedClass());
                    if (!template.equals(storedTemplate)){
                        Redis.getInstance().save(template);
                    }
                }
                return cachedTemplate;
            }
        }catch (Exception e){
            throw new CacheException("Error while replacing template with id '" + template.getId() + "'.", e);
        }
    }

    /**
     * Fill up the page-cache with all template found in file-system
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
                            try {
                                new TemplateParser().cacheTemplatesFromFile(new String(Files.readAllBytes(filePath)));
                            }
                            catch (ParseException e) {
                                Logger.error("Parse error while parsing file '" + filePath + "'.", e);
                            }
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

    /**
     * Gets all  values of the cache's map
     * @return
     */
    public Collection<T> values(){
        return this.getCache().values();
    }

    /**
     *
     * @return the object-class being stored in this cache
     */
    abstract public Class<? extends AbstractTemplate> getCachedClass();

}
package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.utils.Logger;
import com.beligum.core.framework.utils.toolkit.FileFunctions;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by bas on 03.11.14.
 */
public abstract class AbstractTemplatesCache<T extends AbstractTemplate>
{
    //TODO BAS: EntityTemplateCache for frequently visited pages

    /** boolean telling us whether or not one of the inheriting classes is already running through the template-html-files*/
    private static boolean runningTroughHtmlTemplates = false;

    /**
     * protected constructor for singleton-use of extending classes
     */
    protected AbstractTemplatesCache(){

    }

    /**
     * This method returns a map with all present Cachables (value) by name (key)
     * @return a map of all the currently cached Cachables from the application cache
     */
    abstract protected Map<String, T> getCache();

    /**
     * Get the template with a certain name from the application cache.$
     * If that template is not present, return the default template
     * @param name the unique name of the template to get
     * @return a template from the application cache, or the default-template if no template with the specified name can be found
     */
    public T get(String name) throws CacheException
    {
        try {
            if(name != null) {
                Map<String, T> applicationCache = this.getCache();
                T template = applicationCache.get(getTemplateKey(name));
                if(template != null) {
                    return template;
                }
                else{
                    return applicationCache.get(getTemplateKey(getDefaultTemplateName()));
                }
            }
            else{
                return this.getCache().get(getTemplateKey(getDefaultTemplateName()));
            }
        }catch(IDException e){
            throw new CacheException("Could not get "+ PageTemplate.class.getSimpleName() + " '" + name + "' from cache.", e);
        }
    }

    /**
     * Try to add this template to the cache. If a template with the same id is present, the template will not be added and false will be returned.
     * A version will also be stored in the redis-db if the template has be added.
     * @param template the template to be added to the applications cache, the key will be the object's unversioned id
     * @return true if the template has been added, false if not
     */
    public boolean add(T template) throws CacheException
    {
        try{
            if(template == null){
                return false;
            }
            //TODO BAS: when adding possibility to parse multiple entity-class-languages from file to cache, multiple languages should be able to be added. This class should have the functionality to put two different languages together in one entity-template-class
            if(!getCache().containsKey(template.getUnversionedId())) {
                AbstractTemplate storedTemplate = (AbstractTemplate) Redis.getInstance().fetchLastVersion(template.getId(), this.getCachedClass());
                if(storedTemplate == null){
                    Redis.getInstance().create(template);
                }
                else if(!template.equals(storedTemplate)){
                    Redis.getInstance().update(template);
                }
                else{
                    //if this template was already stored in db, we should cache the db-version, since it has the correct time-stamp
                    template = (T) storedTemplate;
                }
                getCache().put(template.getUnversionedId(), template);
                return true;
            }
            //            else if(getCache().get(template.getUnversionedId()).getLanguages().contains(template.getLanguages())) {
            //
            //            }
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
                    BlocksID lastVersion = new BlocksID(template.getId().getUrl(), BlocksID.NO_VERSION, false);
                    AbstractTemplate storedTemplate = (AbstractTemplate) Redis.getInstance().fetchLastVersion(lastVersion, this.getCachedClass());
                    if(storedTemplate == null){
                        Redis.getInstance().create(template);
                    }
                    else if (!template.equals(storedTemplate)){
                        Redis.getInstance().update(template);
                    }
                    getCache().put(template.getUnversionedId(), template);
                }
                return cachedTemplate;
            }
        }catch (Exception e){
            throw new CacheException("Error while replacing template with id '" + template.getId() + "'.", e);
        }
    }

    /**
     *
     * @param templateName
     * @return true if the cache contains a mapping for the specified template-name, false otherwise
     */
    public boolean contains(String templateName) throws IDException
    {
        return getCache().containsKey(getTemplateKey(templateName));
    }

    /**
     * reset this application-cache, trashing all it's content
     */
    abstract public void reset();


    /**
     * Fill up the page-cache with all template found in file-system
     * @throws com.beligum.blocks.core.exceptions.CacheException
     */
    protected void fillCache() throws CacheException
    {
        if(!runningTroughHtmlTemplates) {
            runningTroughHtmlTemplates = true;
            URI rootFolderUri = FileFunctions.getCurrentMavenSrcResourceFolder(this.getClass());
            Path rootFolder = Paths.get(rootFolderUri.getSchemeSpecificPart());
            Path templatesFolder = rootFolder.resolve(BlocksConfig.getTemplateFolder());

            try {
                //list which will be filled up with all templates found in all files in the templates-folder
                final List<AbstractTemplate> foundTemplates = new ArrayList<>();
                //set which will be filled up with all class-names found in all files in the templates-folder
                final Set<String> foundEntityClassNames = new HashSet<>();

                //first fetch all blueprints from all files
                FileVisitor<Path> visitor = new SimpleFileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                                    throws IOException
                    {
                        if (filePath.getFileName().toString().endsWith("html") || filePath.getFileName().toString().endsWith("htm")) {
                            try {
                                String html = new String(Files.readAllBytes(filePath));
                                TemplateParser.findTemplatesFromFile(html, foundTemplates, foundEntityClassNames);
                            }
                            catch (ParseException e) {
                                Logger.error("Parse error while fetching page-templates and blueprints from file '" + filePath + "'.", e);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                };
                Files.walkFileTree(templatesFolder, visitor);

                //then add all default-value's to the found classes
                TemplateParser.injectDefaultsInFoundTemplatesAndCache(foundTemplates);
            }
            catch (Exception e) {
                throw new CacheException("Error while filling cache: " + this, e);
            }
            finally {
                runningTroughHtmlTemplates = false;
            }
        }

    }

    /**
     * Gets all  values of the cache's map, sorted by name
     */
    public List<T> values()
    {
        Collection<T> templates = this.getCache().values();
        List<T> templateList = new LinkedList<>();
        for(T template : templates){
            templateList.add(template);
        }
        Collections.sort(templateList);
        return templateList;
    }

    /**
     * Gets all keys of the cache's map, alphabetically sorted
     */
    public List<String> keys()
    {
        Collection<String> keys = this.getCache().keySet();
        List<String> keysList = new LinkedList<>();
        for(String key : keys){
            keysList.add(key);
        }
        Collections.sort(keysList);
        return keysList;
    }

    /**
     *
     * @return the object-class being stored in this cache
     */
    abstract public Class<? extends AbstractTemplate> getCachedClass();

    abstract protected String getTemplateKey(String templateName) throws IDException;

    abstract protected String getDefaultTemplateName();

}

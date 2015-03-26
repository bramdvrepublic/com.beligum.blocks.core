package com.beligum.blocks.core.caching.redis;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.base.R;
import org.apache.shiro.util.AntPathMatcher;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by bas on 03.11.14.
 */
public abstract class AbstractTemplatesCache<T extends AbstractTemplate>
{
    //TODO: EntityTemplateCache for frequently visited pages

    /**
     * boolean telling us whether or not one of the inheriting classes is already running through the template-html-files
     */
    private static boolean runningTroughHtmlTemplates = false;

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * protected constructor for singleton-use of extending classes
     */
    protected AbstractTemplatesCache()
    {

    }

    /**
     * This method returns a map with all present Cachables (value) by name (key)
     *
     * @return a map of all the currently cached Cachables from the application cache
     */
    abstract protected Map<String, T> getCache();

    /**
     * Get the template with a certain name from the application cache.$
     * If that template is not present, return the default template
     *
     * @param name the unique name of the template to get
     * @return a template from the application cache, or the default-template if no template with the specified name can be found
     */
    public T get(String name) throws CacheException
    {
        try {
            if (name != null) {
                Map<String, T> applicationCache = this.getCache();
                T template = applicationCache.get(getTemplateKey(name));
                if (template != null) {
                    return template;
                }
                else {
                    //TODO: if this is null, check Redis for last version
                    return applicationCache.get(getTemplateKey(getDefaultTemplateName()));
                }
            }
            else {
                return this.getCache().get(getTemplateKey(getDefaultTemplateName()));
            }
        }
        catch (IDException e) {
            throw new CacheException("Could not get " + PageTemplate.class.getSimpleName() + " '" + name + "' from cache.", e);
        }
    }

    /**
     * Try to add this template to the cache. If a template with the same id is present, the template will not be added and false will be returned.
     * A version will also be stored in the redis-db if the template has be added.
     *
     * @param template the template to be added to the applications cache, the key will be the object's unversioned id
     * @return true if the template has been added, false if not
     */
    public boolean add(T template) throws CacheException
    {
        try {
            if (template == null) {
                return false;
            }
            //TODO: when adding possibility to parse multiple entity-class-languages from file to cache, multiple languages should be able to be added. This class should have the functionality to put two different languages together in one blueprint
            if(!getCache().containsKey(getTemplateKey(template.getName()))) {
                template = (T) RedisDatabase.getInstance().createOrUpdate(template.getId(), template, this.getCachedClass());
                getCache().put(template.getUnversionedId(), template);
                return true;
            }
            //            else if(getCache().get(template.getUnversionedId()).getLanguages().contains(template.getLanguages())) {
            //
            //            }
            else {
                return false;
            }
        }
        catch (Exception e) {
            throw new CacheException("Error while trying to add template with id '" + template.getId() + "'.", e);
        }
    }

    /**
     * Add a template to the cache. If a template with the same id is already present in the cache, it is replaced with the one specified.
     * A new version will be stored in the redis-db.
     *
     * @param template the template to be added to the applications cache, the key will be the object's unversioned id
     * @return the template with the same unversioned id which was in the cache before, or null if no template with that id was present in cache before
     */
    public AbstractTemplate replace(T template) throws CacheException
    {
        try {
            boolean added = this.add(template);
            if (added) {
                return null;
            }
            else {
                AbstractTemplate cachedTemplate = getCache().get(template.getUnversionedId());
                if (!template.equals(cachedTemplate)) {
                    template = (T) RedisDatabase.getInstance().createOrUpdate(template.getId(), template, this.getCachedClass());
                    getCache().put(template.getUnversionedId(), template);
                }
                return cachedTemplate;
            }
        }
        catch (Exception e) {
            throw new CacheException("Error while replacing template with id '" + template.getId() + "'.", e);
        }
    }

    /**
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
     *
     * @throws com.beligum.blocks.core.exceptions.CacheException
     */
    protected void fillCache() throws CacheException
    {
        if (!runningTroughHtmlTemplates) {
            runningTroughHtmlTemplates = true;

            try {
                List<Path> allResourceFolders = R.resourceLoader().getResourceFolders();

                //list which will be filled up with all blueprints and page templates found in all files in the templates-folder
                final List<AbstractTemplate> foundTemplates = new ArrayList<>();
                //set which will be filled up with all blueprint types found in all files in the templates-folder
                final Set<String> foundBlueprintTypes = new HashSet<>();

                //first fetch all blueprints from all files
                for (Path resourceFolder : allResourceFolders) {
                    Path templatesFolder = resourceFolder.resolve(Blocks.config().getTemplateFolder());

                    if (Files.exists(templatesFolder)) {

                        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>()
                        {
                            @Override
                            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                                            throws IOException
                            {
                                String path = filePath.getFileName().toString();
                                if (pathMatcher.matches("*.html", path) || pathMatcher.match("*.htm", path)) {
//                                    try {
//                                        String html = new String(Files.readAllBytes(filePath));
////                                        TemplateParser.findTemplatesFromFile(html, foundTemplates, foundEntityClassNames);
//                                    }
//                                    catch (ParseException e) {
//                                        Logger.error("Parse error while fetching page-templates and blueprints from file '" + filePath + "'.", e);
//                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        };
                        Files.walkFileTree(templatesFolder, visitor);

                    }
                }

                //then add all default-value's to the found blueprints
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
        for (T template : templates) {
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
        for (String key : keys) {
            keysList.add(key);
        }
        Collections.sort(keysList);
        return keysList;
    }

    /**
     * @return the object-class being stored in this cache
     */
    abstract public Class<? extends AbstractTemplate> getCachedClass();

    abstract protected String getTemplateKey(String templateName) throws IDException;

    abstract protected String getDefaultTemplateName();

    //-----PRIVATE FUNCTIONS-----
    protected List<Path> findAllResourceFolders() throws Exception
    {
        return R.resourceLoader().getResourceFolders();
//        return FileFunctions.searchResourcesInClasspath(FileFunctions.getClasswideSearchFolder(), new FileFunctions.ResourceSearchPathFilter()
//        {
//            @Override
//            public Path doFilter(Path path)
//            {
//                //since the URI is the META-INF folder, we're looking for it's parent, the root (resources) folder
//                return path.getParent();
//            }
//        });
    }
}

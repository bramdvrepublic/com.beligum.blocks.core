package com.beligum.blocks.routing;

import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.models.interfaces.WebPath;
import com.beligum.blocks.models.sql.DBPath;
import com.beligum.blocks.rdf.ontology.Classes;
import com.beligum.blocks.utils.RdfTools;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 1/06/15.
 * <p/>
 * Defines an internal Blocks route based on a uri
 * this is: the getLanguage, the rootNode, the final node (node where the route ends that contains the info for the router )
 */
public class Route
{
    private URI uri;

    private PersistenceController database;
    // simplePath is path without getLanguage e.g. /test
    private Path simplePath;
    // languagedPath is path with getLanguage e.g. /en/test
    private Path languagedPath;
    private Locale locale;
    private WebPath finalNode;

    public Route(URI uri, PersistenceController database)
    {
        this.database = database;

        if (uri == null) {
            uri = Settings.instance().getSiteDomain();
        }
        this.uri = uri;

        java.nio.file.Path currentPath = Paths.get(uri.getPath());

        // If not in production set the domain manually (overwrite localhost)
        URI domain = uri;
        if (!R.configuration().getProduction()) {
            domain = Settings.instance().getSiteDomain();
        }

        this.locale = getLanguageFromPath(currentPath);
        if (locale.equals(Locale.ROOT)) {
            this.simplePath = Paths.get("/").resolve(currentPath).normalize();
            this.languagedPath = Paths.get("/").resolve(Settings.instance().getDefaultLanguage().getLanguage()).resolve(this.simplePath).normalize();
            this.uri = UriBuilder.fromUri("").scheme(this.uri.getScheme()).userInfo(this.uri.getUserInfo()).host(this.uri.getHost()).port(this.uri.getPort()).path(this.languagedPath.toString())
                                      .replaceQuery(this.uri.getQuery()).fragment(this.uri.getFragment()).build();
        }
        else {
            this.languagedPath = Paths.get("/").resolve(currentPath).normalize();
            if (currentPath.getNameCount() > 1) {
                this.simplePath = Paths.get("/").resolve(currentPath.subpath(1, currentPath.getNameCount())).normalize();
            }
            else {
                this.simplePath = Paths.get("/");
            }
        }

        this.finalNode = PersistenceControllerImpl.instance().getPath(simplePath, this.locale);
    }

    public boolean exists()
    {
        return this.finalNode != null;
    }

    public void create() throws Exception
    {
        WebPath retVal = PersistenceControllerImpl.instance().getPath(simplePath, locale);
        URI blockId = RdfTools.createRelativeResourceId(Classes.Page);
        if (retVal == null) {

            // Check if other languages are linked to this path
            Map<String, WebPath> languagePaths = PersistenceControllerImpl.instance().getLanguagePaths(simplePath.toString());
            if (languagePaths.size() > 0) {
                if (languagePaths.containsKey(Settings.instance().getDefaultLanguage().toString())) {
                    blockId = languagePaths.get(Settings.instance().getDefaultLanguage().toString()).getBlockId();
                }
                else {
                    blockId = languagePaths.values().iterator().next().getBlockId();
                }
                retVal = new DBPath(blockId, simplePath, locale);
            }
            else {
                // this path does not yet exist for any language, so we can create it for al known languages
                retVal = new DBPath(blockId, simplePath, locale);

                PersistenceControllerImpl.instance().savePath(retVal);

                Map<String, WebPath> paths = PersistenceControllerImpl.instance().getPaths(blockId);

                //TODO: implement better localized paths
                // find the subpath for this language based our current path
                // check if this path exists, if not create

                // now add paths for other languages
                for (Locale l : Settings.instance().getLanguages().values()) {
                    if (!l.equals(locale) && !paths.containsKey(l.getLanguage())) {
                        WebPath webPath = new DBPath(blockId, simplePath, l);
                        PersistenceControllerImpl.instance().savePath(webPath);
                    }
                }

            }

        }

        this.finalNode = retVal;
    }

    // Path without getLanguage
    public Path getPath()
    {
        return this.simplePath;
    }

    // Path with getLanguage
    public Path getLanguagedPath()
    {
        return this.languagedPath;
    }

    public Locale getLocale()
    {
        return this.locale;
    }

    public URI getURI()
    {
        return this.uri;
    }

    public WebPath getWebPath()
    {
        return this.finalNode;
    }

    public PersistenceController getBlocksDatabase()
    {
        return this.database;
    }

    public void getAlternateLocalPath()
    {
        WebPath webPath = PersistenceControllerImpl.instance().getActivePath(simplePath);
        if (webPath != null) {
            this.finalNode = webPath;
            this.locale = webPath.getLanguage();
        }
    }

    private Locale getLanguageFromPath(Path path)
    {
        Locale retVal = null;
        if (path.getNameCount() > 0) {
            String lang = path.getName(0).toString();
            retVal = Settings.instance().getLanguages().get(lang);
            if (retVal == null) {
                retVal = Locale.ROOT;
            }
        }
        else {
            retVal = Locale.ROOT;
        }
        return retVal;
    }
    //    /*
    //    * returns the end node following a path starting from a source,
    //    * if no node is found return null
    //    *
    //    * @Param srcNode: node to start form
    //    * @Param path: path without a getLanguage
    //    * @Param locale: locale to search in
    //    * */
    //    public WebNode getNodeFromNodeWithPath(WebNode srcNode, Path path, Locale locale)
    //    {
    //        if (locale.equals(Locale.ROOT)) {
    //            locale = Settings.instance().getDefaultLanguage();
    //        }
    //
    //        WebNode currentNode = srcNode;
    //        /*
    //        * Starting from the root node, find each sub path. First find 1 for the currnet getLanguage
    //        * If subpath is not found find a path with this name for the default getLanguage and without a path for the current getLanguage
    //        * */
    //        for (int i =0; i < path.getNameCount(); i++) {
    //            if (currentNode != null) {
    //                WebPath subPath = currentNode.getChildPath(path.getName(i).toString(), locale);
    //                if (subPath == null && !locale.equals(Settings.instance().getDefaultLanguage())) {
    //                    subPath = currentNode.getChildPath(path.getName(i).toString(), Settings.instance().getDefaultLanguage());
    //
    //                }
    //                if (subPath != null) {
    //                    currentNode = subPath.getChildWebNode();
    //                } else {
    //                    currentNode = null;
    //                }
    //            } else {
    //                break;
    //            }
    //        }
    //        return currentNode;
    //
    //    }

    /*
    * Create a path in the database starting from the given node
    *
    * @param srcNode    the node to start from
    * @param path       the path to create
    * @param locale     the getLanguage of the path
    * @return           returns the last node of the path
    * */
    //    public WebNode addPathToNode(URI masterpage, Path path, Locale locale)
    //    {
    //        if (locale.equals(Locale.ROOT)) {
    //            locale = Settings.instance().getDefaultLanguage();
    //        }
    //
    //        WebNode retVal = null;
    //        /*
    //        * Starting from the root node, find each sub path. First find 1 for the current getLanguage
    //        * If subpath is not found find a path with this name for the default getLanguage and without a path for the current getLanguage
    //        * */
    //        for (int i =0; i < path.getNameCount(); i++) {
    //            if (retVal != null) {
    //                WebNode prev = retVal;
    //                retVal = getNodeFromNodeWithPath(retVal, path.getName(i), locale);
    //                if (retVal == null) {
    //                    retVal = database.createWebNode(masterpage, path.getName(i), locale);
    //                }
    //            } else {
    //                break;
    //            }
    //        }
    //        return retVal;
    //    }

}

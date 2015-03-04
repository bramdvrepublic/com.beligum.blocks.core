package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.exceptions.UrlIdMappingException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.UrlIdMapping;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.org.mozilla.javascript.ast.Block;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by bas on 23.02.15.
 */
public class XMLUrlIdMapper implements UrlIdMapper
{
    public static final String ROOT = "root";
    public static final String PATH = "path";
    public static final String TRANSLATION = "translation";
    public static final String TRANSLATIONS = "translations";
    public static final String LANGUAGE = "lang";
    public static final String PATH_ID = "path-id";
    public static final String BLOCKS_ID = "blocks-id";


    private static XMLUrlIdMapper instance;

    private Document urlIdMapping;
    /**all path ids assigned, ordered by depth and from first assigned to last assigned (f.i. 1.1, 1.2, 1.3, 1.4, 2.1, 3.1, 3.2, 3.3)*/
    private SortedSet<String> pathIds = new TreeSet<>(new PathIdComparator());
    /**pre compiled xpath expression for fetching all path ancestors of an element*/
    private XPathExpression pathAncestorsExpr;
    /**pre compiled xpath expression for fetching the 'translations' child element*/
    private XPathExpression translationsElementExpr;
    /**pre compiled xpath expression for fetching the path parent of an element*/
    private XPathExpression pathParentExpr;
    /**pre compiled xpath expression for fetching the path children of an element*/
    private XPathExpression pathChildrenExpr;

    private HashMap<String, SiteMap> cachedSiteMaps = new HashMap<>();

    private Deque<XMLUrlIdMapper> cachedMappingVersions = new LinkedBlockingDeque<>(3);


    private XMLUrlIdMapper(Document urlIdMapping) throws XPathExpressionException
    {
        this.urlIdMapping = urlIdMapping;
        XPath xPath = XPathFactory.newInstance().newXPath();
        //precompile some xpath expressions
        this.pathAncestorsExpr = xPath.compile("ancestor-or-self::" + PATH);
        this.translationsElementExpr = xPath.compile("child::" + TRANSLATIONS);
        this.pathParentExpr = xPath.compile("ancestor::" + PATH);
        this.pathChildrenExpr = xPath.compile("child::" + PATH);
    }

    public static XMLUrlIdMapper getInstance()
                    throws UrlIdMappingException
    {
        try{
            if(instance == null){
                BlocksID xmlMappingId = getXMLMappingId(BlocksID.NO_VERSION);
                UrlIdMapping storedMapping = (UrlIdMapping) RedisDatabase.getInstance().fetchLastVersion(xmlMappingId, UrlIdMapping.class);
                instance = renderInstance(storedMapping);
                instance.writeOut();
            }
            return instance;

        }catch (Exception e){
            throw new UrlIdMappingException("Could not initialize url-id mapping.", e);
        }
    }

    private static XMLUrlIdMapper renderInstance(UrlIdMapping mapping) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document;
        if(mapping == null){
            //start with an empty url-mapping xml-string
            document = builder.parse(IOUtils.toInputStream("<?xml version=\"1.0\"?>\n" +
                                                           "<" + PATH + " " + ROOT +"='"+BlocksConfig.getSiteDomain()+"'>\n" +
                                                           "</"+PATH +">"));
            return new XMLUrlIdMapper(document);
        }
        else{
            document = builder.parse(IOUtils.toInputStream(mapping.getTemplate()));
            XPathExpression assignedIdsExpr = XPathFactory.newInstance().newXPath().compile("//" + PATH + "[@"+PATH_ID+"]");
            NodeList assignedPaths = (NodeList) assignedIdsExpr.evaluate(document, XPathConstants.NODESET);
            SortedSet<String> pathIds = new TreeSet<>(new PathIdComparator());
            for(int i = 0; i<assignedPaths.getLength(); i++){
                Element pathElement = (Element) assignedPaths.item(i);
                pathIds.add(pathElement.getAttribute(PATH_ID));
            }
            XMLUrlIdMapper mapper = new XMLUrlIdMapper(document);
            mapper.pathIds = pathIds;
            return mapper;
        }
    }

    @Override
    public BlocksID getId(URL url) throws UrlIdMappingException
    {
        try {
            BlocksID id = this.fetchId(url);
            if(id == null){
                return new BlocksID(url, BlocksID.NEW_VERSION, true);
            }
            else{
                return id;
            }
        }catch(UrlIdMappingException e){
            throw e;
        }
        catch(Exception e){
            throw new UrlIdMappingException("Could not get id for url '" + url + "'.", e);
        }
    }
    /**
     *
     * @param url
     * @return null if no id can be found for the url
     * @throws UrlIdMappingException
     */
    private BlocksID fetchId(URL url) throws UrlIdMappingException
    {
        try {
            if (url == null) {
                return null;
            }
            else {
                String[] urlAndLanguage = Languages.translateUrl(url.toString(), Languages.NO_LANGUAGE);
                URL urlNoLanguage = new URL(urlAndLanguage[0]);
                String language = urlAndLanguage[1];
                String[] splittedPath = urlNoLanguage.getPath().split("/");
                Element path = this.urlIdMapping.getDocumentElement();
                //if no parts are splitted of, we are dealing with the baseurl
                if(splittedPath.length == 0){
                    splittedPath = new String[1];
                    splittedPath[0] = "";
                }
                int i = 0;
                while(path != null && i<splittedPath.length){
                    //the base url is the document element, so we don't need to go deeper yet
                    if(splittedPath[i].equals("")){
                        path = path;
                    }
                    else {
                        XPathExpression translationExpr = XPathFactory.newInstance().newXPath().compile("child::" + PATH + "/child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[text()='" + splittedPath[i] + "']");
                        Element translationElement = (Element) translationExpr.evaluate(path, XPathConstants.NODE);
                        if(translationElement != null){
                            path = this.getPathParent(translationElement);
                        }
                        else{
                            path = null;
                        }
                    }
                    i++;
                }
                //we have the end of the path, here we should find the id
                if(path != null){
                    String blocksId = path.getAttribute(BLOCKS_ID);
                    if(!StringUtils.isEmpty(blocksId)) {
                        if (StringUtils.isEmpty(language)) {
                            return new BlocksID(blocksId, BlocksID.LAST_VERSION, BlocksID.PRIMARY_LANGUAGE);
                        }
                        else {
                            return new BlocksID(blocksId, BlocksID.LAST_VERSION, language);
                        }
                    }
                    else{
                        return null;
                    }
                }
                else{
                    return null;
                }

            }
        }catch (Exception e){
            throw new UrlIdMappingException("Could not get id for url '" + url + "'.", e);
        }
    }
    @Override
    public URL getUrl(BlocksID id) throws UrlIdMappingException
    {
        try {
            URL url = this.fetchUrl(id);
            if (url == null) {
                return id.getUrl();
            }
            else {
                return url;
            }
        }catch(UrlIdMappingException e){
            throw e;
        }
        catch(Exception e){
            throw new UrlIdMappingException("Could not fetch an url for '" + id + "'.", e);
        }
    }
    /**
     * @param id
     * @return Return the url the specified id is paired to (language inclusive), or null if no pairing exists.
     * @throws UrlIdMappingException
     */
    private URL fetchUrl(BlocksID id) throws UrlIdMappingException
    {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression idNodesExpr = xPath.compile("//" + PATH + "[@" + BLOCKS_ID + "='" + id.getUnversionedId() + "']");
            NodeList blocksIdElements = (NodeList) idNodesExpr.evaluate(this.urlIdMapping, XPathConstants.NODESET);
            if (blocksIdElements.getLength() == 0) {
                return null;
            }
            else {
                Element leave = this.selectLeave(blocksIdElements, id.getLanguage());
                //if no translation in the wanted language was found
                if (leave == null) {
                    String[] languages = BlocksConfig.getLanguages();
                    int i = 0;
                    while (leave == null && i < languages.length) {
                        String language = languages[i];
                        leave = this.selectLeave(blocksIdElements, language);
                        i++;
                    }
                    //if none of the preferred languages could be found, just select id path element as the leave
                    if(leave == null){
                        leave = (Element) blocksIdElements.item(0);
                    }
                }
                List<Element> ancestors = this.getPathAncestors(leave);
                String url = BlocksConfig.getSiteDomain();
                for(int i = 1; i<ancestors.size(); i++){
                    Element path = ancestors.get(i);
                    url += "/" + this.getTranslation(path, id.getLanguage());
                }
                return new URL(url);
            }
        }catch(Exception e){
            throw new UrlIdMappingException("Could not fetch an url for '" + id + "'.", e);
        }
    }

    @Override
    public UrlIdPair put(BlocksID id, URL url) throws UrlIdMappingException
    {
        try {
            if (url == null) {
                throw new NullPointerException("Cannot add null url to url-id mapping.");
            }
            if (id == null) {
                throw new NullPointerException("Cannot add null id to url-id mapping.");
            }
            String[] urlAndLanguage = Languages.translateUrl(url.toString(), Languages.NO_LANGUAGE);
            String language = urlAndLanguage[1];
            if (!Languages.isNonEmptyLanguageCode(language)) {
                language = id.getLanguage();
            }
            URL urlNoLanguage = new URL(urlAndLanguage[0]);


            /*
             * Remove the url (with the found language) that was already present in the mapping with this id
             */
            URL previousUrl = this.remove(id, false);
            BlocksID previousId = this.remove(new URL(Languages.translateUrl(urlNoLanguage.toString(), language)[0]), false);

            /*
             * Split the url in it's path-parts and add them to the mapping
             */
            String[] splittedPath = urlNoLanguage.getPath().split("/");
            if(splittedPath.length == 0){
                splittedPath = new String[1];
                splittedPath[0] = "";
            }


            /*
             * Determine if part of this url is already present in the mapping, and if so, return it's ancestors.
             * If no parts of the url can be found, make sure the id is not already in the mapping. If so, return it's ancestors
             * If it not present in the mapping, create a new path element to start from.
             */

            Element parent = null;
            List<Element> ancestors = this.getExistingPathParts(splittedPath, language);
            //if the only ancestor is the root, no path parts were found
            if(ancestors.size()==1){
                ancestors = this.getPathAncestors(id);
            }

            parent = ancestors.get(0);

            int i = 0;
            while (i < splittedPath.length) {
                Element translations = this.getTranslationsElement(parent);
                if (translations == null) {
                    translations = this.urlIdMapping.createElement(TRANSLATIONS);
                    parent.appendChild(translations);
                }
                translations = this.addTranslationElement(translations, language, splittedPath[i]);
                Element path;
                //if we have reached the leave of this url-branch, add the specified id
                if (i == splittedPath.length - 1) {
                    parent.setAttribute(BLOCKS_ID, id.getUnversionedId());
                    path = null;
                }
                //if still some ancestors haven't been used, proceed along the ancestorial branch
                else if (i + 1 < ancestors.size() && ancestors.get(i) != null) {
                    path = ancestors.get(i + 1);
                }
                //if no ancestors are left, a new path needs to be created at depth 'i'
                else {
                    path = this.createNewPathElement(i + 1);
                    parent.appendChild(path);
                }
                parent = path;
                i++;
            }
            this.cachedSiteMaps.clear();
            this.writeOut();
            return new UrlIdPair(previousUrl, previousId);
        }catch (Exception e){
            throw new UrlIdMappingException("Could not add url-id pair to mapping: (" + url + "," + id + ")", e);
        }
    }

    @Override
    public URL remove(BlocksID languagedId) throws UrlIdMappingException
    {
        return this.remove(languagedId, true);
    }
    private URL remove(BlocksID languagedId, boolean writeOut) throws UrlIdMappingException
    {
        try{
            URL previousUrl = this.fetchUrl(languagedId);
            boolean changed = this.removePathForId(languagedId);
            if(changed){
                if(writeOut) this.writeOut();
                this.cachedSiteMaps.clear();
                return previousUrl;
            }
            else{
                return null;
            }
        }catch (Exception e){
            throw new UrlIdMappingException("Could not remove id '" + languagedId + "' from mapping.", e);
        }
    }

    @Override
    public BlocksID remove(URL languagedUrl) throws UrlIdMappingException
    {
        return this.remove(languagedUrl, true);
    }
    private BlocksID remove(URL languagedUrl, boolean writeOut) throws UrlIdMappingException
    {
        try {
            String[] urlAndLanguage = Languages.translateUrl(languagedUrl.toString(), Languages.NO_LANGUAGE);
            String urlNoLanguageString = urlAndLanguage[0];
            String language = urlAndLanguage[1];
            if(StringUtils.isEmpty(language)){
                throw new UrlIdMappingException("Cannot remove an url without language information: '" + languagedUrl + "'.");
            }
            BlocksID id = this.fetchId(languagedUrl);
            boolean changed = this.removePathForId(id);
            if(changed){
                if(writeOut) this.writeOut();
                this.cachedSiteMaps.clear();
                return id;
            }
            else{
                return null;
            }
        }
        catch (UrlIdMappingException e){
            throw e;
        }
        catch(Exception e){
            throw new UrlIdMappingException("Could not remove url '" + languagedUrl + "' from mapping.", e);
        }
    }

    @Override
    public BlocksID getLastId(URL url) throws UrlIdMappingException
    {
        try{
            //TODO BAS SH: je bent bezig met het checken van the volgorde bij de deque van de mappings (.iterator() of .descendingIterator()?), zet dan de capacity op 10 of zo
            //TODO BAS SH 2: we willen een sitemap viewable maken en daar kun je dan urls aanpassen, vertalen of verplaatsen (kan dat in 1 methode?)
            BlocksID retVal = null;
            Iterator<XMLUrlIdMapper> it = this.cachedMappingVersions.iterator();
            while(retVal == null && it.hasNext()){
                XMLUrlIdMapper mapper = it.next();
                retVal = mapper.fetchId(url);
            }
            if(retVal != null){
                return retVal;
            }
            else {
                BlocksID mapperId = getXMLMappingId(BlocksID.NO_VERSION);
                List<UrlIdMapping> mappings = RedisDatabase.getInstance().fetchVersionList(mapperId, UrlIdMapping.class);
                int i = this.cachedMappingVersions.size();
                while (retVal == null && i < mappings.size()) {
                    XMLUrlIdMapper mapper = this.addMappingToDeque(mappings.get(i));
                    retVal = mapper.fetchId(url);
                    i++;
                }
                return retVal;
            }
        }catch(Exception e){
            throw new UrlIdMappingException("Could not fetch the last id that was not deleted for url '" + url + "'.", e);
        }
    }
    /**
     * Remove the mapping from cache.
     */
    @Override
    public void reset()
    {
        this.instance = null;
    }
    @Override
    public SiteMap renderSiteMap(String language) throws UrlIdMappingException
    {
        try {
            if(!Languages.isNonEmptyLanguageCode(language)){
                throw new LanguageException("Unknown language found '" + language + "'.");
            }
            if(!this.cachedSiteMaps.containsKey(language)) {
                SiteMapNode root = this.createSiteMapNode(this.urlIdMapping.getDocumentElement(), language);
                SiteMap siteMap = new SiteMap(root, language);
                this.cachedSiteMaps = new HashMap<>();
                this.cachedSiteMaps.put(language, siteMap);
            }
            return this.cachedSiteMaps.get(language);
        }
        catch (Exception e) {
            throw new UrlIdMappingException("Could not render site map.", e);
        }
    }

    private SiteMapNode createSiteMapNode(Element path, String language) throws MalformedURLException, IDException, UrlIdMappingException, XPathExpressionException, LanguageException
    {
        URL url = null;
        String entityId = path.getAttribute(BLOCKS_ID);
        boolean hasEntity = !StringUtils.isEmpty(entityId);
        if(hasEntity){
            url = this.fetchUrl(new BlocksID(entityId, BlocksID.NO_VERSION, language));
        }
        else if(path.equals(this.urlIdMapping.getDocumentElement())) {
            url = new URL(this.urlIdMapping.getDocumentElement().getAttribute(ROOT));
        }
        else {
            List<Element> ancestors = this.getPathAncestors(path);
            String urlPath = "";
            int i = 1;
            while(i<ancestors.size()){
                Element p = ancestors.get(i);
                String translation = this.getTranslation(p, language);
                if(!StringUtils.isEmpty(translation)) {
                    urlPath += "/" + translation;
                }
                i++;
            }
            url = new URL(new URL(BlocksConfig.getSiteDomain()), urlPath);
        }
        url = new URL(Languages.translateUrl(url.toString(), language)[0]);
        SiteMapNode retVal = new SiteMapNode(url, hasEntity);
        NodeList pathChildren = (NodeList) this.pathChildrenExpr.evaluate(path, XPathConstants.NODESET);
        for(int i = 0; i<pathChildren.getLength(); i++){
            retVal.addChild(createSiteMapNode((Element) pathChildren.item(i), language));
        }
        return retVal;
    }

    //______________________PRIVATE_METHODS___________________

    private void writeOut() throws TransformerException, IOException, IDException, DatabaseException, ParserConfigurationException, SAXException, XPathExpressionException
    {
        /*
         * Writing xml as found in  'Core Java - Volume II - Advanced Features - ninth edition' by 'Cay S. Horstman, Gay Cornell', p 161
         * The easiest way is doing a null-transformation.
         */
        //construct the do-nothing transformation
        Transformer t = TransformerFactory.newInstance().newTransformer();
        //set indentation
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        //set indent to 2 for a good debug view
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        // apply the do-nothing transformation and send the output to a file
        DOMSource source = new DOMSource(urlIdMapping);
        Writer result = new StringWriter();
        t.transform(source, new StreamResult(result));

        /*
         * Save new version to db
         */
        BlocksID xmlId = getXMLMappingId(BlocksID.NEW_VERSION);
        UrlIdMapping treeTemplate = new UrlIdMapping(xmlId, result.toString());
        //make sure only one thread is reading or writing from or to db
        synchronized(this) {
            UrlIdMapping storedXML = (UrlIdMapping) RedisDatabase.getInstance().fetchLastVersion(xmlId, UrlIdMapping.class);
            if (storedXML == null) {
                RedisDatabase.getInstance().create(treeTemplate);
                this.addMappingToDeque(treeTemplate);
            }
            else if (!treeTemplate.equals(storedXML)) {
                RedisDatabase.getInstance().update(treeTemplate);
                this.addMappingToDeque(treeTemplate);
            }
            else {
                //no need to save to db, since this is an unchanged version
            }
        }
    }

    private static BlocksID getXMLMappingId(long version) throws MalformedURLException, IDException
    {
        return new BlocksID(new URL(BlocksConfig.getSiteDomain() + "/" + DatabaseConstants.URL_ID_MAPPING), version, true);
    }


    /**
     * Returns a list with all path-elements which are an ancestor of the path-leave holding the specified blocks-id, in order from the root on forward.
     * If the id is present in the mapping, the list also holds the node itself.
     * If the id is not present, the root is returned as the only ancestor.
     * @param blocksId
     * @throws XPathExpressionException
     */
    private List<Element> getPathAncestors(BlocksID blocksId) throws Exception
    {

        XPathExpression idNodesExpr = XPathFactory.newInstance().newXPath().compile("//" + PATH + "[@" + BLOCKS_ID + "='" + blocksId.getUnversionedId() + "']");
        NodeList blockIdElements = (NodeList) idNodesExpr.evaluate(this.urlIdMapping, XPathConstants.NODESET);
        //get the last (i.e. the deepest) occurrence of an element with the specified id
        Element idPath = blockIdElements.getLength() > 0 ? (Element) blockIdElements.item(blockIdElements.getLength()-1) : null;
        NodeList ancestors;
        if(idPath!=null){
            ancestors = (NodeList) this.pathAncestorsExpr.evaluate(idPath, XPathConstants.NODESET);
        }
        else{
            List<Element> retVal = new ArrayList<>();
            retVal.add(this.urlIdMapping.getDocumentElement());
            return retVal;
        }
        List<Element> ancestorList = new ArrayList<>();
        for(int i = 0; i<ancestors.getLength(); i++){
            ancestorList.add((Element) ancestors.item(i));
        }
        return ancestorList;
    }

    private List<Element> getPathAncestors(Element element) throws XPathExpressionException
    {
        if(element!=null){
            NodeList ancestors = (NodeList) this.pathAncestorsExpr.evaluate(element, XPathConstants.NODESET);
            List<Element> ancestorList = new ArrayList<>();
            for(int i = 0; i<ancestors.getLength(); i++){
                ancestorList.add((Element) ancestors.item(i));
            }
            return ancestorList;
        }
        else{
            return new ArrayList<>();
        }
    }

    private Element getPathParent(Element element) throws XPathExpressionException
    {
        if(element != null){
            NodeList ancestors = (NodeList) this.pathParentExpr.evaluate(element, XPathConstants.NODESET);
            if(ancestors.getLength()>0){
                return (Element) ancestors.item(ancestors.getLength()-1);
            }
            else{
                return null;
            }
        }
        else{
            return null;
        }
    }

    private Element getTranslationsElement(Element pathElement) throws XPathExpressionException
    {
        return (Element) this.translationsElementExpr.evaluate(pathElement, XPathConstants.NODE);
    }

    private String getTranslation(Element pathElement, String language) throws XPathExpressionException
    {
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression translationExpr = xPath.compile("child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[@"+LANGUAGE+"='"+language+"']");
        Element translation = (Element) translationExpr.evaluate(pathElement, XPathConstants.NODE);
        //if the language wasn't found, return another preferred language if present
        if(translation == null) {
            String[] languages = BlocksConfig.getLanguages();
            int i = 0;
            while(translation == null && i<languages.length){
                language = languages[i];
                translationExpr = xPath.compile("child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[@"+LANGUAGE+"='"+language+"']");
                translation = (Element) translationExpr.evaluate(pathElement, XPathConstants.NODE);
                i++;
            }
            //if none of the preferred languages could be found, return the first of the present translations
            if(translation == null){
                translationExpr = xPath.compile("child::" + TRANSLATIONS + "/child::" + TRANSLATION);
                translation = (Element) translationExpr.evaluate(pathElement, XPathConstants.NODE);
            }
        }
        //if the translation is still null, this means no translations were present in the path element, so we return an empty string
        if(translation == null){
            return "";
        }
        else{
            return translation.getTextContent();
        }
    }

    private Element addTranslationElement(Element translationsElement, String language, String content) throws Exception
    {
        //create a new translation element
        Element translation = this.urlIdMapping.createElement(TRANSLATION);
        translation.setAttribute(LANGUAGE, language);
        translation.setTextContent(content);

        //compare it to the previous translation and replace it if necessary
        XPathExpression previousTranslationExpr = XPathFactory.newInstance().newXPath().compile("child::"+TRANSLATION+"[@"+LANGUAGE+"='"+language+"']");
        Element previousTranslation = (Element) previousTranslationExpr.evaluate(translationsElement, XPathConstants.NODE);
        if(previousTranslation != null){
            translationsElement.replaceChild(translation, previousTranslation);
        }
        else {
            translationsElement.appendChild(translation);
        }
        return translationsElement;
    }

    private Element createNewPathElement(int depth) throws Exception
    {
        String id = "";
        SortedSet<String> depthIds = pathIds.subSet(depth + ".1", (depth + 1) + ".1");
        if(depthIds.isEmpty()){
            id = depth + ".1";
        }
        else{
            String lastAssignedId= depthIds.last();
            int nextNumber = Integer.parseInt(lastAssignedId.split("\\.")[1])+1;
            id = depth + "." + nextNumber;
        }
        Element path = this.urlIdMapping.createElement(PATH);
        //        path.setAttribute(PATH_ID, id);
        boolean added = this.pathIds.add(id);
        if(!added){
            throw new Exception("Could not add freshly rendered path id. This should not happen!");
        }
        return path;
    }

    /**
     * Start from the last element in de list and look if a translation in the specified language exists.
     * When it is found, return the last path-element of that branch.
     */
    private Element selectLeave(NodeList pathElements, String language) throws XPathExpressionException
    {
        XPathExpression translationExpr = XPathFactory.newInstance().newXPath().compile("child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[@" + LANGUAGE + "='" + language + "']");
        Element leave = null;
        int i = pathElements.getLength() - 1;
        while(leave == null && i>=0){
            Element blocksIdElement = (Element) pathElements.item(i);
            if((Element) translationExpr.evaluate(blocksIdElement, XPathConstants.NODE) != null){
                leave = blocksIdElement;
            }
            i--;
        }
        return leave;
    }

    /**
     *
     * @param id
     * @return true if the mapping has been altered, false otherwise
     * @throws Exception
     */
    private boolean removePathForId(BlocksID id) throws Exception
    {
        //if no id is found, nothing needs to be done
        if(id != null){
            List<Element> ancestors = this.getPathAncestors(id);
            int i = ancestors.size()-1;
            XPathExpression translationExpr = XPathFactory.newInstance().newXPath().compile("child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[@"+LANGUAGE+"='"+id.getLanguage()+"']");
            boolean changed = false;
            while(i>0){
                Element ancestor = ancestors.get(i);
                String blocksid = ancestor.getAttribute(BLOCKS_ID);
                //only pathpart without a blocksid should be removed
                if(StringUtils.isEmpty(blocksid) || blocksid.equals(id.getUnversionedId())) {
                    //remove the wanted translation, or if it does not exists, do nothing
                    Element translation = (Element) translationExpr.evaluate(ancestor, XPathConstants.NODE);
                    if (translation != null) {
                        Node translationsElement = translation.getParentNode();
                        translationsElement.removeChild(translation);
                        changed = !changed ? true : changed;
                        NodeList children = translationsElement.getChildNodes();
                        boolean hasElementChild = false;
                        int j = 0;
                        while (!hasElementChild && j < children.getLength()) {
                            hasElementChild = children.item(j) instanceof Element;
                            j++;
                        }
                        //if no other translation nodes are found, we can remove this 'translations' node and thus also this path node
                        if (!hasElementChild) {
                            Node path = translationsElement.getParentNode();
                            Node pathParent = path.getParentNode();
                            pathParent.removeChild(path);
                        }
                    }
                }
                else{
                    i = -1;
                }
                i--;
            }
            return changed;
        }
        else{
            return false;
        }
    }

    private List<Element> getExistingPathParts(String[] splittedPath, String language) throws XPathExpressionException
    {
        List<Element> existingPathParts = new ArrayList<>();
        Element path = this.urlIdMapping.getDocumentElement();
        existingPathParts.add(path);
        int i = 1;
        while(path != null && i<splittedPath.length) {
            XPathExpression pathPartExpr = XPathFactory.newInstance().newXPath().compile(
                            "child::" + PATH + "/child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[@"+ LANGUAGE +"='" + language + "' and text()='" + splittedPath[i] + "']");
            Element translation = (Element) pathPartExpr.evaluate(path, XPathConstants.NODE);
            if(translation != null){
                Element pathPart = (Element) translation.getParentNode().getParentNode();
                existingPathParts.add(pathPart);
                path = pathPart;
            }
            i++;
        }
        return existingPathParts;
    }


    private XMLUrlIdMapper addMappingToDeque(UrlIdMapping mapping) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        XMLUrlIdMapper mapper = renderInstance(mapping);
        if(!this.cachedMappingVersions.offerLast(mapper)){
            this.cachedMappingVersions.removeFirst();
            this.cachedMappingVersions.addLast(mapper);
        }
        return mapper;
    }
}

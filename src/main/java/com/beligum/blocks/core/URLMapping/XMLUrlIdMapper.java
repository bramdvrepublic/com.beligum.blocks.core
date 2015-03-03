package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.exceptions.IDException;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 23.02.15.
 */
public class XMLUrlIdMapper implements UrlIdMapper
{
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

    private XMLUrlIdMapper(Document urlIdMapping) throws XPathExpressionException
    {
        this.urlIdMapping = urlIdMapping;
        XPath xPath = XPathFactory.newInstance().newXPath();
        //precompile some xpath expressions
        this.pathAncestorsExpr = xPath.compile("ancestor-or-self::" + PATH);
        this.translationsElementExpr = xPath.compile("child::" + TRANSLATIONS);
        this.pathParentExpr = xPath.compile("ancestor::" + PATH);
    }

    public static XMLUrlIdMapper getInstance()
                    throws UrlIdMappingException
    {
        try{
            if(instance == null){
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                BlocksID xmlMappingId = getNewVersionXMLMappingId();
                UrlIdMapping storedXml = (UrlIdMapping) RedisDatabase.getInstance().fetchLastVersion(xmlMappingId, UrlIdMapping.class);
                Document document;
                if(storedXml == null){
                    //start with an empty url-mapping xml-string
                    document = builder.parse(IOUtils.toInputStream("<?xml version=\"1.0\"?>\n" +
                                                                   "<urls>\n" +
                                                                   "</urls>"));
                    instance = new XMLUrlIdMapper(document);
                }
                else{
                    document = builder.parse(IOUtils.toInputStream(storedXml.getTemplate()));
                    XPathExpression assignedIdsExpr = XPathFactory.newInstance().newXPath().compile("//" + PATH + "[@"+PATH_ID+"]");
                    NodeList assignedPaths = (NodeList) assignedIdsExpr.evaluate(document, XPathConstants.NODESET);
                    SortedSet<String> pathIds = new TreeSet<>(new PathIdComparator());
                    for(int i = 0; i<assignedPaths.getLength(); i++){
                        Element pathElement = (Element) assignedPaths.item(i);
                        pathIds.add(pathElement.getAttribute(PATH_ID));
                    }
                    instance = new XMLUrlIdMapper(document);
                    instance.pathIds = pathIds;
                }
                instance.writeOut();
            }
            return instance;

        }catch (Exception e){
            throw new UrlIdMappingException("Could not initialize url-id mapping.", e);
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
                int i;
                //if no parts are splitted of, we are dealing with the baseurl, so we start with i = 0
                if(splittedPath.length == 1){
                    i = 0;
                }
                //if parts are splitted of the url path, we start from the first interesting part (splittedPath = [ "", "the first word after the /", "the second word delimited by a /", ...])
                else{
                    i = 1;
                }
                while(path != null && i<splittedPath.length){
                    XPathExpression translationExpr = XPathFactory.newInstance().newXPath().compile("child::" + PATH + "/child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[text()='"+splittedPath[i]+"']");
                    Element translationElement = (Element) translationExpr.evaluate(path, XPathConstants.NODE);
                    if(translationElement != null){
                        path = this.getPathParent(translationElement);
                    }
                    else{
                        path = null;
                    }
                    i++;
                }
                //we have the end of the path, here we should find the id
                if(path != null){
                    String blocksId = path.getAttribute(BLOCKS_ID);
                    if(StringUtils.isEmpty(language)){
                        return new BlocksID(blocksId, BlocksID.LAST_VERSION, BlocksID.PRIMARY_LANGUAGE);
                    }
                    else{
                        return new BlocksID(blocksId, BlocksID.LAST_VERSION, language);
                    }
                }
                else{
                    return null;
                }

            }
        }catch (Exception e){
            throw new UrlIdMappingException("Could not get id for url '" + url + "'.");
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
                for (Element path : ancestors) {
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
            if (StringUtils.isEmpty(language)) {
                language = id.getLanguage();
            }
            URL urlNoLanguage = new URL(urlAndLanguage[0]);


            /*
             * Remove the url (with the found language) that was already present in the mapping with this id
             */
            URL previousUrl = this.remove(id, false);
            BlocksID previousId = this.remove(new URL(Languages.translateUrl(urlNoLanguage.toString(), language)[0]), false);

            /*
             * Determine if this id is already present in the mapping, and if so, return it's ancestors.
             * If it is not present in the mapping, the only element in the list will be the document-root.
             */
            List<Element> ancestors = this.getOrCreatePathAncestors(id);

            /*
             * Split the url in it's path-parts and add them to the mapping
             */
            String[] splittedPath = urlNoLanguage.getPath().split("/");
            int i;
            //if no parts are splitted of, we are dealing with the baseurl, so we start with i = 0
            if(splittedPath.length == 1){
                i = 0;
            }
            //if parts are splitted of the url path, we start from the first interesting part (splittedPath = [ "", "the first word after the /", "the second word delimited by a /", ...])
            else{
                i = 1;
            }
            Element parent = ancestors.get(0);
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
                else if (i < ancestors.size() && ancestors.get(i) != null) {
                    path = ancestors.get(i);
                }
                //if no ancestors are left, a new path needs to be created at depth 'i'
                else {
                    path = this.createNewPathElement(i + 1);
                    parent.appendChild(path);
                }
                parent = path;
                i++;
            }
            this.writeOut();
            return new UrlIdPair(previousUrl, previousId);
        }catch (Exception e){
            throw new UrlIdMappingException("Could not add url-id pair to mapping: (" + url + "," + id + ")" );
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
    /**
     * Remove the mapping from cache.
     */
    @Override
    public void reset()
    {
        this.instance = null;
    }



    //______________________PRIVATE_METHODS___________________

    private void writeOut() throws TransformerException, FileNotFoundException, MalformedURLException, IDException, DatabaseException
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
        BlocksID xmlId = getNewVersionXMLMappingId();
        UrlIdMapping treeTemplate = new UrlIdMapping(xmlId, result.toString());
        //make sure only one thread is reading or writing from or to db
        synchronized(this) {
            UrlIdMapping storedXML = (UrlIdMapping) RedisDatabase.getInstance().fetchLastVersion(xmlId, UrlIdMapping.class);
            if (storedXML == null) {
                RedisDatabase.getInstance().create(treeTemplate);
            }
            else if (!treeTemplate.equals(storedXML)) {
                RedisDatabase.getInstance().update(treeTemplate);
            }
            else {
                //no need to save to db, since this is an unchanged version
            }
        }
    }

    private static BlocksID getNewVersionXMLMappingId() throws MalformedURLException, IDException
    {
        return new BlocksID(new URL(BlocksConfig.getSiteDomain() + "/" + DatabaseConstants.URL_ID_MAPPING), BlocksID.NEW_VERSION, true);
    }

    /**
     * Returns a list with all path-elements which are an ancestor of the path-leave holding the specified blocks-id, in order from the root on forward.
     * If the id is present in the mapping, the list also holds the node itself.
     * If the id is not present, a new path root is returned as the only (new) ancestor.
     * @param blocksId
     * @throws XPathExpressionException
     */
    private List<Element> getOrCreatePathAncestors(BlocksID blocksId) throws Exception
    {
        XPathExpression idNodesExpr = XPathFactory.newInstance().newXPath().compile("//" + PATH + "[@" + BLOCKS_ID + "='" + blocksId.getUnversionedId() + "']");
        return this.getOrCreatePathAncestors(idNodesExpr);
    }

    //    private List<Element> getOrCreatePathAncestors(String pathId) throws Exception
    //    {
    //        XPathExpression idNodesExpr = XPathFactory.newInstance().newXPath().compile("//" + PATH + "[@" + PATH_ID + "='" + pathId + "']");
    //        return this.getOrCreatePathAncestors(idNodesExpr);
    //    }

    private List<Element> getOrCreatePathAncestors(XPathExpression expression) throws Exception
    {
        if(expression == null) {
            throw new NullPointerException("Cannot evaluate null-expression.");
        }
        NodeList blockIdElements = (NodeList) expression.evaluate(this.urlIdMapping, XPathConstants.NODESET);
        //get the last (i.e. the deepest) occurrence of an element with the specified id
        Element idPath = blockIdElements.getLength() > 0 ? (Element) blockIdElements.item(blockIdElements.getLength()-1) : null;
        NodeList ancestors;
        if(idPath!=null){
            ancestors = (NodeList) this.pathAncestorsExpr.evaluate(idPath, XPathConstants.NODESET);
        }
        else{
            //no such path exists yet, so we create its root at depth 1
            Element path = this.createNewPathElement(1);
            this.urlIdMapping.getDocumentElement().appendChild(path);
            ancestors = (NodeList) this.pathAncestorsExpr.evaluate(path, XPathConstants.NODESET);
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
        translation.appendChild(this.urlIdMapping.createTextNode(content));

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
        path.setAttribute(PATH_ID, id);
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
            List<Element> ancestors = this.getOrCreatePathAncestors(id);
            int i = ancestors.size()-1;
            XPathExpression translationExpr = XPathFactory.newInstance().newXPath().compile("child::" + TRANSLATIONS + "/child::" + TRANSLATION + "[@"+LANGUAGE+"='"+id.getLanguage()+"']");
            boolean changed = false;
            while(i>=0){
                Element ancestor = ancestors.get(i);
                //remove the wanted translation, or if it does not exists, do nothing
                Element translation = (Element) translationExpr.evaluate(ancestor, XPathConstants.NODE);
                if(translation != null) {
                    Node translationsElement = translation.getParentNode();
                    translationsElement.removeChild(translation);
                    changed = !changed ? true : changed;
                    NodeList children = translationsElement.getChildNodes();
                    boolean hasElementChild = false;
                    int j = 0;
                    while(!hasElementChild && j<children.getLength()){
                        hasElementChild = children.item(j) instanceof Element;
                        j++;
                    }
                    //if no other translation nodes are found, we can remove this 'translations' node and thus also this path node
                    if(!hasElementChild){
                        Node path = translationsElement.getParentNode();
                        Node pathParent = path.getParentNode();
                        pathParent.removeChild(path);
                    }
                }
                i--;
            }
            return changed;
        }
        else{
            return false;
        }
    }
}

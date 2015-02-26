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
import com.beligum.core.framework.utils.Logger;
import com.sun.crypto.provider.BlowfishKeyGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

    private XMLUrlIdMapper(Document urlIdMapping) throws XPathExpressionException
    {
        this.urlIdMapping = urlIdMapping;
        XPath xPath = XPathFactory.newInstance().newXPath();
        //precompile some xpath expressions
        this.pathAncestorsExpr = xPath.compile("ancestor-or-self::" + PATH);
        this.translationsElementExpr = xPath.compile("child::" + TRANSLATIONS);
    }

    public static XMLUrlIdMapper getInstance()
                    throws Exception
    {
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
            instance.add(new URL(BlocksConfig.getSiteDomain() + "/en/home/waterwell"), new BlocksID("blocks://LOC/hexadecimal:123123/en"));
            instance.add(new URL(BlocksConfig.getSiteDomain() + "/nl/thuis/waterput"), new BlocksID("blocks://LOC/hexadecimal:1231213/nl"));
//            instance.add(new URL(BlocksConfig.getSiteDomain() + "/fr/maison/source"), new BlocksID("blocks://LOC/hexadecimal:1231213/fr"));
            instance.add(new URL(BlocksConfig.getSiteDomain() + "/nl/thuis/eenanderevertaling"), new BlocksID("blocks://LOC/hexadecimal:1231213/nl"));
            instance.add(new URL(BlocksConfig.getSiteDomain() + "/nl/thuis/een/andere/vertaling"), new BlocksID("blocks://LOC/hexadecimal:1231213/nl"));
//            instance.add(new URL(BlocksConfig.getSiteDomain() + "/fr/maison/une/nouvelle/translation"), new BlocksID("blocks://LOC/hexadecimal:1231213/nl"));
            URL url = instance.getUrl(new BlocksID("blocks://LOC/hexadecimal:123112213/fr"));
            Logger.info("Found url " + url);
        }
        return instance;
    }

    @Override
    public BlocksID getId(URL url)
    {
        //TODO BAS: implement getId
        return null;
    }
    @Override
    public URL getUrl(BlocksID id) throws UrlIdMappingException
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
    public void add(URL url, BlocksID id) throws UrlIdMappingException
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
         * Determine if this id is already present in the mapping, and if so, return it's ancestors.
         * If it is not present in the mapping, the only element in the list will be the document-root.
         */
            List<Element> ancestors = this.getOrCreatePathAncestors(id);

        /*
         * Split the url in it's path-parts and add it to the mapping
         */
            String[] splittedPath = urlNoLanguage.getPath().split("/");
            int i = 1;
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

            //TODO BAS: this should not be done all the time, use threading for that?
            this.writeOut();
        }catch (Exception e){
            throw new UrlIdMappingException("Could not add url-id pair to mapping: (" + url + "," + id + ")" );
        }
    }

    @Override
    public void remove(BlocksID id)
    {
        //TODO BAS: implement remove
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

    private List<Element> getOrCreatePathAncestors(String pathId) throws Exception
    {
        XPathExpression idNodesExpr = XPathFactory.newInstance().newXPath().compile("//" + PATH + "[@" + PATH_ID + "='" + pathId + "']");
        return this.getOrCreatePathAncestors(idNodesExpr);
    }

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
}

package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.XMLTemplate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bas on 23.02.15.
 */
public class XMLMapper implements URLMapper
{
    private static XMLMapper instance;

    private Document urlIdMapping;

    private XMLMapper(Document urlIdMapping)
    {
        this.urlIdMapping = urlIdMapping;
    }

    public static XMLMapper getInstance()
                    throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException, DatabaseException, IDException, LanguageException
    {
        if(instance == null){
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            BlocksID xmlMappingId = getNewVersionXMLMappingId();
            XMLTemplate storedXml = (XMLTemplate) Redis.getInstance().fetchLastVersion(xmlMappingId, XMLTemplate.class);
            Document document;
            if(storedXml == null){
                //start with an empty url-mapping xml-string
                document = builder.parse(IOUtils.toInputStream("<?xml version=\"1.0\"?>\n" +
                                         "<urls>\n" +
                                         "</urls>"));
            }
            else{
                document = builder.parse(IOUtils.toInputStream(storedXml.getTemplate()));
            }
            instance = new XMLMapper(document);
            instance.writeOut();
            instance.add(new URL(BlocksConfig.getSiteDomain()), new BlocksID("blocks://LOC/blabla:123123/en"));
        }
        return instance;
    }

    @Override
    public BlocksID getId(URL url)
    {
        return null;
    }
    @Override
    public URL getTranslation(URL url, String language)
    {
        return null;
    }
    @Override
    public URL getUrl(BlocksID id)
    {
        return null;
    }
    @Override
    public void add(URL languagedUrl, BlocksID id) throws LanguageException
    {
        if(languagedUrl == null){
            throw new NullPointerException("Cannot add null url to url-id mapping.");
        }
        if(id == null){
            throw new NullPointerException("Cannot add null id to url-id mapping.");
        }
        String language = Languages.determineLanguage(languagedUrl.toString());
        if(StringUtils.isEmpty(language)){
            throw new LanguageException("Cannot add url-id pair to mapping: could not find language in url '" + languagedUrl + "'.");
        }


    }
    @Override
    public void addTranslation(URL url, String language)
    {
        //            List<Path> paths = new ArrayList<>();
        //            XPath xPath = XPathFactory.newInstance().newXPath();
        //            XPathExpression pathNodesExpr = xPath.compile("//path");
        //            NodeList pathNodes = (NodeList) pathNodesExpr.evaluate(document, XPathConstants.NODESET);
        //            for(int i = 0; i<pathNodes.getLength(); i++){
        //                Node pathNode = pathNodes.item(i);
        //                if(pathNode instanceof Element){
        //                    Element pathElement = (Element) pathNode;
        //
        //                    Path path = new Path(pathElement.getAttribute("id"));
        //
        //                    XPathExpression translationsExpr = xPath.compile("child::translations/child::translation");
        //                    NodeList translationNodes = (NodeList) translationsExpr.evaluate(pathElement, XPathConstants.NODESET);
        //                    for(int j = 0; j<translationNodes.getLength(); j++){
        //                        if(translationNodes.item(j) instanceof Element){
        //                            Element translationElement = (Element) translationNodes.item(j);
        //                            path.addTranslation(translationElement.getAttribute(ParserConstants.LANGUAGE), translationElement.getTextContent().trim());
        //                        }
        //                    }
        //                    paths.add(path);
        //                }
        //            }
        //            pathNodes.item(0).setTextContent("bla");
    }
    synchronized public void writeOut() throws TransformerException, FileNotFoundException, MalformedURLException, IDException, DatabaseException
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
        // apply the do-nothing transformation and send the output to a file
        DOMSource source = new DOMSource(urlIdMapping);
        Writer result = new StringWriter();
        t.transform(source, new StreamResult(result));

        /*
         * Save new version to db
         */
        BlocksID xmlId = getNewVersionXMLMappingId();
        XMLTemplate treeTemplate = new XMLTemplate(xmlId, result.toString(), DatabaseConstants.URL_ID_MAPPING);
        XMLTemplate storedXML = (XMLTemplate) Redis.getInstance().fetchLastVersion(xmlId, XMLTemplate.class);
        if(storedXML == null){
            Redis.getInstance().create(treeTemplate);
        }
        else if(!treeTemplate.equals(storedXML)){
            Redis.getInstance().update(treeTemplate);
        }
        else{
            //no need to save to db, since this is an unchanged version
        }
    }

    private static BlocksID getNewVersionXMLMappingId() throws MalformedURLException, IDException
    {
        return new BlocksID(new URL(BlocksConfig.getSiteDomain() + "/" + DatabaseConstants.URL_ID_MAPPING), BlocksID.NEW_VERSION, true);
    }
}

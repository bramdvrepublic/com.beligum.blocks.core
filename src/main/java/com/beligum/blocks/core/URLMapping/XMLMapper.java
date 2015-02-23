package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.URLMapping.URLMapper;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.core.framework.utils.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bas on 23.02.15.
 */
public class XMLMapper implements URLMapper
{
    private static XMLMapper instance;

    private Path treeRoot;

    private XMLMapper(Path treeRoot)
    {
        this.treeRoot = treeRoot;
    }

    public static XMLMapper getInstance() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException
    {
        if(instance == null){

            //TODO BAS SH: je hebt net een unsafe algoritme om alle paths te vinden, nu nog de db-id invullen en de path-boom opbouwen
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            File xml = new File("/home/bas/Projects/Workspace/idea/com.beligum.blocks/src/main/java/com/beligum/blocks/core/URLMapping/url-id-mapping.xml");
            Document document = builder.parse(xml);
            Element root = document.getDocumentElement();

            List<Path> paths = new ArrayList<>();
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression pathNodesExpr = xPath.compile("//path");
            NodeList pathNodes = (NodeList) pathNodesExpr.evaluate(document, XPathConstants.NODESET);
            for(int i = 0; i<pathNodes.getLength(); i++){
                Node pathNode = pathNodes.item(i);
                if(pathNode instanceof Element){
                    Element pathElement = (Element) pathNode;

                    Path path = new Path(pathElement.getAttribute("id"));

                    XPathExpression translationsExpr = xPath.compile("child::translations/child::translation");
                    NodeList translationNodes = (NodeList) translationsExpr.evaluate(pathElement, XPathConstants.NODESET);
                    for(int j = 0; j<translationNodes.getLength(); j++){
                        if(translationNodes.item(j) instanceof Element){
                            Element translationElement = (Element) translationNodes.item(j);
                            path.addTranslation(translationElement.getAttribute(ParserConstants.LANGUAGE), translationElement.getTextContent().trim());
                        }
                    }
                    paths.add(path);
                }
            }
            Path rootPath = paths.get(0);
            instance = new XMLMapper(rootPath);
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
    public void addUrl(URL url, String language, BlocksID id)
    {

    }
    @Override
    public void addTranslation(Path path, String language)
    {

    }
}

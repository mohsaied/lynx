package lynx.xml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlNoc {

    private static final Logger log = Logger.getLogger(XmlNoc.class.getName());

    public static Map<String, Integer> readXMLNoC(String nocPath) throws ParserConfigurationException, SAXException,
            IOException {

        log.info("Parsing NoC description file " + nocPath);

        Map<String, Integer> varMap = new HashMap<String, Integer>();

        // Get the DOM Builder Factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Get the DOM Builder
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Load and Parse the XML document
        // document contains the complete XML as a Tree
        Document document = builder.parse(ClassLoader.getSystemResourceAsStream(nocPath));

        // Iterating through the nodes and extracting the data
        NodeList nodeList = document.getDocumentElement().getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            // We have encountered an xml tag
            Node node = nodeList.item(i);

            if (node instanceof Element && node.getNodeName().equals("parameter")) {

                String parName = node.getAttributes().getNamedItem("name").getNodeValue();
                varMap.put(parName, Integer.parseInt(node.getAttributes().getNamedItem("value").getNodeValue()));
            }
        }

        log.info("Found NoC with width=" + varMap.get("width") + ", num_routers=" + varMap.get("num_routers") + ", num_vcs=" + varMap.get("num_vcs")
                + ", vc_depth=" + varMap.get("vc_depth"));

        return varMap;
    }
}

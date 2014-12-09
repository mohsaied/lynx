package lynx.xml;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lynx.data.Noc;

public class XmlNoc {

    private static final Logger log = Logger.getLogger(XmlNoc.class.getName());

    public static Noc readXMLNoC(String nocPath) throws ParserConfigurationException, SAXException, IOException {

        log.info("Parsing NoC description file " + nocPath);

        Noc noc = new Noc();

        // Get the DOM Builder Factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Get the DOM Builder
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Load and Parse the XML document
        // document contains the complete XML as a Tree
        Document document = builder.parse(ClassLoader.getSystemResourceAsStream(nocPath));

        int nocWidth = -1;
        int nocNumRouters = -1;
        int nocNumVcs = -1;
        int nocVcDepth = -1;

        // Iterating through the nodes and extracting the data
        NodeList nodeList = document.getDocumentElement().getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            // We have encountered an xml tag
            Node node = nodeList.item(i);

            if (node instanceof Element && node.getNodeName().equals("parameter")) {

                switch (node.getAttributes().getNamedItem("name").getNodeValue()) {
                case "width":
                    nocWidth = Integer.parseInt(node.getAttributes().getNamedItem("value").getNodeValue());
                    break;
                case "num_routers":
                    nocNumRouters = Integer.parseInt(node.getAttributes().getNamedItem("value").getNodeValue());
                    break;
                case "num_vcs":
                    nocNumVcs = Integer.parseInt(node.getAttributes().getNamedItem("value").getNodeValue());
                    break;
                case "vc_depth":
                    nocVcDepth = Integer.parseInt(node.getAttributes().getNamedItem("value").getNodeValue());
                    break;
                }
            }
        }

        log.info("Found NoC with width=" + nocWidth + ", num_routers=" + nocNumRouters + ", num_vcs=" + nocNumVcs
                + ", vc_depth=" + nocVcDepth);

        noc.configureNoC(nocWidth, nocNumRouters, nocNumVcs, nocVcDepth);

        return noc;
    }
}

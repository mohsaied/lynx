package lynx.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lynx.data.Design;
import lynx.data.InterfacePort;
import lynx.data.Module;
import lynx.data.ModulePort;
import lynx.data.Parameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLIO {

    private static final Logger log = Logger.getLogger(XMLIO.class.getName());

    /**
     * Read an XML file describing a design, parse it and create a List of
     * modules that describes it.
     * 
     * @param designPath
     *            a string containing the path of the design
     * @return List a list of modules parsed from the xml file
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Design readXMLDesign(String designPath) throws ParserConfigurationException, SAXException,
            IOException {

        // Get the DOM Builder Factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Get the DOM Builder
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Load and Parse the XML document
        // document contains the complete XML as a Tree
        Document document = builder.parse(ClassLoader.getSystemResourceAsStream(designPath));

        // Iterating through the nodes and extracting the data
        NodeList nodeList = document.getDocumentElement().getChildNodes();

        String designName = document.getDocumentElement().getAttribute("name");

        Design design = new Design(designName);

        log.info("Parsing Design " + designName + " from file " + designPath);

        // will only parse modules in the first pass
        // could also disconnect them from my DOM after processing
        for (int i = 0; i < nodeList.getLength(); i++) {

            // We have encountered a <Module> tag
            Node node = nodeList.item(i);

            if (node instanceof Element && node.getNodeName().equals("module")) {
                String modName = node.getAttributes().getNamedItem("name").getNodeValue();
                String modType = node.getAttributes().getNamedItem("type").getNodeValue();

                Module mod = new Module(modType, modName);

                NodeList childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node cNode = childNodes.item(j);

                    // Identifying the child tag of Module encountered
                    if (cNode instanceof Element) {

                        // child tags can be either ports or parameters
                        switch (cNode.getNodeName()) {
                        case "parameter":
                            String name = cNode.getAttributes().getNamedItem("name").getNodeValue();
                            String value = cNode.getAttributes().getNamedItem("value").getNodeValue();
                            Parameter par = new Parameter(name, value);
                            mod.addParameter(par);
                            break;
                        case "port":
                            String pname = cNode.getAttributes().getNamedItem("name").getNodeValue();
                            String direction = cNode.getAttributes().getNamedItem("direction").getNodeValue();
                            int width = Integer.parseInt(cNode.getAttributes().getNamedItem("width").getNodeValue());
                            ModulePort por = new ModulePort(pname, direction, width, mod);
                            mod.addPort(por);
                            break;
                        }
                    }
                }

                // add to list of modules in this design
                design.addModule(mod);

                // TODO remove this node from the nodelist if possible
            }
        }

        // Now that all modules are created
        // parse connections and interfaces in second pass
        for (int i = 0; i < nodeList.getLength(); i++) {

            // We have encountered a <Module> tag
            Node node = nodeList.item(i);

            if (node instanceof Element) {

                if (node.getNodeName().equals("connection")) {

                    String[] start = node.getAttributes().getNamedItem("start").getNodeValue().split("\\.");
                    assert (start.length != 2);
                    String startMod = start[0];
                    String startPort = start[1];

                    String[] end = node.getAttributes().getNamedItem("end").getNodeValue().split("\\.");
                    assert (end.length != 2);
                    String endMod = end[0];
                    String endPort = end[1];

                    // fetch the ports
                    ModulePort startPor = design.getModuleByName(startMod).getPortByName(startPort);
                    ModulePort endPor = design.getModuleByName(endMod).getPortByName(endPort);

                    // add connection
                    startPor.addConnection(endPor);
                    endPor.addConnection(startPor);

                } else if (node.getNodeName().equals("interface")) {

                    String port[] = node.getAttributes().getNamedItem("port").getNodeValue().split("\\.");
                    assert (port.length != 2);
                    String porMod = port[0];
                    String porPort = port[1];
                    String direction = node.getAttributes().getNamedItem("direction").getNodeValue();
                    String name = node.getAttributes().getNamedItem("name").getNodeValue();

                    ModulePort actualPort = design.getModuleByName(porMod).getPortByName(porPort);

                    InterfacePort intPort = new InterfacePort(name, direction, actualPort);

                    design.addInterfacePort(intPort);
                }
            }
        }

        return design;

    }

    /**
     * Write a list of modules to an xml output file
     * 
     * @param design
     *            a Design object
     * @param outputFileName
     *            string containing path of output file
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public static void writeXMLDesign(Design design, String outputFileName) throws ParserConfigurationException,
            TransformerException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // root element is called <design>
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement("design");
        rootElement.setAttribute("name", design.getName());
        doc.appendChild(rootElement);

        log.info("Writing Design " + design.getName() + " to " + outputFileName);

        Map<String, Module> modList = design.getModules();

        List<Element> connectionElements = new ArrayList<Element>();

        for (Module mod : modList.values()) {

            // new <module> tag
            Element modElement = doc.createElement("module");
            // set name and type - both are required
            modElement.setAttribute("type", mod.getType());
            modElement.setAttribute("name", mod.getName());

            // loop over parameters
            for (Parameter par : mod.getParameters()) {
                Element parElement = doc.createElement("parameter");
                parElement.setAttribute("name", par.getName());
                parElement.setAttribute("value", par.getValue());
                modElement.appendChild(parElement);
            }

            // loop over ports
            Map<String, ModulePort> porList = mod.getPorts();
            for (ModulePort por : porList.values()) {
                Element porElement = doc.createElement("port");
                porElement.setAttribute("name", por.getName());
                porElement.setAttribute("direction", por.getDirection());
                porElement.setAttribute("width", Integer.toString(por.getWidth()));
                modElement.appendChild(porElement);

                // find the list of connections
                // only go over input ports and find connections
                if (por.getDirection().equals("input"))
                    for (ModulePort con : por.getConnections()) {
                        Element conElement = doc.createElement("connection");
                        conElement.setAttribute("start", con.getFullName());
                        conElement.setAttribute("end", por.getFullName());
                        connectionElements.add(conElement);
                    }

            }

            rootElement.appendChild(modElement);
        }

        for (Element conElement : connectionElements) {
            rootElement.appendChild(conElement);
        }

        for (InterfacePort intPor : design.getInterfacePorts()) {
            Element intPorElement = doc.createElement("interface");
            intPorElement.setAttribute("port", intPor.getPhysicalPort().getFullName());
            intPorElement.setAttribute("direction", intPor.getDirection());
            intPorElement.setAttribute("name", intPor.getName());
            rootElement.appendChild(intPorElement);
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputFileName));

        transformer.transform(source, result);
    }

}

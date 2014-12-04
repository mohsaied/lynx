package nocsys.xml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nocsys.data.Design;
import nocsys.data.Module;
import nocsys.data.Parameter;
import nocsys.data.Port;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLIO {

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

        Design design = new Design();

        // Iterating through the nodes and extracting the data
        NodeList nodeList = document.getDocumentElement().getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {

            // We have encountered a <Module> tag
            Node node = nodeList.item(i);

            if (node instanceof Element) {
                Module mod = new Module();
                mod.setName(node.getAttributes().getNamedItem("name").getNodeValue());
                mod.setType(node.getAttributes().getNamedItem("type").getNodeValue());

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
                            String type = cNode.getAttributes().getNamedItem("type").getNodeValue();
                            int width = Integer.parseInt(cNode.getAttributes().getNamedItem("width").getNodeValue());
                            Port por = new Port(pname, type, width);
                            mod.addPort(por);
                            break;
                        }
                    }
                }

                // add to list of modules in this design
                design.addModule(mod);
            }
        }

        return design;

    }

    /**
     * Write a list of modules to an xml output file
     * 
     * @param design
     *            List<Module> of modules that define the design
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
        doc.appendChild(rootElement);

        List<Module> modList = design.getModules();

        for (int i = 0; i < modList.size(); i++) {
            // new <module> tag
            Element modElement = doc.createElement("module");
            // set name and type - both are required
            modElement.setAttribute("type", modList.get(i).getType());
            modElement.setAttribute("name", modList.get(i).getName());

            // loop over parameters
            List<Parameter> parList = modList.get(i).getParameters();
            for (int j = 0; j < parList.size(); j++) {
                Element parElement = doc.createElement("parameter");
                parElement.setAttribute("name", parList.get(j).getName());
                parElement.setAttribute("value", parList.get(j).getValue());
                modElement.appendChild(parElement);
            }

            // loop over ports
            List<Port> porList = modList.get(i).getPorts();
            for (int j = 0; j < porList.size(); j++) {
                Element porElement = doc.createElement("port");
                porElement.setAttribute("name", porList.get(j).getName());
                porElement.setAttribute("type", porList.get(j).getType());
                porElement.setAttribute("width", Integer.toString(porList.get(j).getWidth()));
                modElement.appendChild(porElement);
            }

            rootElement.appendChild(modElement);
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

    public static void main(String[] args) throws Exception {

        Design design = readXMLDesign("designs/quadratic.xml");

        writeXMLDesign(design, "designs/out.xml");

        // Printing the Module list populated.
        System.out.println(design);

    }
}

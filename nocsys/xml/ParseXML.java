package nocsys.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nocsys.data.Module;
import nocsys.data.Parameter;
import nocsys.data.Port;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ParseXML {
    public static void main(String[] args) throws Exception {
        // Get the DOM Builder Factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        // Get the DOM Builder
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Load and Parse the XML document
        // document contains the complete XML as a Tree.
        Document document = builder.parse(ClassLoader.getSystemResourceAsStream("designs/quadratic.xml"));

        List<Module> modList = new ArrayList<>();

        // Iterating through the nodes and extracting the data.
        NodeList nodeList = document.getDocumentElement().getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {

            // We have encountered an <Module> tag.
            Node node = nodeList.item(i);

            if (node instanceof Element) {
                Module mod = new Module();
                mod.setName(node.getAttributes().getNamedItem("name").getNodeValue());
                mod.setType(node.getAttributes().getNamedItem("type").getNodeValue());

                NodeList childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node cNode = childNodes.item(j);

                    // Identifying the child tag of Module encountered.
                    if (cNode instanceof Element) {
                        
                        switch(cNode.getNodeName()){
                        case "parameter":
                            String name = cNode.getAttributes().getNamedItem("name").getNodeValue();
                            String value = cNode.getAttributes().getNamedItem("value").getNodeValue();
                            Parameter par = new Parameter(name,value);
                            mod.addParameter(par);
                            break;
                        case "port":
                            String pname = cNode.getAttributes().getNamedItem("name").getNodeValue();
                            String type = cNode.getAttributes().getNamedItem("type").getNodeValue();
                            int width = Integer.parseInt(cNode.getAttributes().getNamedItem("width").getNodeValue());
                            Port por = new Port(pname,type,width);
                            mod.addPort(por);
                            break;
                        }
                    }
                }
                modList.add(mod);
            }
        }

        // Printing the Module list populated.
        System.out.println("Modules:\n");
        for (Module mod : modList) {
            System.out.println(mod);
        }

    }
}

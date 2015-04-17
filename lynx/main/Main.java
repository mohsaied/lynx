package lynx.main;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import lynx.clustering.NocClustering;
import lynx.data.Design;
import lynx.graphics.Gui;
import lynx.interconnect.NocInterconnect;
import lynx.log.MyLogger;
import lynx.nocmapping.NocMapping;
import lynx.xml.XmlDesign;

public class Main {

    @SuppressWarnings("unused")
    public final static void main(String[] args) throws Exception {

        if (args.length == 0) {
            // bring up the GUI
            Gui gui = new Gui(null);
        } else if (args[0].equals("-c")) {
            // command line requested -- second argument is the designpath
            String filePath = args[1];
            runFlow(filePath);
        }

        // MyLogger log = new MyLogger(Level.INFO);
        MyLogger parentLog = new MyLogger(Level.ALL);

    }

    private static void runFlow(String filePath) throws ParserConfigurationException, SAXException, IOException,
            TransformerException {

        ProgramData.getInstance().setDesignFile(new File(filePath));

        // read XML design
        Design design = XmlDesign.readXMLDesign(filePath);
        design.update();

        // add NoC circuitry - NoC and translators
        NocInterconnect.addNoc(design, "designs/noc.xml");

        // cluster design into SCCs
        NocClustering.clusterDesign(design);

        // find possible locations on the NoC
        NocMapping.findMappings(design);

        // write out XML design
        XmlDesign.writeXMLDesign(design, "designs/out.xml");

        // write out verilog design
        // VerilogOut.writeVerilogDesign(design);

    }

}

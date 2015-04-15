package lynx.main;

import java.util.logging.Level;

import lynx.data.Design;
import lynx.graphics.Gui;
import lynx.interconnect.NocInterconnect;
import lynx.clustering.NocClustering;
import lynx.log.MyLogger;
import lynx.nocmapping.NocMapping;
import lynx.xml.XmlDesign;
import lynx.verilog.VerilogOut;

public class Main {

    public final static void main(String[] args) throws Exception {

        // draw fcns
        Gui gui = new Gui(null);

        @SuppressWarnings("unused")
        // MyLogger log = new MyLogger(Level.INFO);
        MyLogger parentLog = new MyLogger(Level.ALL);

        // read XML design
        Design design = XmlDesign.readXMLDesign("designs/tarjan_test.xml");
        design.update();

        // add NoC circuitry - NoC and translators
        NocInterconnect.addNoc(design, "designs/noc.xml");

        // cluster design into SCCs
        NocClustering.clusterDesign(design);

        // find possible locations on the NoC
        NocMapping.findMappings(design);

        gui.setDesign(design);

        // write out XML design
        XmlDesign.writeXMLDesign(design, "designs/out.xml");

        // write out verilog design
        VerilogOut.writeVerilogDesign(design);

    }

}

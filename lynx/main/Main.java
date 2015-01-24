package lynx.main;

import java.util.logging.Level;

import lynx.data.Design;
import lynx.graphics.Utils;
import lynx.interconnect.NocInterconnect;
import lynx.interconnect.NocMapping;
import lynx.log.MyLogger;
import lynx.xml.XmlDesign;
import lynx.verilog.VerilogOut;

public class Main {

    public final static void main(String[] args) throws Exception {

        @SuppressWarnings("unused")
        // MyLogger log = new MyLogger(Level.INFO);
        MyLogger log = new MyLogger(Level.ALL);

        // read XML design
        Design design = XmlDesign.readXMLDesign("designs/ram_big.xml");
        design.update();

        // add NoC circuitry - NoC and translators
        NocInterconnect.addNoc(design, "designs/noc.xml");

        // find possible locations on the NoC
        long startTime = System.nanoTime();
        NocMapping.findMappings(design);
        long endTime = System.nanoTime();
        System.out.println("Elapsed Time = " + (endTime - startTime) / 1e9 + " seconds");

        // write out XML design
        XmlDesign.writeXMLDesign(design, "designs/out.xml");

        // write out verilog design
        VerilogOut.writeVerilogDesign(design);

        // draw fcns
        Utils.initWindow(design);

        // Printing the current design
        // System.out.println(design);
    }

}

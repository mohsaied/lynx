package lynx.main;

import java.util.logging.Level;

import lynx.data.Design;
import lynx.interconnect.NocInterconnect;
import lynx.log.MyLogger;
import lynx.xml.XmlDesign;
import lynx.verilog.VerilogOut;

public class Main {

    public static void main(String[] args) throws Exception {

        @SuppressWarnings("unused")
        MyLogger log = new MyLogger(Level.INFO);

        // read XML design
        Design design = XmlDesign.readXMLDesign("designs/quadratic.xml");

        // add NoC circuitry - NoC and translators
        NocInterconnect.addNoc(design, "designs/noc.xml");

        // write out XML design
        XmlDesign.writeXMLDesign(design, "designs/out.xml");

        // write out verilog design
        VerilogOut.writeVerilogDesign(design);

        // Printing the current design
        // System.out.println(design);
    }

}

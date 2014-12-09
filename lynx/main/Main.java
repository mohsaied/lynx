package lynx.main;

import java.util.logging.Level;

import lynx.data.Design;
import lynx.log.MyLogger;
import lynx.noc.Interconnect;
import lynx.xml.XmlDesign;
import lynx.verilog.VerilogOut;

public class Main {

    public static void main(String[] args) throws Exception {

        @SuppressWarnings("unused")
        MyLogger log = new MyLogger(Level.INFO);

        Design design = XmlDesign.readXMLDesign("designs/quadratic.xml");

        Interconnect.addNoc(design);

        XmlDesign.writeXMLDesign(design, "designs/out.xml");

        VerilogOut.writeVerilogDesign(design);

        // Printing the current design
        // System.out.println(design);

    }

}

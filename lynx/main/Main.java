package lynx.main;

import java.util.logging.Level;

import lynx.data.Design;
import lynx.log.MyLogger;
import lynx.noc.Interconnect;
import lynx.xml.XMLIO;
import lynx.verilog.VerilogOut;

public class Main {

    public static void main(String[] args) throws Exception {

        @SuppressWarnings("unused")
        MyLogger log = new MyLogger(Level.INFO);

        Design design = XMLIO.readXMLDesign("designs/quadratic.xml");

        Interconnect.addNoc(design);

        XMLIO.writeXMLDesign(design, "designs/out.xml");

        VerilogOut.writeVerilogDesign(design);

        // Printing the current design
        // System.out.println(design);

    }

}

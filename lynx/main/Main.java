package lynx.main;

import java.util.logging.Level;

import lynx.data.Design;
import lynx.log.MyLogger;
import lynx.xml.XMLIO;

public class Main {

    public static void main(String[] args) throws Exception {

        @SuppressWarnings("unused")
        MyLogger log = new MyLogger(Level.INFO);

        Design design = XMLIO.readXMLDesign("designs/quadratic.xml");

        XMLIO.writeXMLDesign(design, "designs/out.xml");

        // Printing the current design
        System.out.println(design);

    }

}

package noclynx.main;

import java.util.logging.Level;

import noclynx.data.Design;
import noclynx.log.MyLogger;
import noclynx.xml.XMLIO;

public class Main {

    public static void main(String[] args) throws Exception {

        @SuppressWarnings("unused")
        MyLogger log = new MyLogger(Level.ALL);

        Design design = XMLIO.readXMLDesign("designs/quadratic.xml");

        XMLIO.writeXMLDesign(design, "designs/out.xml");

        // Printing the Module list populated.
        System.out.println(design);

    }

}

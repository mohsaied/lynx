package nocys.main;

import java.util.logging.Level;

import nocsys.data.Design;
import nocsys.xml.XMLIO;
import nocys.log.MyLogger;

public class Main {

    public static void main(String[] args) throws Exception {

        @SuppressWarnings("unused")
        MyLogger log = new MyLogger(Level.OFF);

        Design design = XMLIO.readXMLDesign("designs/quadratic.xml");

        XMLIO.writeXMLDesign(design, "designs/out.xml");

        // Printing the Module list populated.
        System.out.println(design);

    }

}

package nocys.main;

import java.util.logging.Level;
import java.util.logging.Logger;

import nocsys.data.Design;
import nocsys.xml.XMLIO;

public class Main {

    public static void main(String[] args) throws Exception {
        
        Logger mainlog = Logger.getLogger(XMLIO.class.getName());

        mainlog.setLevel(Level.INFO);

        Design design = XMLIO.readXMLDesign("designs/quadratic.xml");

        XMLIO.writeXMLDesign(design, "designs/out.xml");

        // Printing the Module list populated.
        // System.out.println(design);

    }

}

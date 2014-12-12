package lynx.interconnect;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.Depacketizer;
import lynx.data.Design;
import lynx.data.Bundle;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.Packetizer;

/**
 * Utility class that adds NoC components and connects them to the design
 * 
 * @author Mohamed
 *
 */
public class NocInterconnect {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    public static void addNoc(Design design) throws ParserConfigurationException, SAXException, IOException {
        addNoc(design, null);
    }

    public static void addNoc(Design design, String nocPath) throws ParserConfigurationException, SAXException, IOException {

        // want to make sure that it's only called once
        assert design.getNoc() == null : "NoC is already defined, will not overwrite!";

        log.info("Adding NoC (fabric interface)");

        insertNoc(design, nocPath);

        log.info("Adding Translators and connecting them to modules");

        insertTranslators(design);

        log.info("Figuring out the best location of modules on the NoC...");

        // not implemented yet
        // after this step, each module/bundle should be associated to a router
        // currently it is user-provided

        log.info("Connecting to NoC");

        connectNoc(design);
    }

    private static void connectNoc(Design design) {
        // this method will only connect modules if they are already assigned
        // and so will assert if a module is not assigned yet

        for (DesignModule mod : design.getDesignModules().values()) {
            assert mod.getRouter() != -1 : "Attempting to connect module to NoC, but module wasn't mapped to a router!";

            // loop over bundles and connect their translators to the router
            int router = mod.getRouter();

            for (Bundle bun : mod.getBundles().values()) {
                bun.connectToRouter(router);
            }
        }

    }

    private static void insertNoc(Design design, String nocPath) throws ParserConfigurationException, SAXException, IOException {

        Noc noc;
        if (nocPath == null)
            noc = new Noc();
        else
            noc = new Noc(nocPath);

        design.setNoc(noc);
    }

    private static void insertTranslators(Design design) {

        // loop over all modules and insert translators
        for (DesignModule mod : design.getDesignModules().values()) {
            for (Bundle bun : mod.getBundles().values()) {
                switch (bun.getDirection()) {
                case OUTPUT:
                    insertPacketizer(bun, mod, design);
                    break;
                case INPUT:
                    insertDepacketizer(bun, mod, design);
                    break;
                default:
                    assert false : "Bundle direction was never set.";
                }
            }
        }
    }

    private static void insertPacketizer(Bundle bun, DesignModule mod, Design design) {
        Packetizer packetizer = new Packetizer(design.getNoc(), mod, bun);
        design.addTranslator(packetizer);
    }

    private static void insertDepacketizer(Bundle bun, DesignModule mod, Design design) {
        Depacketizer depacketizer = new Depacketizer(design.getNoc(), mod, bun);
        design.addTranslator(depacketizer);
    }

}

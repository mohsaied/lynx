package lynx.interconnect;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.Depacketizer;
import lynx.data.Design;
import lynx.data.Bundle;
import lynx.data.DesignModule;
import lynx.data.Module;
import lynx.data.Noc;
import lynx.data.Packetizer;
import lynx.data.Port;
import lynx.main.DesignData;

/**
 * Utility class that adds NoC components and connects them to the design
 * 
 * @author Mohamed
 *
 */
@SuppressWarnings("unused")
public class NocInterconnect {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    public static void addNoc() throws ParserConfigurationException, SAXException, IOException {
        addNoc(null);
    }

    public static void addNoc(String nocPath) throws ParserConfigurationException, SAXException, IOException {

        log.info("Adding NoC (fabric interface)");

        insertNoc(nocPath);

        log.info("Adding Translators and connecting them to modules");

        // insertTranslators(design);

        log.info("Connecting to NoC");

        // connectNoc(design);

        log.info("Inferring top-level ports, and connecting the wires to submodules");

        // inferTopLevelPorts(design);

    }

    private static void insertNoc(String nocPath) throws ParserConfigurationException, SAXException, IOException {

        Noc noc;
        if (nocPath == null)
            noc = new Noc();
        else
            noc = new Noc(nocPath);

        DesignData.getInstance().setNoc(noc);
    }

    private static void inferTopLevelPorts(Design design) {
        // loop over all modules and all ports - the ports that have a global
        // export
        for (Module mod : design.getAllModules()) {
            for (Port por : mod.getPorts().values()) {
                if (por.getGlobalPortName() != null) {
                    design.addPort(por);
                }
            }
        }
    }

    private static void connectNoc(Design design) {
        // this method will only connect modules if they are already assigned
        // and so will assert if a module is not assigned yet

        for (DesignModule mod : design.getDesignModules().values()) {
            // assert mod.getRouter() != -1 :
            // "Attempting to connect module to NoC, but module wasn't mapped to a router!";

            // loop over bundles and connect their translators to the router
            int router = mod.getRouter();

            for (Bundle bun : mod.getBundles().values()) {
                bun.connectToRouter(router);
            }
        }

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
        Packetizer packetizer = new Packetizer(DesignData.getInstance().getNoc(), mod, bun);
        design.addTranslator(packetizer);
    }

    private static void insertDepacketizer(Bundle bun, DesignModule mod, Design design) {
        Depacketizer depacketizer = new Depacketizer(DesignData.getInstance().getNoc(), mod, bun);
        design.addTranslator(depacketizer);
    }

}

package lynx.interconnect;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.Design;
import lynx.data.Bundle;
import lynx.data.DesignModule;
import lynx.data.Module;
import lynx.data.Noc;
import lynx.data.Translator;

/**
 * Utility class that adds NoC components and connects them to the design
 * 
 * @author Mohamed
 *
 */
public class NocInterconnect {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    public static void addNoc(Design design) throws ParserConfigurationException, SAXException, IOException {
        log.info("Adding NoC circuitry...");

        insertNocInterface(design);

        insertTranslators(design);
    }

    private static void insertNocInterface(Design design) throws ParserConfigurationException, SAXException,
            IOException {

        Noc nocInterface = new Noc("designs/noc.xml");
        design.setFabricInterface(nocInterface);
    }

    private static void insertTranslators(Design design) {

        // loop over all modules and insert translators
        for (DesignModule mod : design.getModules().values()) {
            for (Bundle bun : mod.getBundles()) {
                switch (bun.getDirection()) {
                case INPUT:
                    insertPacketizer(bun, mod, design);
                    break;
                case OUTPUT:
                    insertDepacketizer(bun, mod, design);
                    break;
                default:
                    assert false : "Bundle direction was never set.";
                }
            }
        }
    }

    private static void insertPacketizer(Bundle bun, Module mod, Design design) {
        Translator packetizer = new Translator(design.getFabricInterface(), mod, bun);
        design.addTranslator(packetizer);
    }

    private static void insertDepacketizer(Bundle bun, Module mod, Design design) {
        Translator depacketizer = new Translator(design.getFabricInterface(), mod, bun);
        design.addTranslator(depacketizer);
    }

}

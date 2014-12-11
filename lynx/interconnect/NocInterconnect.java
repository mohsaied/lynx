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

    public static void addNoc(Design design, String nocPath) throws ParserConfigurationException, SAXException,
            IOException {

        // want to make sure that it's only called once
        assert design.getNoc() == null : "NoC is already defined, will not overwrite!";

        log.info("Adding NoC (fabric interface)");

        insertNoc(design, nocPath);

        log.info("Adding Translators and connecting them to modules");

        insertTranslators(design);
    }

    private static void insertNoc(Design design, String nocPath) throws ParserConfigurationException, SAXException,
            IOException {

        Noc noc;
        if (nocPath == null)
            noc = new Noc();
        else
            noc = new Noc(nocPath);

        design.setNoc(noc);
    }

    private static void insertTranslators(Design design) {

        // loop over all modules and insert translators
        for (DesignModule mod : design.getModules().values()) {
            for (Bundle bun : mod.getBundles().values()) {
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
        Packetizer packetizer = new Packetizer(design.getNoc(), mod, bun);
        design.addTranslator(packetizer);
    }

    private static void insertDepacketizer(Bundle bun, Module mod, Design design) {
        Depacketizer depacketizer = new Depacketizer(design.getNoc(), mod, bun);
        design.addTranslator(depacketizer);
    }

}

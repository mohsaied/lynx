package lynx.interconnect;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.Depacketizer;
import lynx.data.Design;
import lynx.data.Bundle;
import lynx.data.DesignModule;
import lynx.data.Module;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.Packetizer;
import lynx.data.Port;
import lynx.main.DesignData;
import lynx.nocmapping.Mapping;

/**
 * Utility class that adds NoC components and connects them to the design
 * 
 * @author Mohamed
 *
 */
@SuppressWarnings("unused")
public class NocInterconnect {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    /**
     * Add default NoC to design
     * 
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static void addNoc() throws ParserConfigurationException, SAXException, IOException {
        addNoc(null);
    }

    /**
     * Parse an NoC description from n xml file
     * 
     * @param nocPath
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static void addNoc(String nocPath) throws ParserConfigurationException, SAXException, IOException {

        log.info("Adding NoC to design");

        insertNoc(nocPath);
    }

    /**
     * After mapping etc, call this function to connect the design modules to
     * NoC - that includes instantiating translators, connecting the translators
     * to the design, connecting translators to NoC, and connecting global
     * top-level ports
     * 
     * @param design
     * @param noc
     */
    public static void connectDesignToNoc(Design design, Noc noc) {

        Mapping mapping = DesignData.getInstance().getNocMapping();

        log.info("Adding Translators and connecting them to modules");

        insertTranslators(design, mapping);

        log.info("Connecting to NoC");

        // connectNoc(design);

        log.info("Inferring top-level ports");

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

    private static void insertTranslators(Design design, Mapping mapping) {

        // mapping contains bundle-nocbundle mapping
        Map<Bundle, List<NocBundle>> bundleMap = mapping.getBundleMap();
        for (Bundle bun : bundleMap.keySet()) {
            // insert and connect translators for bundles that are mapped onto
            // NoC - i.e. has nonzero list of nocbundles
            List<NocBundle> nocbuns = bundleMap.get(bun);
            if (nocbuns.size() != 0) {
                switch (bun.getDirection()) {
                case OUTPUT:
                    insertPacketizer(bun, design);
                    //bun.connectToRouter(nocbuns);
                    break;
                case INPUT:
                    insertDepacketizer(bun, design);
                    //bun.connectToRouter(nocbuns);
                    break;
                default:
                    assert false : "Bundle direction was never set.";
                }
            }
        }
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

    private static void insertPacketizer(Bundle bun, Design design) {
        Packetizer packetizer = new Packetizer(DesignData.getInstance().getNoc(), bun);
        design.addTranslator(packetizer);
    }

    private static void insertDepacketizer(Bundle bun, Design design) {
        Depacketizer depacketizer = new Depacketizer(DesignData.getInstance().getNoc(), bun);
        design.addTranslator(depacketizer);
    }

}

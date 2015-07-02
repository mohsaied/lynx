package lynx.interconnect;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.Depacketizer;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Bundle;
import lynx.data.Module;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.Packetizer;
import lynx.data.Port;
import lynx.elaboration.ConnectionGroup;
import lynx.main.DesignData;
import lynx.nocmapping.Mapping;

/**
 * Utility class that adds NoC components and connects them to the design
 * 
 * @author Mohamed
 *
 */
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
    public static void connectDesignToNoc(Design design, Noc noc, List<ConnectionGroup> cgList) {

        log.info("Creating and connecting simulation model");
        createAndConnectHollowSim(design, noc, cgList);

        log.info("Creating and connecting actual design");
        connectActualDesignToNoc(design, noc, cgList);
    }

    private static void createAndConnectHollowSim(Design design, Noc noc, List<ConnectionGroup> cgList) {

        Noc newNoc = noc.clone();
        HollowSim.CURRID = 0;

        Mapping mapping = DesignData.getInstance().getNocMapping();

        Design simulationDesign = new Design(design.getName(), newNoc);
        DesignData.getInstance().setSimulationDesign(simulationDesign);

        log.info("Adding srcs/sinks/vias instead of designmodules");
        Map<Bundle, Bundle> designToSimBundleMap = HollowSim.populateHollowSimDesign(design, simulationDesign, mapping);

        log.info("Configuring module clocks");
        configureModuleClocks(simulationDesign, newNoc, mapping, designToSimBundleMap);

        log.info("Adding Translators and connecting them to modules");
        HollowSim.insertSimulationTranslators(newNoc, simulationDesign, mapping, designToSimBundleMap);

        log.info("Adding traffic managers");
        HollowSim.insertTrafficManagers(newNoc, simulationDesign, designToSimBundleMap, mapping, cgList);

        log.info("Inferring top-level ports");
        inferTopLevelPorts(simulationDesign);
    }

    public static void connectActualDesignToNoc(Design design, Noc noc, List<ConnectionGroup> cgList) {
        Mapping mapping = DesignData.getInstance().getNocMapping();

        log.info("Configuring module clocks");
        configureModuleClocks(design, noc, mapping);

        log.info("Adding Translators and connecting them to modules");
        insertTranslators(noc, design, mapping);

        log.info("Inferring top-level ports");
        inferTopLevelPorts(design);

    }

    private static void configureModuleClocks(Design design, Noc noc, Mapping mapping) {
        for (DesignModule mod : design.getDesignModules().values()) {
            int router = mapping.getApproxRouterForModule(mod);
            if (router < noc.getNumRouters()) {
                mod.getClock().setGlobalOnNoc(true);
                mod.getClock().setGlobalPortName(noc.getModuleGlobalClockName(router));
            }
        }
    }

    private static void configureModuleClocks(Design simulationDesign, Noc noc, Mapping mapping,
            Map<Bundle, Bundle> designBundlesToSimBundlesMap) {
        for (DesignModule mod : simulationDesign.getDesignModules().values()) {
            DesignModule origMod = getOrigMod(mod, designBundlesToSimBundlesMap);
            int router = mapping.getApproxRouterForModule(origMod);
            if (router < noc.getNumRouters()) {
                mod.getClock().setGlobalOnNoc(true);
                mod.getClock().setGlobalPortName(noc.getModuleGlobalClockName(router));
            }
        }
    }

    private static DesignModule getOrigMod(DesignModule mod, Map<Bundle, Bundle> designBundlesToSimBundlesMap) {
        for (Bundle bun : designBundlesToSimBundlesMap.keySet()) {
            DesignModule currModule = bun.getParentModule();
            if (mod.getBundles().values().contains(designBundlesToSimBundlesMap.get(bun)))
                return currModule;
        }
        return null;
    }

    private static void insertNoc(String nocPath) throws ParserConfigurationException, SAXException, IOException {

        Noc noc;
        if (nocPath == null)
            noc = new Noc();
        else
            noc = new Noc(nocPath);

        DesignData.getInstance().setNoc(noc);
    }

    private static void insertTranslators(Noc noc, Design design, Mapping mapping) {

        // mapping contains bundle-nocbundle mapping
        Map<Bundle, List<NocBundle>> bundleMap = mapping.getBundleMap();
        for (Bundle bun : bundleMap.keySet()) {
            // insert and connect translators for bundles that are mapped onto
            // NoC - i.e. has nonzero list of nocbundles
            List<NocBundle> nocbuns = bundleMap.get(bun);
            if (nocbuns.size() != 0) {
                switch (bun.getDirection()) {
                case OUTPUT:
                    Packetizer packetizer = new Packetizer(noc, bun, nocbuns);
                    design.addTranslator(packetizer);
                    break;
                case INPUT:
                    Depacketizer depacketizer = new Depacketizer(noc, bun, nocbuns);
                    design.addTranslator(depacketizer);
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
                if (por.isGlobal() && !por.isGlobalOnNoc()) {
                    design.addPort(por);
                }
            }
        }
    }

}

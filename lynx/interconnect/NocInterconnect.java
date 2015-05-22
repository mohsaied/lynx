package lynx.interconnect;

import java.io.IOException;
import java.util.HashMap;
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
import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.Packetizer;
import lynx.data.Parameter;
import lynx.data.Port;
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
    private static int CURRID = 0;

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

        log.info("Creating and connecting simulation model");
        createAndConnectSimulationDesignToNoc(design, noc);

        log.info("Creating and connecting actual design");
        connectActualDesignToNoc(design, noc);
    }

    /**
     * Create a simulation model of the design by going over all designmodules,
     * replacing them with src/sink/via's and connecting them to noc with
     * translators
     * 
     * @param design
     * @param noc
     */
    public static void createAndConnectSimulationDesignToNoc(Design design, Noc noc) {

        Noc newNoc = noc.clone();

        Mapping mapping = DesignData.getInstance().getNocMapping();

        Design simulationDesign = new Design(design.getName(), newNoc);
        DesignData.getInstance().setSimulationDesign(simulationDesign);

        log.info("Adding srcs/sinks/vias instead or designmodules");
        Map<Bundle, Bundle> designToSimBundleMap = populateSimulationDesign(design, simulationDesign, mapping);
        createAndConnectHaltModule(simulationDesign);

        log.info("Configuring module clocks");
        configureModuleClocks(simulationDesign, newNoc, mapping, designToSimBundleMap);

        log.info("Adding Translators and connecting them to modules");
        insertSimulationTranslators(newNoc, simulationDesign, mapping, designToSimBundleMap);

        log.info("Inferring top-level ports");
        inferTopLevelPorts(simulationDesign);
    }

    /**
     * Replace designmodules with src/sink/vias
     * 
     * @param originalDesign
     * @param simulationDesign
     * @param mapping
     * @return
     */
    private static Map<Bundle, Bundle> populateSimulationDesign(Design originalDesign, Design simulationDesign, Mapping mapping) {
        Map<Bundle, Bundle> bbMap = new HashMap<Bundle, Bundle>();

        Noc noc = simulationDesign.getNoc();

        // loop over bundles that are mapped to NoC
        for (DesignModule mod : originalDesign.getDesignModules().values()) {
            // first we need to determine whether this is a source,sink or via
            int numSrc = mod.getBundles(Direction.OUTPUT).size();
            int numSink = mod.getBundles(Direction.INPUT).size();

            if (numSrc == 0 && numSink == 0) // empty module?
                continue;

            if (numSink == 0) { // we're a src/tpg
                for (Bundle bun : mod.getBundles().values()) {
                    DesignModule src = createSrcModule(noc, bun, mapping, bbMap);
                    simulationDesign.addModule(src);
                }
            } else if (numSrc == 0) { // we're a sink/ora
                for (Bundle bun : mod.getBundles().values()) {
                    DesignModule sink = createSinkModule(noc, bun, mapping, bbMap);
                    simulationDesign.addModule(sink);
                }
            } else { // this is a via
                DesignModule src = createViaModule(noc, mod, mapping, bbMap);
                simulationDesign.addModule(src);
            }
        }

        return bbMap;
    }

    private static void createAndConnectHaltModule(Design design) {
        DesignModule halt = new DesignModule("halt_sim", "halter");
        Port haltPort = new Port("done", Direction.INPUT, 1, PortType.DONE, halt);
        halt.addPort(haltPort);

        // loop over all designmodules (which are srcs/sinks and vias) and
        // connect their done signal to the halter
        for (DesignModule mod : design.getDesignModules().values()) {
            Port por = mod.getPortByName("done");
            haltPort.addWire(por);
            por.addWire(haltPort);
        }

        design.setHaltModule(halt);
    }

    private static DesignModule createViaModule(Noc noc, DesignModule mod, Mapping mapping, Map<Bundle, Bundle> bbMap) {
        int numSrc = mod.getBundles(Direction.OUTPUT).size();
        int numSink = mod.getBundles(Direction.INPUT).size();
        DesignModule via = new DesignModule("via_" + numSink + "_" + numSrc, "via_" + mod.getName());

        // fixed parameters
        via.addParameter(new Parameter("N", noc.getNumRouters()));
        via.addParameter(new Parameter("NODE", mapping.getApproxRouterForModule(mod)));

        // fixed ports
        via.addPort(new Port("clk", Direction.INPUT, PortType.CLK, via, "clk"));
        via.addPort(new Port("rst", Direction.INPUT, PortType.RST, via, "rst"));
        via.addPort(new Port("done", Direction.OUTPUT, 1, PortType.DONE, via));

        // numbun-dependant parameters and ports
        int num = 0;
        for (Bundle bun : mod.getBundles(Direction.INPUT)) {
            via.addParameter(new Parameter("i" + num + "_WIDTH", bun.getWidth()));
            via.addParameter(new Parameter("i" + num + "_ID", CURRID++));

            // create bundle ports
            Port dataPort = new Port("i" + num + "_data_in", Direction.INPUT, bun.getWidth(), via);
            Port validPort = new Port("i" + num + "_valid_in", Direction.INPUT, via);
            Port readyPort = new Port("i" + num + "_ready_out", Direction.OUTPUT, via);

            // create bundle and add ports to it
            Bundle newBun = new Bundle("i" + num + "_inbun_" + num, via);
            newBun.addDataPort(dataPort);
            newBun.setValidPort(validPort);
            newBun.setReadyPort(readyPort);

            // add both bundle and ports to mod
            via.addBundle(newBun);
            bbMap.put(bun, newBun);
            num++;
        }
        num = 0;
        for (Bundle bun : mod.getBundles(Direction.OUTPUT)) {
            via.addParameter(new Parameter("o" + num + "_WIDTH", bun.getWidth()));
            via.addParameter(new Parameter("o" + num + "_ID", CURRID++));
            via.addParameter(new Parameter("o" + num + "_DEST", mapping.getRouter(bun.getConnections().get(0))));

            // create bundle ports
            Port dataPort = new Port("o" + num + "_data_out", Direction.OUTPUT, bun.getWidth(), via);
            Port destPort = new Port("o" + num + "_dest_out", Direction.OUTPUT, noc.getAddressWidth(), via);
            Port validPort = new Port("o" + num + "_valid_out", Direction.OUTPUT, via);
            Port readyPort = new Port("o" + num + "_ready_in", Direction.INPUT, via);

            // create bundle and add ports to it
            Bundle newBun = new Bundle("o" + num + "_outbun_" + num, via);
            newBun.addDataPort(dataPort);
            newBun.setDstPort(destPort);
            newBun.setValidPort(validPort);
            newBun.setReadyPort(readyPort);

            // add both bundle and ports to mod
            via.addBundle(newBun);
            bbMap.put(bun, newBun);
            num++;
        }
        return via;
    }

    private static DesignModule createSrcModule(Noc noc, Bundle bun, Mapping mapping, Map<Bundle, Bundle> bbMap) {
        DesignModule src = new DesignModule("src", "src_" + bun.getFullNameDash());

        // add parameters
        src.addParameter(new Parameter("WIDTH", bun.getWidth()));
        src.addParameter(new Parameter("N", noc.getNumRouters()));
        src.addParameter(new Parameter("ID", CURRID++));
        src.addParameter(new Parameter("NODE", mapping.getRouter(bun)));
        src.addParameter(new Parameter("DEST", mapping.getRouter(bun.getConnections().get(0))));

        // add clk/rst ports
        src.addPort(new Port("clk", Direction.INPUT, PortType.CLK, src, "clk"));
        src.addPort(new Port("rst", Direction.INPUT, PortType.RST, src, "rst"));
        src.addPort(new Port("done", Direction.OUTPUT, 1, PortType.DONE, src));

        // create bundle ports
        Port dataPort = new Port("data_out", Direction.OUTPUT, bun.getWidth(), src);
        Port destPort = new Port("dest_out", Direction.OUTPUT, noc.getAddressWidth(), src);
        Port validPort = new Port("valid_out", Direction.OUTPUT, src);
        Port readyPort = new Port("ready_in", Direction.INPUT, src);

        // create bundle and add ports to it
        Bundle newBun = new Bundle("outbun", src);
        newBun.addDataPort(dataPort);
        newBun.setDstPort(destPort);
        newBun.setValidPort(validPort);
        newBun.setReadyPort(readyPort);

        // add both bundle and ports to mod
        src.addBundle(newBun);

        // now we want to add the newbun to the bbMap
        bbMap.put(bun, newBun);

        return src;
    }

    private static DesignModule createSinkModule(Noc noc, Bundle bun, Mapping mapping, Map<Bundle, Bundle> bbMap) {
        DesignModule sink = new DesignModule("sink", "sink_" + bun.getFullNameDash());

        // add parameters
        sink.addParameter(new Parameter("WIDTH", bun.getWidth()));
        sink.addParameter(new Parameter("N", noc.getNumRouters()));
        sink.addParameter(new Parameter("ID", CURRID++));
        sink.addParameter(new Parameter("NODE", mapping.getRouter(bun)));

        // add clk/rst ports
        sink.addPort(new Port("clk", Direction.INPUT, PortType.CLK, sink, "clk"));
        sink.addPort(new Port("rst", Direction.INPUT, PortType.RST, sink, "rst"));
        sink.addPort(new Port("done", Direction.OUTPUT, 1, PortType.DONE, sink));

        // create bundle ports
        Port dataPort = new Port("data_in", Direction.INPUT, bun.getWidth(), sink);
        Port validPort = new Port("valid_in", Direction.INPUT, sink);
        Port readyPort = new Port("ready_out", Direction.OUTPUT, sink);

        // create bundle and add ports to it
        Bundle newBun = new Bundle("inbun", sink);
        newBun.addDataPort(dataPort);
        newBun.setValidPort(validPort);
        newBun.setReadyPort(readyPort);

        // add both bundle and ports to mod
        sink.addBundle(newBun);

        // now we want to add the newbun to the bbMap
        bbMap.put(bun, newBun);

        return sink;
    }

    public static void connectActualDesignToNoc(Design design, Noc noc) {
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
                    insertPacketizer(noc, design, bun, nocbuns);
                    break;
                case INPUT:
                    insertDepacketizer(noc, design, bun, nocbuns);
                    break;
                default:
                    assert false : "Bundle direction was never set.";
                }
            }
        }
    }

    private static void insertSimulationTranslators(Noc noc, Design design, Mapping mapping, Map<Bundle, Bundle> bbMap) {

        // mapping contains bundle-nocbundle mapping
        Map<Bundle, List<NocBundle>> bundleMap = mapping.getBundleMap();
        for (Bundle origBun : bundleMap.keySet()) {
            // this is the bundle in our src/sink/via modules
            Bundle bun = bbMap.get(origBun);
            // insert and connect translators for bundles that are mapped onto
            // NoC - i.e. has nonzero list of nocbundles
            List<NocBundle> nocbuns = bundleMap.get(origBun);
            if (nocbuns.size() != 0) {
                switch (bun.getDirection()) {
                case OUTPUT:
                    insertPacketizer(noc, design, bun, nocbuns);
                    break;
                case INPUT:
                    insertDepacketizer(noc, design, bun, nocbuns);
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

    private static void insertPacketizer(Noc noc, Design design, Bundle bun, List<NocBundle> nocbuns) {
        Packetizer packetizer = new Packetizer(noc, bun, nocbuns);
        design.addTranslator(packetizer);
    }

    private static void insertDepacketizer(Noc noc, Design design, Bundle bun, List<NocBundle> nocbuns) {
        Depacketizer depacketizer = new Depacketizer(noc, bun, nocbuns);
        design.addTranslator(depacketizer);
    }

}

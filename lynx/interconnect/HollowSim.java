package lynx.interconnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.Parameter;
import lynx.data.Port;
import lynx.data.MyEnums.ConnectionType;
import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.elaboration.ConnectionGroup;
import lynx.elaboration.Elaboration;
import lynx.main.DesignData;
import lynx.nocmapping.AnnealBundleStruct;
import lynx.nocmapping.Mapping;
import lynx.vcmapping.VcMap;

/**
 * Includes subroutines that pertain to creating a hollow simulation
 * 
 * @author Mohamed
 *
 */
public class HollowSim {

    private static final Logger log = Logger.getLogger(HollowSim.class.getName());

    protected static int CURRID = 0;

    protected static void createAndConnectHollowSim(Design design, Noc noc, List<ConnectionGroup> cgList, VcMap vcMap) {

        HollowSim.CURRID = 0;

        Mapping mapping = DesignData.getInstance().getNocMapping();

        // clone the data structures for the hollowsim
        Noc nocClone = noc.clone();
        Mapping mappingClone;
        VcMap vcMapClone;
        List<ConnectionGroup> cgListClone;

        Design simulationDesign = new Design(design.getName(), nocClone);
        DesignData.getInstance().setSimulationDesign(simulationDesign);

        log.info("Adding srcs/sinks/vias instead of designmodules");
        Map<Bundle, Bundle> designToSimBundleMap = populateHollowSimDesign(design, simulationDesign, mapping, vcMap);

        mappingClone = cloneMappingSimBundles(simulationDesign, nocClone, mapping, designToSimBundleMap);
        vcMapClone = cloneVcMapSimBundles(vcMap, designToSimBundleMap);
        cgListClone = cloneCgListSimBundles(simulationDesign, designToSimBundleMap);

        log.info("Configuring module clocks");
        NocInterconnect.configureModuleClocks(simulationDesign, nocClone, mappingClone);

        log.info("Adding Translators and connecting them to modules");
        NocInterconnect.insertTranslators(nocClone, simulationDesign, mappingClone, vcMapClone);

        log.info("Adding traffic managers");
        NocInterconnect.insertTrafficManagers(nocClone, simulationDesign, mappingClone, cgListClone);

        log.info("Inferring top-level ports");
        NocInterconnect.inferTopLevelPorts(simulationDesign);
    }

    /**
     * Replace designmodules with src/sink/vias
     * 
     * @param originalDesign
     * @param simulationDesign
     * @param mapping
     * @return
     */
    protected static Map<Bundle, Bundle> populateHollowSimDesign(Design originalDesign, Design simulationDesign, Mapping mapping,
            VcMap vcMap) {
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
                    DesignModule src = createSrcModule(noc, bun, mapping, vcMap, bbMap);
                    simulationDesign.addModule(src);
                }
            } else if (numSrc == 0) { // we're a sink/ora
                for (Bundle bun : mod.getBundles().values()) {
                    DesignModule sink = createSinkModule(noc, bun, mapping, bbMap);
                    simulationDesign.addModule(sink);
                }
            } else { // this is a via
                DesignModule src = createViaModule(noc, mod, mapping, vcMap, bbMap);
                simulationDesign.addModule(src);
            }
        }

        connectSimulationDesign(simulationDesign, bbMap);

        createAndConnectHaltModule(simulationDesign);

        return bbMap;
    }

    private static void connectSimulationDesign(Design simulationDesign, Map<Bundle, Bundle> bbMap) {

        // loop over bb map
        for (Bundle origBun : bbMap.keySet()) {
            Bundle simBun = bbMap.get(origBun);
            // find connections in origBun
            for (Bundle conOrigBun : origBun.getConnections()) {
                simBun.addConnection(bbMap.get(conOrigBun));
            }
        }

        simulationDesign.update();
    }

    protected static DesignModule createViaModule(Noc noc, DesignModule mod, Mapping mapping, VcMap vcMap,
            Map<Bundle, Bundle> bbMap) {
        int numSrc = mod.getBundles(Direction.OUTPUT).size();
        int numSink = mod.getBundles(Direction.INPUT).size();
        DesignModule via = new DesignModule("via_" + numSrc + "_" + numSink, "via_" + mod.getName());

        // fixed parameters
        via.addParameter(new Parameter("N", noc.getNumRouters()));
        via.addParameter(new Parameter("NUM_VC", noc.getNumVcs()));

        // fixed ports
        via.addPort(new Port("clk", Direction.INPUT, PortType.CLK, via, "clk"));
        via.addPort(new Port("rst", Direction.INPUT, PortType.RST, via, "rst"));
        via.addPort(new Port("done", Direction.OUTPUT, 1, PortType.DONE, via));

        // numbun-dependant parameters and ports
        int num = 0;
        for (Bundle bun : mod.getBundles(Direction.INPUT)) {
            via.addParameter(new Parameter("i" + num + "_WIDTH", bun.getWidth()));
            via.addParameter(new Parameter("i" + num + "_ID", CURRID++));
            via.addParameter(new Parameter("i" + num + "_NODE", mapping.getRouter(bun)));
            via.addParameter(new Parameter("i" + num + "_VC", vcMap.getVcForBundle(bun)));

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
            via.addParameter(new Parameter("o" + num + "_NODE", mapping.getRouter(bun)));
            via.addParameter(new Parameter("o" + num + "_NUM_DEST", bun.getConnections().size()));
            String destinations = "'{";
            String destinationVcs = "'{";
            for (Bundle con : bun.getConnections()) {
                destinations += mapping.getRouter(con) + ",";
                destinationVcs += vcMap.getVcForBundle(con) + ",";
            }
            destinations = destinations.substring(0, destinations.length() - 1) + "}";
            destinationVcs = destinationVcs.substring(0, destinationVcs.length() - 1) + "}";

            via.addParameter(new Parameter("o" + num + "_DEST", destinations));
            via.addParameter(new Parameter("o" + num + "_VC", destinationVcs));

            // nodep parameter is set to 1 when this via's outputs do not wait
            // for all its inputs to be valid - this happens for masters that
            // support outstanding transactions
            int noDepValue = findNoDepValue(bun);
            via.addParameter(new Parameter("o" + num + "_NODEP", noDepValue));

            // create bundle ports
            Port dataPort = new Port("o" + num + "_data_out", Direction.OUTPUT, bun.getWidth(), via);
            Port destPort = new Port("o" + num + "_dest_out", Direction.OUTPUT, noc.getAddressWidth(), via);
            Port vcPort = new Port("o" + num + "_vc_out", Direction.OUTPUT, noc.getVcAddressWidth(), via);
            Port validPort = new Port("o" + num + "_valid_out", Direction.OUTPUT, via);
            Port readyPort = new Port("o" + num + "_ready_in", Direction.INPUT, via);

            // create bundle and add ports to it
            Bundle newBun = new Bundle("o" + num + "_outbun_" + num, via);
            newBun.addDataPort(dataPort);
            newBun.setDstPort(destPort);
            newBun.setVcPort(vcPort);
            newBun.setValidPort(validPort);
            newBun.setReadyPort(readyPort);

            // add both bundle and ports to mod
            via.addBundle(newBun);
            bbMap.put(bun, newBun);
            num++;
        }
        return via;
    }

    private static int findNoDepValue(Bundle bun) {
        // nodepvalue is 1 for masters - we want them to always be sending data
        // so that we can investigate the different tradeoffs of credit or
        // token-based schemes
        // to qualify for nodep, the bundle has to be part of an arbitration
        // connectiongroup and be a master as well
        if (bun.getConnectionGroup().getConnectionType() == ConnectionType.ARBITRATION && bun.getConnectionGroup().isMaster(bun))
            return 1;
        // also if it's part of a simple p2p group it will have nodep = 1 -- a
        // module in a feedforward pipeline doesn't have to wait for it's input
        // to send data in our hollowsim simulation (this is also partly because
        // the RETURN_TO_SENDER parameter in the vias is currently tied to
        // NODEP)
        if (bun.getConnectionGroup().getConnectionType() != ConnectionType.ARBITRATION)
            return 1;
        return 0;
    }

    protected static DesignModule createSrcModule(Noc noc, Bundle bun, Mapping mapping, VcMap vcMap, Map<Bundle, Bundle> bbMap) {
        DesignModule src = new DesignModule("src", "src_" + bun.getFullNameDash());

        // add parameters
        src.addParameter(new Parameter("WIDTH", bun.getWidth()));
        src.addParameter(new Parameter("N", noc.getNumRouters()));
        src.addParameter(new Parameter("NUM_VC", noc.getNumVcs()));
        src.addParameter(new Parameter("ID", CURRID++));
        src.addParameter(new Parameter("NODE", mapping.getRouter(bun)));
        src.addParameter(new Parameter("NUM_DEST", bun.getConnections().size()));

        String destinations = "'{";
        String destinationVcs = "'{";
        for (Bundle con : bun.getConnections()) {
            destinations += mapping.getRouter(con) + ",";
            destinationVcs += vcMap.getVcForBundle(con) + ",";
        }
        destinations = destinations.substring(0, destinations.length() - 1) + "}";
        destinationVcs = destinationVcs.substring(0, destinationVcs.length() - 1) + "}";

        src.addParameter(new Parameter("DEST", destinations));
        src.addParameter(new Parameter("VC", destinationVcs));

        // add clk/rst ports
        src.addPort(new Port("clk", Direction.INPUT, PortType.CLK, src, "clk"));
        src.addPort(new Port("rst", Direction.INPUT, PortType.RST, src, "rst"));
        src.addPort(new Port("done", Direction.OUTPUT, 1, PortType.DONE, src));

        // create bundle ports
        Port dataPort = new Port("data_out", Direction.OUTPUT, bun.getWidth(), src);
        Port destPort = new Port("dest_out", Direction.OUTPUT, noc.getAddressWidth(), src);
        Port vcPort = new Port("vc_out", Direction.OUTPUT, noc.getVcAddressWidth(), src);
        Port validPort = new Port("valid_out", Direction.OUTPUT, src);
        Port readyPort = new Port("ready_in", Direction.INPUT, src);

        // create bundle and add ports to it
        Bundle newBun = new Bundle("outbun", src);
        newBun.addDataPort(dataPort);
        newBun.setDstPort(destPort);
        newBun.setVcPort(vcPort);
        newBun.setValidPort(validPort);
        newBun.setReadyPort(readyPort);

        // add both bundle and ports to mod
        src.addBundle(newBun);

        // now we want to add the newbun to the bbMap
        bbMap.put(bun, newBun);

        return src;
    }

    protected static DesignModule createSinkModule(Noc noc, Bundle bun, Mapping mapping, Map<Bundle, Bundle> bbMap) {
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

    public static Mapping cloneMappingSimBundles(Design simDesign, Noc simNoc, Mapping mapping,
            Map<Bundle, Bundle> designToSimBundleMap) {
        AnnealBundleStruct oldAnnealStruct = mapping.getAnnealBundleStruct();
        AnnealBundleStruct newAnnealStruct = new AnnealBundleStruct();

        // deep copy and replace with sim bundles
        for (int i = 0; i < oldAnnealStruct.bundlesAtRouter.size(); i++) {
            Set<Bundle> bunSet = oldAnnealStruct.bundlesAtRouter.get(i);
            HashSet<Bundle> newBunList = new HashSet<Bundle>();
            for (Bundle bun : bunSet) {
                newBunList.add(designToSimBundleMap.get(bun));
            }
            newAnnealStruct.bundlesAtRouter.add(newBunList);
        }

        for (NocBundle nocbun : oldAnnealStruct.usedNocBundle.keySet()) {
            NocBundle simNocbun = getEquivalentSimNocBundle(nocbun, simNoc);
            boolean used = oldAnnealStruct.usedNocBundle.get(nocbun);
            newAnnealStruct.usedNocBundle.put(simNocbun, used);
        }

        for (Bundle bun : oldAnnealStruct.bundleMap.keySet()) {
            List<NocBundle> oldNocBunList = oldAnnealStruct.bundleMap.get(bun);
            List<NocBundle> newNocBunList = new ArrayList<NocBundle>();
            for (NocBundle nocbun : oldNocBunList) {
                newNocBunList.add(getEquivalentSimNocBundle(nocbun, simNoc));
            }
            newAnnealStruct.bundleMap.put(designToSimBundleMap.get(bun), newNocBunList);
        }

        Mapping mappingClone = new Mapping(newAnnealStruct, simDesign, simNoc);
        return mappingClone;
    }

    private static NocBundle getEquivalentSimNocBundle(NocBundle nocbun, Noc simNoc) {
        NocBundle simNocbun = null;
        if (nocbun.getDirection() == Direction.INPUT) {
            simNocbun = simNoc.getNocInBundles(nocbun.getRouter()).get(nocbun.getIndex());
        } else {
            simNocbun = simNoc.getNocOutBundles(nocbun.getRouter()).get(nocbun.getIndex());
        }
        assert simNocbun != null : "Can't find equivalent nocbun for " + nocbun;
        return simNocbun;
    }

    public static VcMap cloneVcMapSimBundles(VcMap vcMap, Map<Bundle, Bundle> designToSimBundleMap) {
        VcMap vcMapClone = new VcMap();

        // get the original pieces
        Map<Bundle, Integer> oldDestBundleToCombineData = vcMap.getDstBundleToCombineData();
        Map<Bundle, Integer> oldDestBundleToVc = vcMap.getBundleToVcs();
        Map<Integer, Integer> oldRouterToCombineData = vcMap.getRouterToCombineData();

        // create the clones
        Map<Bundle, Integer> newDestBundleToCombineData = new HashMap<Bundle, Integer>();
        Map<Bundle, Integer> newBundleToVc = new HashMap<Bundle, Integer>();
        Map<Integer, Integer> newRouterToCombineData = new HashMap<Integer, Integer>();

        // deep copy and replace bundles
        for (int router : oldRouterToCombineData.keySet()) {
            newRouterToCombineData.put(router, oldRouterToCombineData.get(router));
        }
        for (Bundle bun : oldDestBundleToCombineData.keySet()) {
            newDestBundleToCombineData.put(designToSimBundleMap.get(bun), oldDestBundleToCombineData.get(bun));
        }
        for (Bundle bun : oldDestBundleToVc.keySet()) {
            newBundleToVc.put(designToSimBundleMap.get(bun), oldDestBundleToVc.get(bun));
        }

        // put them back in the new data structure
        vcMapClone.setBundleToVcs(newBundleToVc);
        vcMapClone.setDstBundleToCombineData(newDestBundleToCombineData);
        vcMapClone.setRouterToCombineData(newRouterToCombineData);

        return vcMapClone;
    }

    private static List<ConnectionGroup> cloneCgListSimBundles(Design simulationDesign, Map<Bundle, Bundle> designToSimBundleMap) {
        List<ConnectionGroup> cgListClone = Elaboration.identifyConnectionGroups(simulationDesign);

        return cgListClone;
    }
}

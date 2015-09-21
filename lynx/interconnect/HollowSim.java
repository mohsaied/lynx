package lynx.interconnect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Depacketizer;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.Packetizer;
import lynx.data.Parameter;
import lynx.data.Port;
import lynx.data.MyEnums.ConnectionType;
import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.data.Wrapper;
import lynx.elaboration.ConnectionGroup;
import lynx.nocmapping.Mapping;

/**
 * Includes subroutines that pertain to creating a hollow simulation
 * 
 * @author Mohamed
 *
 */
public class HollowSim {

    private static final Logger log = Logger.getLogger(HollowSim.class.getName());

    protected static int CURRID = 0;

    /**
     * Replace designmodules with src/sink/vias
     * 
     * @param originalDesign
     * @param simulationDesign
     * @param mapping
     * @return
     */
    protected static Map<Bundle, Bundle> populateHollowSimDesign(Design originalDesign, Design simulationDesign, Mapping mapping) {
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

        createAndConnectHaltModule(simulationDesign);

        return bbMap;
    }

    protected static void insertSimulationTranslators(Noc noc, Design design, Mapping mapping, Map<Bundle, Bundle> bbMap) {

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

    protected static DesignModule createViaModule(Noc noc, DesignModule mod, Mapping mapping, Map<Bundle, Bundle> bbMap) {
        int numSrc = mod.getBundles(Direction.OUTPUT).size();
        int numSink = mod.getBundles(Direction.INPUT).size();
        DesignModule via = new DesignModule("via_" + numSrc + "_" + numSink, "via_" + mod.getName());

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
            via.addParameter(new Parameter("o" + num + "_NUM_DEST", bun.getConnections().size()));
            String destinations = "'{";
            for (Bundle con : bun.getConnections()) {
                destinations += mapping.getRouter(con) + ",";
            }
            destinations = destinations.substring(0, destinations.length() - 1) + "}";
            via.addParameter(new Parameter("o" + num + "_DEST", destinations));

            // nodep parameter is set to 1 when this via's outputs do not wait
            // for all its inputs to be valid - this happens for masters that
            // support outstanding transactions
            int noDepValue = findNoDepValue(bun);
            via.addParameter(new Parameter("o" + num + "_NODEP", noDepValue));

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
        if (bun.getConnectionGroup().getConnectionType() == ConnectionType.P2P)
            return 1;
        // TODO make sure we set the via NODEP value correctly for all cases
        // (e.g. broadcast etc)
        return 0;
    }

    protected static DesignModule createSrcModule(Noc noc, Bundle bun, Mapping mapping, Map<Bundle, Bundle> bbMap) {
        DesignModule src = new DesignModule("src", "src_" + bun.getFullNameDash());

        // add parameters
        src.addParameter(new Parameter("WIDTH", bun.getWidth()));
        src.addParameter(new Parameter("N", noc.getNumRouters()));
        src.addParameter(new Parameter("ID", CURRID++));
        src.addParameter(new Parameter("NODE", mapping.getRouter(bun)));
        src.addParameter(new Parameter("NUM_DEST", bun.getConnections().size()));
        String destinations = "'{";
        for (Bundle con : bun.getConnections()) {
            destinations += mapping.getRouter(con) + ",";
        }
        destinations = destinations.substring(0, destinations.length() - 1) + "}";
        src.addParameter(new Parameter("DEST", destinations));

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

    public static void insertTrafficManagers(Noc newNoc, Design simulationDesign, Map<Bundle, Bundle> designToSimBundleMap,
            Mapping mapping, List<ConnectionGroup> cgList) {

        // loop over connectionGroups, we'll add traffic managers for
        // arbitration masters only
        for (ConnectionGroup cgGroup : cgList) {
            if (cgGroup.getConnectionType() == ConnectionType.ARBITRATION) {
                // find bundles that are:
                // 1- masters
                // 2- output
                for (Bundle outbun : cgGroup.getFromBundles()) {
                    if (cgGroup.isMaster(outbun)) {

                        // then find the corresponding input bundle:
                        // 1- master
                        // 2- input
                        // 3- same module
                        // (TODO: doesn't handle the cases of modules that
                        // consist of more than one master)
                        for (Bundle inbun : cgGroup.getToBundles()) {
                            if (cgGroup.isMaster(inbun) && inbun.getParentModule() == outbun.getParentModule()) {
                                Bundle inbunSim = designToSimBundleMap.get(inbun);
                                Bundle outbunSim = designToSimBundleMap.get(outbun);
                                // TODO need definite function to return the
                                // router index for a bundle
                                int router = mapping.getApproxRouterForModule(inbun.getParentModule());
                                createAndConnectCreditMasterTM(inbunSim, outbunSim, simulationDesign, newNoc, router);
                            }
                        }
                        // if no input bundle is found, we'll do nothing
                        // for converge patterns, receive bundle should be
                        // inserted earlier
                    }
                }
            }
        }
    }

    private static void createAndConnectCreditMasterTM(Bundle inbunSim, Bundle outbunSim, Design simulationDesign, Noc newNoc,
            int router) {
        // create the credit TM module
        Wrapper tm = new Wrapper("tm_master_credit", outbunSim.getParentModule().getName() + "_tm", outbunSim.getParentModule());

        log.info("Adding TM " + tm.getName());

        // parameters
        tm.addParameter(new Parameter("NUM_CREDITS", 8));

        // ports
        Port sendValidPort = new Port("send_valid", Direction.INPUT, tm);
        Port sendReadyOutPort = new Port("send_ready_out", Direction.OUTPUT, tm);
        Port sendReadyInPort = new Port("send_ready_in", Direction.INPUT, tm);
        Port receiveValidPort = new Port("receive_valid", Direction.INPUT, tm);

        tm.addPort(sendValidPort);
        tm.addPort(sendReadyOutPort);
        tm.addPort(sendReadyInPort);
        tm.addPort(receiveValidPort);

        // fetch the translators for the inbunSim and outbunsim
        Packetizer outbunPkt = (Packetizer) outbunSim.getTranslator();

        // now stich in the ports in their right locations

        // send_valid reads the output valid
        Port pktValid = outbunPkt.getPort(PortType.VALID, Direction.OUTPUT);
        sendValidPort.addWire(pktValid);
        pktValid.addWire(sendValidPort);

        // receive valid is pretty much the same but snoops on the noc valid
        Port nocValid = newNoc.getPort(PortType.VALID, Direction.OUTPUT, router);
        nocValid.addWire(receiveValidPort);
        receiveValidPort.addWire(nocValid);

        // sendReadyOut requires undoing a connection between the router and
        // packetizer -- first: get these two ports
        Port routerReady = newNoc.getPort(PortType.READY, Direction.OUTPUT, router);
        Port pktReady = outbunPkt.getPort(PortType.READY, Direction.INPUT);
        // second: remove their connection
        routerReady.removeWire(pktReady);
        pktReady.removeWire(routerReady);
        // third: connect the TM
        routerReady.addWire(sendReadyInPort);
        sendReadyInPort.addWire(routerReady);
        pktReady.addWire(sendReadyOutPort);
        sendReadyOutPort.addWire(pktReady);

        // add to design
        simulationDesign.addWrapper(tm);
    }
}

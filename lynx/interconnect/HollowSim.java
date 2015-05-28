package lynx.interconnect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lynx.data.Bundle;
import lynx.data.Depacketizer;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.Packetizer;
import lynx.data.Parameter;
import lynx.data.Port;
import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.nocmapping.Mapping;

/**
 * Includes subroutines that pertain to creating a hollow simulation
 * 
 * @author Mohamed
 *
 */
public class HollowSim {

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
}

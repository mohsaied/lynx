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
import lynx.data.Parameter;
import lynx.data.Port;
import lynx.data.Wrapper;
import lynx.data.MyEnums.ConnectionType;
import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.elaboration.ConnectionGroup;
import lynx.main.DesignData;
import lynx.nocmapping.Mapping;
import lynx.vcmapping.VcMap;

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
     */
    public static void addNoc() throws ParserConfigurationException, SAXException, IOException {
        addNoc(null);
    }

    /**
     * Parse an NoC description from an xml file
     * 
     * @param nocPath
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
    public static void connectDesignToNoc(Design design, Noc noc, List<ConnectionGroup> cgList, VcMap vcMap) {

        log.info("Creating and connecting simulation model");
        HollowSim.createAndConnectHollowSim(design, noc, cgList, vcMap);

        log.info("Creating and connecting actual design");
        connectActualDesignToNoc(design, noc, cgList, vcMap);
    }

    // TODO this flow is unfinished (but shouldn't take long to get it working)
    public static void connectActualDesignToNoc(Design design, Noc noc, List<ConnectionGroup> cgList, VcMap vcMap) {
        Mapping mapping = DesignData.getInstance().getNocMapping();

        log.info("Configuring module clocks");
        configureModuleClocks(design, noc, mapping);

        log.info("Adding Translators and connecting them to modules");
        insertTranslators(noc, design, mapping, vcMap);

        log.info("Inferring top-level ports");
        inferTopLevelPorts(design);

    }

    protected static void configureModuleClocks(Design design, Noc noc, Mapping mapping) {
        for (DesignModule mod : design.getDesignModules().values()) {
            int router = mapping.getApproxRouterForModule(mod);
            if (router < noc.getNumRouters()) {
                mod.getClock().setGlobalOnNoc(true);
                mod.getClock().setGlobalPortName(noc.getModuleGlobalClockName(router));
            }
        }
    }

    private static void insertNoc(String nocPath) throws ParserConfigurationException, SAXException, IOException {

        Noc noc;
        if (nocPath == null)
            noc = new Noc();
        else
            noc = new Noc(nocPath);

        DesignData.getInstance().setNoc(noc);
    }

    protected static void insertTranslators(Noc noc, Design design, Mapping mapping, VcMap vcMap) {

        // mapping contains bundle-nocbundle mapping
        Map<Bundle, List<NocBundle>> bundleMap = mapping.getBundleMap();
        for (Bundle bun : bundleMap.keySet()) {
            // insert and connect translators for bundles that are mapped onto
            // NoC - i.e. has nonzero list of nocbundles
            List<NocBundle> nocbuns = bundleMap.get(bun);
            if (nocbuns.size() != 0) {
                switch (bun.getDirection()) {
                case OUTPUT:
                    Packetizer packetizer = new Packetizer(noc, bun, nocbuns, mapping, vcMap);
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

    public static void insertTrafficManagers(Noc noc, Design design, Mapping mapping, List<ConnectionGroup> cgList) {

        // loop over connectionGroups, we'll add traffic managers for
        // arbitration masters only
        for (ConnectionGroup cgGroup : cgList) {
            if (cgGroup.getConnectionType() == ConnectionType.ARBITRATION) {
                // how many masters do we have in this arbitration group?
                int numSendingMasters = 0;
                for (Bundle outbun : cgGroup.getFromBundles()) {
                    if (cgGroup.isMaster(outbun)) {
                        numSendingMasters++;
                    }
                }
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
                                // TODO need definite function to return the
                                // router index for a bundle
                                int router = mapping.getApproxRouterForModule(inbun.getParentModule());
                                createAndConnectCreditMasterTM(inbun, outbun, design, noc, router, numSendingMasters);
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

    private static void createAndConnectCreditMasterTM(Bundle inbun, Bundle outbun, Design design, Noc noc, int router,
            int numSendingMasters) {
        // create the credit TM module
        Wrapper tm = new Wrapper("tm_master_credit", outbun.getParentModule().getName() + "_tm", outbun.getParentModule());

        log.info("Adding TM " + tm.getName());

        // parameters
        // TODO replace 8 with the ideal number of credits for fair arbitration
        float idealNumCredits = Math.round((2 * noc.getAverageLatency() + 1) / numSendingMasters);
        int idealNumCreditsInt = (int) Math.ceil(idealNumCredits);
        tm.addParameter(new Parameter("NUM_CREDITS", idealNumCreditsInt));

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
        Packetizer outbunPkt = (Packetizer) outbun.getTranslator();

        // now stitch in the ports in their right locations

        // send_valid reads the output valid
        Port pktValid = outbunPkt.getPort(PortType.VALID, Direction.OUTPUT);
        sendValidPort.addWire(pktValid);
        pktValid.addWire(sendValidPort);

        // we track the valid signal going to the depacketizer of that module
        // to increment the number of credits whenever we get something back
        Depacketizer inbundepkt = (Depacketizer) inbun.getTranslator();
        Port depktValid = inbundepkt.getPort(PortType.VALID, Direction.OUTPUT);
        depktValid.addWire(receiveValidPort);
        receiveValidPort.addWire(depktValid);

        // sendReadyOut requires undoing a connection between the router and
        // packetizer -- first: get these two ports
        Port routerReady = noc.getPort(PortType.READY, Direction.OUTPUT, router);
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
        design.addWrapper(tm);
    }

    protected static void inferTopLevelPorts(Design design) {
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

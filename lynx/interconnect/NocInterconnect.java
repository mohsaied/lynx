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
import lynx.data.Translator;
import lynx.data.Wrapper;
import lynx.data.MyEnums.ConnectionType;
import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.data.MyEnums.TranslatorType;
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

        // log.info("Creating and connecting actual design");
        // connectActualDesignToNoc(design, noc, cgList, vcMap);
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

        // after adding all the translators, and connecting them to noc and
        // modules we'll add any dest appenders that are needed, and connect
        // them to the right translators
        for (Translator translator : design.getTranslators()) {
            if (translator.getTranslatorType() == TranslatorType.DEPACKETIZER_DA) {
                Depacketizer depkt = (Depacketizer) translator;
                // we need to add a dest_appender
                // find the packetizer for the very same slave module
                Bundle inbun = translator.getParentBundle();
                assert inbun.getConnectionGroup().isSlave(inbun) && inbun.getDirection() == Direction.INPUT : "bundle for depacketizer_da isn't a slave or has wrong direction";
                // find the slave input port and associated packetizer
                Bundle outbun = inbun.getSisterBundle();
                assert outbun != null : "Couldn't find output bundle for slave of input bundle " + inbun.getFullName();
                assert inbun.getParentModule() == outbun.getParentModule() : "Slave input and output ports ("
                        + inbun.getFullName() + "," + outbun.getFullName() + ") aren't part of the same module.";
                // now find the packetizer for that outbun
                Packetizer pkt = null;
                for (Translator translator1 : design.getTranslators()) {
                    if (translator1.getParentBundle() == outbun) {
                        pkt = (Packetizer) translator1;
                    }
                }
                assert pkt != null : "Couldn't find packetizer for " + outbun.getFullName();
                assert pkt.getTranslatorType() == TranslatorType.PACKETIZER_STD : "Packetizer type " + pkt.getTranslatorType()
                        + " cannot be connected to a dest appender.";

                createAndConnectDestAppender(design, noc, inbun, outbun, pkt, depkt);

            }
        }
    }

    private static void createAndConnectDestAppender(Design design, Noc noc, Bundle inbun, Bundle outbun, Packetizer pkt,
            Depacketizer depkt) {

        // create the credit TM module
        Wrapper da = new Wrapper("dest_appender", outbun.getParentModule().getName() + "_da", outbun.getParentModule());

        log.info("Adding DEST APPENDER " + da.getName());

        da.addParameter(new Parameter("ADDRESS_WIDTH", noc.getAddressWidth()));
        da.addParameter(new Parameter("VC_ADDRESS_WIDTH", noc.getVcAddressWidth()));
        // TODO this should be the slave latency + 2 or something
        da.addParameter(new Parameter("DEPTH", 4));

        // ports
        Port iDstIn = new Port("i_dst_in", Direction.INPUT, noc.getAddressWidth(), da);
        Port iVcIn = new Port("i_vc_in", Direction.INPUT, noc.getVcAddressWidth(), da);
        Port iValidIn = new Port("i_valid_in", Direction.INPUT, da);
        Port oDstOut = new Port("o_dst_out", Direction.OUTPUT, noc.getAddressWidth(), da);
        Port oVcOut = new Port("o_vc_out", Direction.OUTPUT, noc.getVcAddressWidth(), da);
        Port oValidIn = new Port("o_valid_in", Direction.INPUT, da);
        da.addPort(iDstIn);
        da.addPort(iVcIn);
        da.addPort(iValidIn);
        da.addPort(oDstOut);
        da.addPort(oVcOut);
        da.addPort(oValidIn);

        // connect ports

        // the i ports come from the depacketizer
        Port dpktDest = depkt.getPort(PortType.DST, Direction.OUTPUT);
        iDstIn.addWire(dpktDest);
        dpktDest.addWire(iDstIn);

        Port dpktVc = depkt.getPort(PortType.VC, Direction.OUTPUT);
        iVcIn.addWire(dpktVc);
        dpktVc.addWire(iVcIn);

        Port validDest = depkt.getPort(PortType.VALID, Direction.OUTPUT);
        iValidIn.addWire(validDest);
        validDest.addWire(iValidIn);

        // the o ports go to the packetizer -- need to rip out existing wires
        // first
        Port pktDst = pkt.getPort(PortType.DST, Direction.INPUT);
        oDstOut.addWire(pktDst);
        Port modDstOut = pkt.getParentBundle().getDstPort();
        modDstOut.removeWire(pktDst);
        pktDst.removeWire(modDstOut);
        pktDst.addWire(oDstOut);

        Port pktVc = pkt.getPort(PortType.VC, Direction.INPUT);
        oVcOut.addWire(pktVc);
        Port modVcOut = pkt.getParentBundle().getVcPort();
        modVcOut.removeWire(pktVc);
        pktVc.removeWire(modVcOut);
        pktVc.addWire(oVcOut);

        Port pktValid = pkt.getPort(PortType.VALID, Direction.OUTPUT);
        oValidIn.addWire(pktValid);
        pktValid.addWire(oValidIn);

        // add to design
        design.addWrapper(da);
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

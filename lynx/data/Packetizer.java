package lynx.data;

import java.util.List;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.data.MyEnums.TranslatorType;
import lynx.nocmapping.Mapping;
import lynx.vcmapping.VcMap;

/**
 * Packetizer
 * 
 * @author Mohamed
 *
 */
public final class Packetizer extends Translator {

    public Packetizer(Noc parentNoc, Bundle parentBundle, List<NocBundle> nocbuns, Mapping mapping, VcMap vcMap) {
        super(parentNoc, parentBundle.getParentModule(), parentBundle, TranslatorType.PACKETIZER_VC);

        addParametersAndPorts(parentBundle, nocbuns, mapping, vcMap);

        connectToBundle();

        connectToRouter(nocbuns);
    }

    @SuppressWarnings("unused")
    @Deprecated
    private static TranslatorType getPacketizerType(Noc parentNoc, List<NocBundle> nocbuns, int vc) {
        // get total nocbuns width
        int width = 0;
        for (NocBundle nocbun : nocbuns) {
            width += nocbun.getWidth();
        }
        int nocWidth = parentNoc.getWidth();

        int numFlitsForThisTranslator = width / nocWidth;

        // if we need a vc table for this packetizer, then we'll instantiate
        // packetizer_vc
        if (vc == -1)
            return TranslatorType.PACKETIZER_VC;

        // otherwise instantiate the right packetizer size
        TranslatorType type = null;
        switch (numFlitsForThisTranslator) {
        case 4:
            type = TranslatorType.PACKETIZER_4;
            break;
        case 3:
            type = TranslatorType.PACKETIZER_3;
            break;
        case 2:
            type = TranslatorType.PACKETIZER_2;
            break;
        case 1:
            type = TranslatorType.PACKETIZER_1;
            break;
        default:
            assert false : "Unsupported packetizer requested";
        }
        return type;
    }

    protected final void addParametersAndPorts(Bundle bundle, List<NocBundle> nocbuns, Mapping mapping, VcMap vcMap) {

        // find the Noc-facing width -- sum up width of nocbuns
        int nocFacingWidth = 0;
        for (NocBundle nocbun : nocbuns) {
            nocFacingWidth += nocbun.getWidth();
        }

        // get total nocbuns width
        int width = 0;
        for (NocBundle nocbun : nocbuns) {
            width += nocbun.getWidth();
        }
        int nocWidth = parentNoc.getWidth();

        int numFlitsForThisTranslator = width / nocWidth;

        // parameters
        this.addParameter(new Parameter("ADDRESS_WIDTH", parentNoc.getAddressWidth()));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", parentNoc.getVcAddressWidth()));
        this.addParameter(new Parameter("WIDTH_IN", parentBundle.getWidth()));
        this.addParameter(new Parameter("WIDTH_OUT", nocFacingWidth));
        this.addParameter(new Parameter("PACKETIZER_WIDTH", numFlitsForThisTranslator));

        int vc = vcMap.getVcForBundle(bundle);

        int numDest = 1;
        String dests = "'{0}";
        String vcs = "'{" + vc + "}";

        // this packetizer will send on multiple VCs
        if (vc == -1) {
            // find the number of dests for this bundle
            numDest = bundle.getConnections().size();

            dests = "'{";
            vcs = "'{";

            // loop over bundles, populate their router and VC
            for (Bundle dstBun : bundle.getConnections()) {
                dests += mapping.getRouter(dstBun) + ",";
                vcs += vcMap.getVcForBundle(dstBun) + ",";
            }

            dests = dests.substring(0, dests.length() - 1) + "}";
            vcs = vcs.substring(0, vcs.length() - 1) + "}";
        }

        this.addParameter(new Parameter("NUM_DEST", numDest));
        this.addParameter(new Parameter("DEST", dests));
        this.addParameter(new Parameter("VC", vcs));

        // ports
        this.addPort(new Port(buildPortName(PortType.DATA, Direction.INPUT), Direction.INPUT, parentBundle.getWidth(), this));
        this.addPort(new Port(buildPortName(PortType.VALID, Direction.INPUT), Direction.INPUT, 1, this));
        this.addPort(new Port(buildPortName(PortType.DST, Direction.INPUT), Direction.INPUT, parentNoc.getAddressWidth(), this));
        this.addPort(new Port(buildPortName(PortType.VC, Direction.INPUT), Direction.INPUT, parentNoc.getVcAddressWidth(), this));
        this.addPort(new Port(buildPortName(PortType.READY, Direction.OUTPUT), Direction.OUTPUT, 1, this));

        this.addPort(new Port(buildPortName(PortType.DATA, Direction.OUTPUT), Direction.OUTPUT, nocFacingWidth, this));
        this.addPort(new Port(buildPortName(PortType.VALID, Direction.OUTPUT), Direction.OUTPUT, 1, this));
        this.addPort(new Port(buildPortName(PortType.READY, Direction.INPUT), Direction.INPUT, 1, this));
    }

    @Override
    protected final void connectToBundle() {

        // each translator has a parent module and bundle
        // connect the module side but leave the NoC side unconnected for now
        // that will be connected later depending on design effort or extra info
        // should be done in lynx.interconnect somewhere

        // connect data
        Port pktDataIn = getPort(PortType.DATA, Direction.INPUT);
        Port modDataOut = parentBundle.getDataPort();
        pktDataIn.addWire(modDataOut);
        modDataOut.addWire(pktDataIn);

        // connect valid
        Port pktValidIn = getPort(PortType.VALID, Direction.INPUT);
        Port modValidOut = parentBundle.getValidPort();
        pktValidIn.addWire(modValidOut);
        modValidOut.addWire(pktValidIn);

        // connect dest
        Port pktDstIn = getPort(PortType.DST, Direction.INPUT);
        Port modDstOut = parentBundle.getDstPort();
        pktDstIn.addWire(modDstOut);
        modDstOut.addWire(pktDstIn);

        // connect vc
        Port pktVcIn = getPort(PortType.VC, Direction.INPUT);
        Port modVcOut = parentBundle.getVcPort();
        pktVcIn.addWire(modVcOut);
        modVcOut.addWire(pktVcIn);

        // connect ready
        Port pktReadyOut = getPort(PortType.READY, Direction.OUTPUT);
        Port modReadyIn = parentBundle.getReadyPort();
        pktReadyOut.addWire(modReadyIn);
        modReadyIn.addWire(pktReadyOut);
    }

    @Override
    public final void connectToRouter(List<NocBundle> nocbuns) {

        assert nocbuns.size() != 0 : "Attempting to connect to a router, but no NocBundles are specified";

        // all nocbundles should be connected to the same router so getting the
        // router of the first one suffices
        int router = nocbuns.get(0).getRouter();

        // and where do they start and end? //max is tdm
        int startIndex = this.parentNoc.getTdmFactor();
        int endIndex = 0;
        // find start and end points
        for (NocBundle nocbun : nocbuns) {
            if (nocbun.getIndex() < startIndex)
                startIndex = nocbun.getIndex();
            if (nocbun.getIndex() > endIndex)
                endIndex = nocbun.getIndex();
        }

        // find start and end widths on the Noc Ports
        int startWidthNocPort = nocbuns.get(0).getNoc().getInterfaceWidth() - nocbuns.get(0).getWidth() * (endIndex + 1);
        int endWidthNocPort = nocbuns.get(0).getNoc().getInterfaceWidth() - nocbuns.get(0).getWidth() * startIndex - 1;

        // data
        Port pktDataOut = getPort(PortType.DATA, Direction.OUTPUT);
        Port nocDataIn = parentNoc.getPort(PortType.DATA, Direction.INPUT, router);
        pktDataOut.addWire(nocDataIn, 0, pktDataOut.getWidth() - 1, startWidthNocPort, endWidthNocPort);
        nocDataIn.addWire(pktDataOut, startWidthNocPort, endWidthNocPort, 0, pktDataOut.getWidth() - 1);

        // valid
        Port pktValidOut = getPort(PortType.VALID, Direction.OUTPUT);
        Port nocValidIn = parentNoc.getPort(PortType.VALID, Direction.INPUT, router);
        pktValidOut.addWire(nocValidIn);
        nocValidIn.addWire(pktValidOut);

        // ready
        Port pktReadyIn = getPort(PortType.READY, Direction.INPUT);
        Port nocReadyOut = parentNoc.getPort(PortType.READY, Direction.OUTPUT, router);
        pktReadyIn.addWire(nocReadyOut);
        nocReadyOut.addWire(pktReadyIn);
    }
}

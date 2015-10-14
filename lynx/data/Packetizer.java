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
        super(parentNoc, parentBundle.getParentModule(), parentBundle, figureOutPacketizerType(parentBundle));

        addParametersAndPorts(parentBundle, nocbuns, mapping, vcMap);

        connectToBundle(mapping, vcMap);

        connectToRouter(nocbuns);
    }

    private static TranslatorType figureOutPacketizerType(Bundle parentBundle) {

        // if this is a master then we'll always append return dst/vc so the
        // slave knows where to return the reply to
        if (parentBundle.getConnectionGroup().isMaster(parentBundle)) {
            return TranslatorType.PACKETIZER_DA;
        }

        return TranslatorType.PACKETIZER_STD;
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

        if (this.getTranslatorType() == TranslatorType.PACKETIZER_DA) {
            int dest_in = mapping.getRouter(bundle);
            int vc_in = vcMap.getVcForBundle(bundle);
            this.addParameter(new Parameter("DEST", dest_in));
            this.addParameter(new Parameter("VC", vc_in));
        }

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

    private final void connectToBundle(Mapping mapping, VcMap vcMap) {

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
        // and vc
        Port pktVcIn = getPort(PortType.VC, Direction.INPUT);
        Port modVcOut = parentBundle.getVcPort();
        if (modDstOut == null || modVcOut == null) {
            if (parentBundle.getConnections().size() == 1) {
                // if we have one dest we can just add it as a constant
                int dest = mapping.getRouter(parentBundle.getConnections().get(0));
                int vc = vcMap.getVcForBundle(parentBundle.getConnections().get(0));
                pktDstIn.setConstantValue(dest);
                pktVcIn.setConstantValue(vc);
            } else if (parentBundle.getConnectionGroup().isSlave(parentBundle)) {
                assert this.getTranslatorType() == TranslatorType.PACKETIZER_STD : "A slave that sends to many masters can only have a std packetizer, not "
                        + this.getTranslatorType();
                // this is the packetizer of a slave that responds to whatever
                // sent to it -- in this case this packetizer will get its
                // signals from a dest_appender
                // TODO add code to connect to dst appender
                //we won't connect these signals here, but we'll do it in the dest_appender once we instantiate it after the translators
            } else {
                // if we have multiple destinations and we're not a slave, then
                // the user has to add the signals (we don't know where data
                // will be sent when)
                assert false : "Bundle " + parentBundle.getFullName() + " doesn't have dst/vc signals but has multiple("
                        + parentBundle.getConnections().size() + ") destinations -- cannot statically set dst/vc.";
            }
        } else {
            pktDstIn.addWire(modDstOut);
            modDstOut.addWire(pktDstIn);
            pktVcIn.addWire(modVcOut);
            modVcOut.addWire(pktVcIn);
        }

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

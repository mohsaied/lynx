package lynx.data;

import java.util.List;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.data.MyEnums.TranslatorType;

public final class Depacketizer extends Translator {

    public Depacketizer(Noc parentNoc, Bundle parentBundle, List<NocBundle> nocbuns) {
        super(parentNoc, parentBundle.getParentModule(), parentBundle, getDepacketizerType(parentNoc, nocbuns));

        addParametersAndPorts(nocbuns);

        connectToBundle();

        connectToRouter(nocbuns);
    }

    private static TranslatorType getDepacketizerType(Noc parentNoc, List<NocBundle> nocbuns) {
        // get total nocbuns width
        int width = 0;
        for (NocBundle nocbun : nocbuns) {
            width += nocbun.getWidth();
        }
        int nocWidth = parentNoc.getWidth();

        int numFlitsForThisTranslator = width / nocWidth;

        TranslatorType type = null;
        switch (numFlitsForThisTranslator) {
        case 4:
            type = TranslatorType.DEPACKETIZER_4;
            break;
        case 2:
            type = TranslatorType.DEPACKETIZER_2;
            break;
        default:
            assert false : "Unsupported depacketizer requested";
        }
        return type;
    }

    @Override
    protected final void addParametersAndPorts(List<NocBundle> nocbuns) {

        // find the Noc-facing width -- sum up width of nocbuns
        int nocFacingWidth = 0;
        for (NocBundle nocbun : nocbuns) {
            nocFacingWidth += nocbun.getWidth();
        }

        // parameters
        this.addParameter(new Parameter("ADDRESS_WIDTH", parentNoc.getAddressWidth()));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", parentNoc.getVcAddressWidth()));
        this.addParameter(new Parameter("WIDTH_PKT", nocFacingWidth));
        this.addParameter(new Parameter("WIDTH_DATA", parentBundle.getWidth()));

        // ports
        this.addPort(new Port(buildPortName(PortType.DATA, Direction.INPUT), Direction.INPUT, nocFacingWidth, this));
        this.addPort(new Port(buildPortName(PortType.VALID, Direction.INPUT), Direction.INPUT, 1, this));
        this.addPort(new Port(buildPortName(PortType.READY, Direction.OUTPUT), Direction.OUTPUT, 1, this));

        this.addPort(new Port(buildPortName(PortType.DATA, Direction.OUTPUT), Direction.OUTPUT, parentBundle.getWidth(), this));
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
        Port pktDataOut = getPort(PortType.DATA, Direction.OUTPUT);
        Port modDataIn = parentBundle.getDataPort();
        pktDataOut.addWire(modDataIn);
        modDataIn.addWire(pktDataOut);

        // connect valid
        Port pktValidOut = getPort(PortType.VALID, Direction.OUTPUT);
        Port modValidIn = parentBundle.getValidPort();
        pktValidOut.addWire(modValidIn);
        modValidIn.addWire(pktValidOut);

        // connect ready
        Port pktReadyIn = getPort(PortType.READY, Direction.INPUT);
        Port modReadyOut = parentBundle.getReadyPort();
        pktReadyIn.addWire(modReadyOut);
        modReadyOut.addWire(pktReadyIn);
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
        int startWidthNocPort = nocbuns.get(0).getWidth() * startIndex;
        int endWidthNocPort = nocbuns.get(0).getWidth() * (endIndex + 1) - 1;

        // data
        Port pktDataIn = getPort(PortType.DATA, Direction.INPUT);
        Port nocDataOut = parentNoc.getPort(PortType.DATA, Direction.OUTPUT, router);
        pktDataIn.addWire(nocDataOut, 0, pktDataIn.getWidth() - 1, startWidthNocPort, endWidthNocPort);
        nocDataOut.addWire(pktDataIn, startWidthNocPort, endWidthNocPort, 0, pktDataIn.getWidth() - 1);

        // valid
        Port pktValidIn = getPort(PortType.VALID, Direction.INPUT);
        Port nocValidOut = parentNoc.getPort(PortType.VALID, Direction.OUTPUT, router);
        pktValidIn.addWire(nocValidOut);
        nocValidOut.addWire(pktValidIn);

        // ready
        Port pktReadyOut = getPort(PortType.READY, Direction.OUTPUT);
        Port nocReadyIn = parentNoc.getPort(PortType.READY, Direction.INPUT, router);
        pktReadyOut.addWire(nocReadyIn);
        nocReadyIn.addWire(pktReadyOut);

    }
}

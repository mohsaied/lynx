package lynx.data;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.data.MyEnums.TranslatorType;

public final class Depacketizer extends Translator {

    public Depacketizer(Noc parentNoc, DesignModule parentModule, Bundle parentBundle) {
        super(parentNoc, parentModule, parentBundle, TranslatorType.DEPACKETIZER);

        addParametersAndPorts();

        connectToBundle();
    }

    @Override
    protected final void addParametersAndPorts() {
        // parameters
        this.addParameter(new Parameter("ADDRESS_WIDTH", parentNoc.getAddressWidth()));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", parentNoc.getVcAddressWidth()));
        this.addParameter(new Parameter("WIDTH_PKT", parentNoc.getInterfaceWidth()));
        this.addParameter(new Parameter("WIDTH_DATA", parentBundle.getWidth()));

        // ports
        this.addPort(new Port(buildPortName(PortType.DATA, Direction.INPUT), Direction.INPUT, parentNoc.getWidth(), this));
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
}

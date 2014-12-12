package lynx.data;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.data.MyEnums.TranslatorType;

/**
 * Packetizer
 * 
 * @author Mohamed
 *
 */
public final class Packetizer extends Translator {


    public Packetizer(Noc parentNoc, DesignModule parentModule, Bundle parentBundle) {
        super(parentNoc, parentModule, parentBundle, TranslatorType.PACKETIZER);

        addPacketizerParametersAndPorts();

        connectPacketizerToBundle();
    }

    private void addPacketizerParametersAndPorts() {

        // parameters
        this.addParameter(new Parameter("ADDRESS_WIDTH", parentNoc.getAddressWidth()));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", parentNoc.getVcAddressWidth()));
        this.addParameter(new Parameter("WIDTH_IN", parentBundle.getWidth()));
        this.addParameter(new Parameter("WIDTH_OUT", parentNoc.getInterfaceWidth()));
        this.addParameter(new Parameter("ASSIGNED_VC", "0"));

        // ports
        this.addPort(new Port(buildPortName(PortType.DATA, Direction.INPUT), Direction.INPUT, parentBundle.getWidth(), this));
        this.addPort(new Port(buildPortName(PortType.VALID, Direction.INPUT), Direction.INPUT, 1, this));
        this.addPort(new Port(buildPortName(PortType.DST, Direction.INPUT), Direction.INPUT, parentNoc.getAddressWidth(), this));
        this.addPort(new Port(buildPortName(PortType.READY, Direction.OUTPUT), Direction.OUTPUT, 1, this));

        this.addPort(new Port(buildPortName(PortType.DATA, Direction.OUTPUT), Direction.OUTPUT, parentNoc.getWidth(), this));
        this.addPort(new Port(buildPortName(PortType.VALID, Direction.OUTPUT), Direction.OUTPUT, 1, this));
        this.addPort(new Port(buildPortName(PortType.READY, Direction.INPUT), Direction.INPUT, 1, this));
    }

    private void connectPacketizerToBundle() {

        // each translator has a parent module and bundle
        // connect the module side but leave the NoC side unconnected for now

        // depends on direction
        // packetizer connects from module(bundle) data/valid to packetizer
        // data/valid

    }

}

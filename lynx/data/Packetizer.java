package lynx.data;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.TranslatorType;

/**
 * Packetizer
 * 
 * @author Mohamed
 *
 */
public final class Packetizer extends Translator {

    private Port modDataIn;
    private Port modValidIn;
    private Port modDestIn;
    private Port modReadyOut;

    private Port nocDataOut;
    private Port nocValidOut;
    private Port nocReadyIn;

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
        modDataIn = new Port("i_data_in", Direction.INPUT, parentBundle.getWidth(), 1, this);
        this.addPort(modDataIn);
        modValidIn = new Port("i_valid_in", Direction.INPUT, 1, 1, this);
        this.addPort(modValidIn);
        modDestIn = new Port("i_dest_in", Direction.INPUT, parentNoc.getAddressWidth(), 1, this);
        this.addPort(modDestIn);
        modReadyOut = new Port("i_ready_out", Direction.OUTPUT, 1, 1, this);
        this.addPort(modReadyOut);

        nocDataOut = new Port("o_data_out", Direction.OUTPUT, parentNoc.getWidth(), 1, this);
        this.addPort(nocDataOut);
        nocValidOut = new Port("o_valid_out", Direction.OUTPUT, 1, 1, this);
        this.addPort(nocValidOut);
        nocReadyIn = new Port("o_ready_in", Direction.INPUT, 1, 1, this);
        this.addPort(nocReadyIn);
    }

    private void connectPacketizerToBundle() {

        // each translator has a parent module and bundle
        // connect the module side but leave the NoC side unconnected for now

        // depends on direction
        // packetizer connects from module(bundle) data/valid to packetizer
        // data/valid

    }
}

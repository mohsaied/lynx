package lynx.data;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.TranslatorType;

public final class Depacketizer extends Translator {

    private Port nocDataOut;
    private Port nocValidOut;
    private Port nocReadyIn;

    private Port modDataIn;
    private Port modValidIn;
    private Port modReadyOut;

    public Depacketizer(Noc parentNoc, DesignModule parentModule, Bundle parentBundle) {
        super(parentNoc, parentModule, parentBundle, TranslatorType.DEPACKETIZER);

        addDepacketizerParametersAndPorts();

        connectDepacketizerToBundle();
    }

    private void addDepacketizerParametersAndPorts() {
        // parameters
        this.addParameter(new Parameter("ADDRESS_WIDTH", parentNoc.getAddressWidth()));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", parentNoc.getVcAddressWidth()));
        this.addParameter(new Parameter("WIDTH_PKT", parentNoc.getInterfaceWidth()));
        this.addParameter(new Parameter("WIDTH_DATA", parentBundle.getWidth()));

        // ports
        nocDataOut = new Port("i_packet_in", Direction.INPUT, parentNoc.getWidth(), this);
        this.addPort(nocDataOut);
        nocValidOut = new Port("i_valid_in", Direction.INPUT, 1, this);
        this.addPort(nocValidOut);
        nocReadyIn = new Port("i_ready_out", Direction.OUTPUT, 1, this);
        this.addPort(nocReadyIn);

        modDataIn = new Port("o_data_out", Direction.OUTPUT, parentBundle.getWidth(), this);
        this.addPort(modDataIn);
        modValidIn = new Port("o_valid_out", Direction.OUTPUT, 1, this);
        this.addPort(modValidIn);
        modReadyOut = new Port("o_ready_in", Direction.INPUT, 1, this);
        this.addPort(modReadyOut);
    }

    private void connectDepacketizerToBundle() {

        // each translator has a parent module and bundle
        // connect the module side but leave the NoC side unconnected for now

        // depends on direction
        // packetizer connects from module(bundle) data/valid to packetizer
        // data/valid

    }
}

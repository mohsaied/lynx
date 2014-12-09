package lynx.data;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.TranslatorType;

/**
 * Translators are modules between a DesignModule bundle and an Noc
 * 
 * @author Mohamed
 *
 */
public class Translator extends Module {

    private Noc parentNoc;
    private Module parentModule;
    private Bundle parentBundle;
    private Direction direction;
    private TranslatorType translatorType;

    public Translator(Noc parentNoc, Module parentModule, Bundle parentBundle) {
        this(parentNoc, parentModule, parentBundle,
                parentBundle.getDirection() == Direction.INPUT ? TranslatorType.DEPACKETIZER
                        : TranslatorType.DEPACKETIZER);
    }

    public Translator(Noc parentNoc, Module parentModule, Bundle parentBundle, TranslatorType type) {
        super(type.toString(), parentModule.getName() + "_" + type.toString());
        this.parentNoc = parentNoc;
        this.parentModule = parentModule;
        this.parentBundle = parentBundle;
        this.direction = parentBundle.getDirection();
        this.translatorType = type;

        addTranslatorParmetersAndPorts();
    }

    public Module getParentModule() {
        return parentModule;
    }

    public Bundle getParentBundle() {
        return parentBundle;
    }

    public Direction getDirection() {
        return direction;
    }

    public TranslatorType getTranslatorType() {
        return translatorType;
    }

    private void addTranslatorParmetersAndPorts() {
        switch (translatorType) {
        case PACKETIZER:
            addPacketizerParametersAndPorts();
            break;
        case DEPACKETIZER:
            addDepacketizerParametersAndPorts();
            break;
        }
    }

    private void addPacketizerParametersAndPorts() {

        // parameters
        this.addParameter(new Parameter("ADDRESS_WIDTH", parentNoc.getAddressWidth()));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", parentNoc.getVcAddressWidth()));
        this.addParameter(new Parameter("WIDTH_IN", parentBundle.getWidth()));
        this.addParameter(new Parameter("WIDTH_OUT", parentNoc.getInterfaceWidth()));
        this.addParameter(new Parameter("ASSIGNED_VC", "0"));

        // ports
        this.addPort(new Port("i_data_in", Direction.INPUT, parentBundle.getWidth(), 1, this));
        this.addPort(new Port("i_valid_in", Direction.INPUT, 1, 1, this));
        this.addPort(new Port("i_dest_in", Direction.INPUT, parentNoc.getAddressWidth(), 1, this));
        this.addPort(new Port("i_ready_out", Direction.OUTPUT, 1, 1, this));

        this.addPort(new Port("o_data_out", Direction.OUTPUT, parentNoc.getWidth(), 1, this));
        this.addPort(new Port("o_valid_out", Direction.OUTPUT, 1, 1, this));
        this.addPort(new Port("o_ready_in", Direction.INPUT, 1, 1, this));

    }

    private void addDepacketizerParametersAndPorts() {
        // parameters
        this.addParameter(new Parameter("ADDRESS_WIDTH", parentNoc.getAddressWidth()));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", parentNoc.getVcAddressWidth()));
        this.addParameter(new Parameter("WIDTH_PKT", parentNoc.getInterfaceWidth()));
        this.addParameter(new Parameter("WIDTH_DATA", parentBundle.getWidth()));

        // ports
        this.addPort(new Port("i_packet_in", Direction.INPUT, parentNoc.getWidth(), 1, this));
        this.addPort(new Port("i_valid_in", Direction.INPUT, 1, 1, this));
        this.addPort(new Port("i_ready_out", Direction.OUTPUT, 1, 1, this));

        this.addPort(new Port("o_data_out", Direction.OUTPUT, parentBundle.getWidth(), 1, this));
        this.addPort(new Port("o_valid_out", Direction.OUTPUT, 1, 1, this));
        this.addPort(new Port("o_ready_in", Direction.INPUT, 1, 1, this));
    }

}

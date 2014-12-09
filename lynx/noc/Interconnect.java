package lynx.noc;

import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.MyEnums.*;
import lynx.data.Module;
import lynx.data.Parameter;
import lynx.data.Port;

/**
 * Utility class that adds NoC components and connects them to the design
 * 
 * @author Mohamed
 *
 */
public class Interconnect {

    private static final Logger log = Logger.getLogger(Interconnect.class.getName());

    public static void addNoc(Design design) {
        log.info("Adding NoC circuitry...");

        insertFabricInterface(design);

        insertTranslators(design);
    }

    private static void insertFabricInterface(Design design) {
        Module FabricInterface = new Module("fabric_interface", "fi_inst");

        // parameters
        FabricInterface.addParameter(new Parameter("WIDTH_NOC", "150"));
        FabricInterface.addParameter(new Parameter("WIDTH_RTL", "600"));
        FabricInterface.addParameter(new Parameter("N", "16"));
        FabricInterface.addParameter(new Parameter("NUM_VC", "2"));
        FabricInterface.addParameter(new Parameter("DEPTH_PER_VC", "16"));
        FabricInterface.addParameter(new Parameter("VERBOSE", "1"));
        FabricInterface.addParameter(new Parameter("VC_ADDRESS_WIDTH", "$clog2(NUM_VC)"));
        FabricInterface.addParameter(new Parameter("[VC_ADDRESS_WIDTH-1:0] ASSIGNED_VC [0:N-1]",
                "'{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}"));

        // ports
        FabricInterface.addPort(new Port("clk_noc", Direction.INPUT, 1, 1, FabricInterface));
        FabricInterface.addPort(new Port("rst", Direction.INPUT, 1, 1, FabricInterface));
        FabricInterface.addPort(new Port("clk_rtl", Direction.INPUT, 16, 1, FabricInterface));
        FabricInterface.addPort(new Port("clk_int", Direction.INPUT, 16, 1, FabricInterface));

        FabricInterface.addPort(new Port("i_packets_in", Direction.INPUT, 600, 16, FabricInterface));
        FabricInterface.addPort(new Port("i_valids_in", Direction.INPUT, 1, 16, FabricInterface));
        FabricInterface.addPort(new Port("i_readys_out", Direction.OUTPUT, 1, 16, FabricInterface));

        FabricInterface.addPort(new Port("o_packets_out", Direction.OUTPUT, 600, 16, FabricInterface));
        FabricInterface.addPort(new Port("o_valids_out", Direction.OUTPUT, 1, 16, FabricInterface));
        FabricInterface.addPort(new Port("o_readys_in", Direction.INPUT, 1, 16, FabricInterface));

        design.addModule(FabricInterface);
    }

    private static void insertTranslators(Design design) {

        // TODO loop over all modules and insert one or two translators
        // Number of translators depends on whether one or both interfaces
        // connect to NoC

    }

}

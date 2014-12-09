package lynx.noc;

import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.MyEnums.*;
import lynx.data.Bundle;
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
        Module fabricInterface = new Module("fabric_interface", "fi_inst");

        // parameters
        fabricInterface.addParameter(new Parameter("WIDTH_NOC", "150"));
        fabricInterface.addParameter(new Parameter("WIDTH_RTL", "600"));
        fabricInterface.addParameter(new Parameter("N", "16"));
        fabricInterface.addParameter(new Parameter("NUM_VC", "2"));
        fabricInterface.addParameter(new Parameter("DEPTH_PER_VC", "16"));
        fabricInterface.addParameter(new Parameter("VERBOSE", "1"));
        fabricInterface.addParameter(new Parameter("VC_ADDRESS_WIDTH", "$clog2(NUM_VC)"));
        fabricInterface.addParameter(new Parameter("[VC_ADDRESS_WIDTH-1:0] ASSIGNED_VC [0:N-1]",
                "'{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}"));

        // ports
        fabricInterface.addPort(new Port("clk_noc", Direction.INPUT, 1, 1, fabricInterface));
        fabricInterface.addPort(new Port("rst", Direction.INPUT, 1, 1, fabricInterface));
        fabricInterface.addPort(new Port("clk_rtl", Direction.INPUT, 16, 1, fabricInterface));
        fabricInterface.addPort(new Port("clk_int", Direction.INPUT, 16, 1, fabricInterface));

        fabricInterface.addPort(new Port("i_packets_in", Direction.INPUT, 600, 16, fabricInterface));
        fabricInterface.addPort(new Port("i_valids_in", Direction.INPUT, 1, 16, fabricInterface));
        fabricInterface.addPort(new Port("i_readys_out", Direction.OUTPUT, 1, 16, fabricInterface));

        fabricInterface.addPort(new Port("o_packets_out", Direction.OUTPUT, 600, 16, fabricInterface));
        fabricInterface.addPort(new Port("o_valids_out", Direction.OUTPUT, 1, 16, fabricInterface));
        fabricInterface.addPort(new Port("o_readys_in", Direction.INPUT, 1, 16, fabricInterface));

        design.setFabricInterface(fabricInterface);
    }

    private static void insertTranslators(Design design) {

        // loop over all modules and insert translators
        for (Module mod : design.getModules().values()) {
            for (Bundle bun : mod.getBundles()) {
                switch (bun.getDirection()) {
                case INPUT:
                    insertPacketizer(bun, mod, design);
                    break;
                case OUTPUT:
                    insertDepacketizer(bun, mod, design);
                    break;
                default:
                    assert false : "Bundle direction was never set.";
                }
            }
        }
    }

    private static void insertPacketizer(Bundle bun, Module mod, Design design) {
        Module packetizer = new Module("packetizer", mod.getName() + "_pkt");

        int width = bun.getWidth();
        String widthString = Integer.toString(width);

        // parameters
        packetizer.addParameter(new Parameter("ADDRESS_WIDTH", "4"));
        packetizer.addParameter(new Parameter("VC_ADDRESS_WIDTH", "1"));
        packetizer.addParameter(new Parameter("WIDTH_IN", widthString));
        packetizer.addParameter(new Parameter("WIDTH_OUT", "600"));
        packetizer.addParameter(new Parameter("ASSIGNED_VC", "0"));

        // ports
        packetizer.addPort(new Port("i_data_in", Direction.INPUT, width, 1, packetizer));
        packetizer.addPort(new Port("i_valid_in", Direction.INPUT, 1, 1, packetizer));
        packetizer.addPort(new Port("i_dest_in", Direction.INPUT, 4, 1, packetizer));
        packetizer.addPort(new Port("i_ready_out", Direction.OUTPUT, 1, 1, packetizer));

        packetizer.addPort(new Port("o_data_out", Direction.OUTPUT, 600, 1, packetizer));
        packetizer.addPort(new Port("o_valid_out", Direction.OUTPUT, 1, 1, packetizer));
        packetizer.addPort(new Port("o_ready_in", Direction.INPUT, 1, 1, packetizer));

        design.addTranslator(packetizer);
    }

    private static void insertDepacketizer(Bundle bun, Module mod, Design design) {
        Module depacketizer = new Module("depacketizer", mod.getName() + "_depkt");

        int width = bun.getWidth();
        String widthString = Integer.toString(width);

        // parameters
        depacketizer.addParameter(new Parameter("ADDRESS_WIDTH", "4"));
        depacketizer.addParameter(new Parameter("VC_ADDRESS_WIDTH", "1"));
        depacketizer.addParameter(new Parameter("WIDTH_PKT", "600"));
        depacketizer.addParameter(new Parameter("WIDTH_DATA", widthString));

        // ports
        depacketizer.addPort(new Port("i_packet_in", Direction.INPUT, 600, 1, depacketizer));
        depacketizer.addPort(new Port("i_valid_in", Direction.INPUT, 1, 1, depacketizer));
        depacketizer.addPort(new Port("i_ready_out", Direction.OUTPUT, 1, 1, depacketizer));

        depacketizer.addPort(new Port("o_data_out", Direction.OUTPUT, width, 1, depacketizer));
        depacketizer.addPort(new Port("o_valid_out", Direction.OUTPUT, 1, 1, depacketizer));
        depacketizer.addPort(new Port("o_ready_in", Direction.INPUT, 1, 1, depacketizer));

        design.addTranslator(depacketizer);
    }

}

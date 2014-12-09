package lynx.Interconnect;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.Design;
import lynx.data.MyEnums.*;
import lynx.data.Bundle;
import lynx.data.DesignModule;
import lynx.data.Module;
import lynx.data.Noc;
import lynx.data.Parameter;
import lynx.data.Port;
import lynx.data.Translator;
import lynx.xml.XmlNoc;

/**
 * Utility class that adds NoC components and connects them to the design
 * 
 * @author Mohamed
 *
 */
public class NocInterconnect {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    public static void addNoc(Design design) throws ParserConfigurationException, SAXException, IOException {
        log.info("Adding NoC circuitry...");

        insertNocInterface(design);

        insertTranslators(design);
    }

    private static void insertNocInterface(Design design) throws ParserConfigurationException, SAXException,
            IOException {
        Noc nocInterface = XmlNoc.readXMLNoC("designs/noc.xml");

        // parameters
        nocInterface.addParameter(new Parameter("WIDTH_NOC", "150"));
        nocInterface.addParameter(new Parameter("WIDTH_RTL", "600"));
        nocInterface.addParameter(new Parameter("N", "16"));
        nocInterface.addParameter(new Parameter("NUM_VC", "2"));
        nocInterface.addParameter(new Parameter("DEPTH_PER_VC", "16"));
        nocInterface.addParameter(new Parameter("VERBOSE", "1"));
        nocInterface.addParameter(new Parameter("VC_ADDRESS_WIDTH", "$clog2(NUM_VC)"));
        nocInterface.addParameter(new Parameter("[VC_ADDRESS_WIDTH-1:0] ASSIGNED_VC [0:N-1]",
                "'{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}"));

        // ports
        nocInterface.addPort(new Port("clk_noc", Direction.INPUT, 1, 1, nocInterface));
        nocInterface.addPort(new Port("rst", Direction.INPUT, 1, 1, nocInterface));
        nocInterface.addPort(new Port("clk_rtl", Direction.INPUT, 16, 1, nocInterface));
        nocInterface.addPort(new Port("clk_int", Direction.INPUT, 16, 1, nocInterface));

        nocInterface.addPort(new Port("i_packets_in", Direction.INPUT, 600, 16, nocInterface));
        nocInterface.addPort(new Port("i_valids_in", Direction.INPUT, 1, 16, nocInterface));
        nocInterface.addPort(new Port("i_readys_out", Direction.OUTPUT, 1, 16, nocInterface));

        nocInterface.addPort(new Port("o_packets_out", Direction.OUTPUT, 600, 16, nocInterface));
        nocInterface.addPort(new Port("o_valids_out", Direction.OUTPUT, 1, 16, nocInterface));
        nocInterface.addPort(new Port("o_readys_in", Direction.INPUT, 1, 16, nocInterface));

        design.setFabricInterface(nocInterface);
    }

    private static void insertTranslators(Design design) {

        // loop over all modules and insert translators
        for (DesignModule mod : design.getModules().values()) {
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
        Translator packetizer = new Translator("packetizer", mod.getName() + "_pkt", mod, bun);

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
        Translator depacketizer = new Translator("depacketizer", mod.getName() + "_depkt", mod, bun);

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

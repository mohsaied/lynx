package lynx.hdlgen;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.Module;
import lynx.data.Port;
import lynx.data.MyEnums.Direction;
import lynx.data.Wire;
import lynx.main.ReportData;

/**
 * Functions to write design to Verilog
 * 
 * @author Mohamed
 *
 */
public class VerilogOut {

    private static final Logger log = Logger.getLogger(VerilogOut.class.getName());

    public static void writeVerilogTestBench(Design design) throws FileNotFoundException, UnsupportedEncodingException {

        log.info("Writing out design to " + design.getType() + ".v");

        PrintWriter writer = ReportData.getInstance().getVerilogFile();

        writePreamble(design, writer);

        writeInterface(design, writer);

        writeWires(design, writer);

        writeModules(design, writer);

        writePostamble(writer);

        ReportData.getInstance().closeVerilogFile();
    }

    private static void writePostamble(PrintWriter writer) {
        writer.println("endmodule");
    }

    private static void writeWires(Design design, PrintWriter writer) {
        // loop over top-level wires and will probably need to create wires
        // and assign statements for the outputs to avoid reg/wire problems

        /*
         * writer.println("//wires for the top-level"); for (Port por :
         * design.getPorts().values()) { if (por.getDirection() ==
         * Direction.OUTPUT) { writeWire(por, writer); } } writer.println();
         */

        // loop over all ports in the design, create a wire for each output port
        // it may be feeding multiple input ports -- that's why

        for (Module mod : design.getAllModules()) {
            writer.println("//wires for the outputs in Module " + mod.getName());
            for (Port por : mod.getUsedPortList()) {
                if (por.getDirection() == Direction.OUTPUT) {
                    writeWire(por, writer);
                }
            }
            writer.println();
        }
    }

    private static void writeWire(Port por, PrintWriter writer) {
        String widthPart = getWidthPart(por);
        String arrayWidthPart = getArrayWidthPart(por);
        writer.println("wire" + widthPart + por.getFullNameDash() + "_wire" + arrayWidthPart + ";");
    }

    private static void writeModules(Design design, PrintWriter writer) {
        for (Module mod : design.getAllModules()) {

            writer.println(mod.getType() + " " + mod.getName() + " (");

            List<Port> porList = mod.getUsedPortList();
            int numPorts = porList.size();

            for (Port por : porList) {
                writer.print("\t." + por.getName() + "(" + figureOutPortConnection(por) + ")");
                if (numPorts-- == 1)
                    writer.println("");
                else
                    writer.println(",");
            }

            writer.println(");");
            writer.println();
        }
    }

    private static String figureOutPortConnection(Port por) {
        String connectionString = "ERROR";
        if (por.getDirection() == Direction.OUTPUT) {
            connectionString = por.getConnectingWireName();
        } else if (por.getDirection() == Direction.INPUT) {
            // first case: both this port and the wire feeding it are same size
            if (por.getWires().size() == 1 && por.getWidth() == por.getWires().get(0).getDstPort().getWidth()) {
                connectionString = por.getConnectingWireName();
                // second case: this port is small, port feeding it is larger
            } else if (por.getWires().size() == 1 && por.getWidth() < por.getWires().get(0).getDstPort().getWidth()) {
                Wire connectingWire = por.getFeedingWire();
                String partSelect = "[" + connectingWire.getDstPortEnd() + ":" + connectingWire.getDstPortStart() + "]";
                connectionString = por.getConnectingWireName() + partSelect;
                // third case: this port is large, and one-or-more smaller ports
                // feed it, possibly needing padding
            } else if (por.getWidth() > por.getWires().get(0).getDstPort().getWidth()) {
                List<Wire> wires = por.getWires();
                connectionString = "{";
                int currBit = por.getWidth() - 1;
                while (!wires.isEmpty()) {
                    // where does the biggest wire start at?
                    int maxEnd = -1;
                    int maxStart = -1;
                    Wire currWire = null;
                    for (Wire wire : wires) {
                        if (wire.getDstPortEnd() > maxEnd) {
                            maxEnd = wire.getDstPortEnd();
                            maxStart = wire.getDstPortStart();
                            currWire = wire;
                        }
                    }

                    // is the maxEnd at our current bit? if not enter padding
                    if (currBit != maxEnd) {
                        int padSize = currBit - maxEnd;
                        connectionString += "{" + padSize + "{1'b0}},";
                    }

                    // output the current wire and remove from list
                    connectionString += por.getConnectingWireName(currWire);
                    wires.remove(currWire);

                    if (maxStart != 0)
                        connectionString += ",";

                    currBit = maxStart;
                }

                // final padding
                if (currBit != 0) {
                    int padSize = currBit + 1;
                    connectionString += "{" + padSize + "{1'b0}}";
                }

                connectionString += "}";
            }
        }

        return connectionString;
    }

    private static void writeInterface(Design design, PrintWriter writer) {
        writer.println("module tb_" + design.getType());
        writer.println("(");

        int numPorts = design.getPorts().size();

        for (Port intPort : design.getPorts().values()) {
            String widthPart = getWidthPart(intPort);
            String arrayWidthPart = getArrayWidthPart(intPort);
            writer.print("\t" + intPort.getDirection() + widthPart + intPort.getName() + arrayWidthPart);
            if (numPorts-- == 1)
                writer.println("");
            else
                writer.println(",");
        }

        writer.println(");");
        writer.println();
    }

    private static String getArrayWidthPart(Port por) {
        return por.getArrayWidth() > 1 ? " [0:" + (por.getArrayWidth() - 1) + "]" : "";
    }

    private static String getWidthPart(Port por) {
        return por.getWidth() > 1 ? " [" + (por.getWidth() - 1) + ":" + "0] " : " ";
    }

    private static void writePreamble(Design design, PrintWriter writer) {
        writer.println("// auto-generated by noclynx");
        writer.println();
    }

}

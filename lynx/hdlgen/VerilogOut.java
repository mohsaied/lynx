package lynx.hdlgen;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.Module;
import lynx.data.MyEnums.PortType;
import lynx.data.Noc;
import lynx.data.Parameter;
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

    public static void writeVerilogTestBench(Design design, Noc noc) throws FileNotFoundException, UnsupportedEncodingException {

        log.info("Writing out design to " + design.getType() + ".v");

        PrintWriter writer = ReportData.getInstance().getVerilogFile();

        writePreamble(design, writer);

        writeSimInterface(design, writer);

        writeClockingAndReset(noc, writer);

        writeWires(design, writer);

        writeModules(design, writer);

        writePostamble(writer);

        ReportData.getInstance().closeVerilogFile();
    }

    private static void writeClockingAndReset(Noc noc, PrintWriter writer) {
        writer.println("//clocking");
        writer.println("initial " + noc.getNocClock().getName() + "  = 1'b1;");
        writer.println("initial " + noc.getNocQuarterClock().getName() + "  = 1'b1;");
        writer.print("initial " + noc.getIntClock().getName() + " = " + noc.getIntClock().getWidth() + "'b");
        for (int i = 0; i < noc.getIntClock().getWidth(); i++)
            writer.print("1");
        writer.println(";");
        writer.print("initial " + noc.getRtlClock().getName() + " = " + noc.getRtlClock().getWidth() + "'b");
        for (int i = 0; i < noc.getRtlClock().getWidth(); i++)
            writer.print("1");
        writer.println(";");
        writer.println("always #1    " + noc.getNocClock().getName() + " = ~" + noc.getNocClock().getName() + ";");
        writer.println("always #4    " + noc.getNocQuarterClock().getName() + " = ~" + noc.getNocQuarterClock().getName() + ";");
        writer.println("always #1.25 " + noc.getIntClock().getName() + " = ~" + noc.getIntClock().getName() + "; ");
        writer.println("always #5    " + noc.getRtlClock().getName() + " = ~" + noc.getRtlClock().getName() + "; ");
        writer.println();
        writer.println("//reset ");
        writer.println("initial begin");
        writer.println("    " + noc.getNocRst().getName() + " = 1'b1;");
        writer.println("    #25;");
        writer.println("    " + noc.getNocRst().getName() + " = 1'b0;");
        writer.println("end");
        writer.println();
    }

    private static void writePostamble(PrintWriter writer) {
        writer.println("endmodule");
    }

    private static void writeWires(Design design, PrintWriter writer) {

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

            writer.print(mod.getType());

            if (mod.getParameters().size() != 0) {
                writer.println();
                writer.println("#(");
                for (int i = 0; i < mod.getParameters().size(); i++) {
                    Parameter par = mod.getParameters().get(i);
                    if (i == mod.getParameters().size() - 1)
                        writer.println("    ." + par.getName() + "(" + par.getValue() + ")");
                    else
                        writer.println("    ." + par.getName() + "(" + par.getValue() + "),");
                }
                writer.println(")");
                writer.println(mod.getName());
            } else {
                writer.println(" " + mod.getName());
            }

            writer.println("(");
            List<Port> porList = mod.getUsedPortList();
            int numPorts = porList.size();

            for (Port por : porList) {
                if (!por.isGlobal())
                    writer.print("\t." + por.getName() + "(" + figureOutPortConnection(por) + ")");
                else
                    writer.print("\t." + por.getName() + "(" + por.getGlobalPortName() + ")");
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
                        if (wire.getSrcPortEnd() > maxEnd) {
                            maxEnd = wire.getSrcPortEnd();
                            maxStart = wire.getSrcPortStart();
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

                    currBit = maxStart - 1;
                }

                // final padding
                if (currBit > 0) {
                    int padSize = currBit + 1;
                    connectionString += "{" + padSize + "{1'b0}}";
                }

                connectionString += "}";
                // fourth case: this is a valid port in an noc port which has
                // multiple bundles going to it -- we'll OR all the valids to
                // accept data from any of the inputs that are sharing this port
            } else if (por.getType() == PortType.VALID && por.getWires().size() > 1) {
                connectionString = "";
                for (Wire wire : por.getWires()) {
                    connectionString += por.getConnectingWireName(wire) + "|";
                }
                connectionString = connectionString.substring(0, connectionString.length() - 1);
            } else if (por.getType() == PortType.DONE && por.getWires().size() > 1) {
                connectionString = "";
                for (Wire wire : por.getWires()) {
                    connectionString += por.getConnectingWireName(wire) + "|";
                }
                connectionString = connectionString.substring(0, connectionString.length() - 1);
            }
        }

        return connectionString;
    }

    @SuppressWarnings("unused")
    private static void writeDesignInterface(Design design, PrintWriter writer) {
        writer.println("module " + design.getName());
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

    private static void writeSimInterface(Design design, PrintWriter writer) {
        writer.println("`timescale 1ns/1ps");
        writer.println("module testbench();");
        writer.println();

        // TODO all global ports seem to be exported to top-level during
        // parsing, but will probably have to get rid of that since clustering
        // and mapping change some of the ports significantly

        for (Port intPort : design.getPorts().values()) {
            String widthPart = getWidthPart(intPort);
            String arrayWidthPart = getArrayWidthPart(intPort);
            if (!intPort.isGlobalOnNoc()) {
                writer.println("logic " + widthPart + intPort.getName() + arrayWidthPart + ";");
            }
        }

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
        writer.println("// testbench for design " + design.getName());
    }

}

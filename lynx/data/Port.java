package lynx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lynx.data.MyEnums.*;

/**
 * The input/output ports of a Verilog module.
 * 
 * @author Mohamed
 * 
 */
public class Port {

    private static final Logger log = Logger.getLogger(Design.class.getName());

    private Direction direction;
    private String name;
    private int width;
    /** for 2D verilog ports -- not used for now */
    private int arrayWidth;
    private PortType type;

    private Module parentModule;
    private List<Wire> wires;

    private boolean isBundled;
    private boolean isGlobal;
    private String globalPortName;

    public Port() {
        this(null, Direction.UNKNOWN, 0, 1, PortType.UNKNOWN, null, false, null);
    }

    public Port(String name, Direction direction, Module parentModule) {
        this(name, direction, 1, 1, PortType.UNKNOWN, parentModule, false, null);
    }

    public Port(String name, Direction direction, Module parentModule, String globalPortName) {
        this(name, direction, 1, 1, PortType.UNKNOWN, parentModule, false, null);
    }

    public Port(String name, Direction direction, PortType type, Module parentModule, String globalPortName) {
        this(name, direction, 1, 1, type, parentModule, false, globalPortName);
    }

    public Port(String name, Direction direction, int width, Module parentModule) {
        this(name, direction, width, 1, PortType.UNKNOWN, parentModule, false, null);
    }

    public Port(String name, Direction direction, int width, int arrayWidth, PortType type, Module parentModule) {
        this(name, direction, width, arrayWidth, type, parentModule, false, null);
    }

    public Port(String name, Direction direction, int width, int arrayWidth, PortType type, Module parentModule, boolean isBundled) {
        this(name, direction, width, arrayWidth, type, parentModule, isBundled, null);
    }

    public Port(String name, Direction direction, int width, int arrayWidth, PortType type, Module parentModule,
            boolean isBundled, String globalPortName) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        this.arrayWidth = arrayWidth;
        this.type = type;
        this.parentModule = parentModule;
        this.wires = new ArrayList<Wire>();
        this.isBundled = isBundled;
        this.isGlobal = globalPortName != null;
        this.setGlobalPortName(globalPortName);
        assert ((isBundled) && (globalPortName == null)) || !isBundled : "Bundled ports cannot be exported to top level!";
    }

    public Port(Port basePort, Design design) {
        this(basePort.globalPortName, basePort.direction, basePort.width, basePort.arrayWidth, basePort.type, design, false, null);
    }

    public final Direction getDirection() {
        return direction;
    }

    public String getDirectionString() {
        return direction.toString();
    }

    public final void setDirection(Direction direction) {
        this.direction = direction;
    }

    public final String getName() {
        return name;
    }

    public final String getFullNameDot() {
        return parentModule.getName() + "." + name;
    }

    public final String getFullNameDash() {
        return parentModule.getName() + "_" + name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final int getWidth() {
        return width;
    }

    public final void setWidth(int width) {
        this.width = width;
    }

    public final int getArrayWidth() {
        return arrayWidth;
    }

    public final void setArrayWidth(int arrayWidth) {
        this.arrayWidth = arrayWidth;
    }

    public PortType getType() {
        return type;
    }

    public String getTypeString() {
        return type.toString();
    }

    public void setType(PortType type) {
        this.type = type;
    }

    public final Module getParentModule() {
        return parentModule;
    }

    public final void setParentModule(Module parentModule) {
        this.parentModule = parentModule;
    }

    public final List<Wire> getWires() {
        return wires;
    }

    public final void addWire(Port dstPort) {
        assert ((dstPort.getDirection() != this.getDirection()) || (this.parentModule instanceof Design)) : "Attempting to connect "
                + dstPort.getFullNameDot() + " and " + getFullNameDot() + " of same direction " + this.getDirection();
        assert dstPort.getWidth() == this.getWidth() : "Attempting to connect " + dstPort.getFullNameDot() + " and "
                + getFullNameDot() + " of different widths " + dstPort.getWidth() + " and " + this.getWidth();
        this.wires.add(new Wire(this, dstPort));
    }

    public final void addWire(Port dstPort, int srcPortStart, int srcPortEnd, int dstPortStart, int dstPortEnd) {
        assert ((dstPort.getDirection() != this.getDirection()) || (this.parentModule instanceof Design)) : "Attempting to connect "
                + dstPort.getFullNameDot() + " and " + getFullNameDot() + " of same direction " + this.getDirection();
        this.wires.add(new Wire(dstPort, srcPortStart, srcPortEnd, dstPortStart, dstPortEnd));
    }

    public boolean isBundled() {
        return isBundled;
    }

    public void setBundled(boolean isBundled) {
        this.isBundled = isBundled;
    }

    public final boolean isGlobal() {
        return isGlobal;
    }

    public final void setGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public final String getGlobalPortName() {
        return globalPortName;
    }

    public final void setGlobalPortName(String globalPortName) {
        this.globalPortName = globalPortName;
    }

    public final String getConnectingWireName() {

        // first check if this is a conduit, we'll need to connect it to the
        // top-level port
        if (this.isGlobal) {
            return globalPortName;
        } else {
            // if this port isn't connected to the top-level, then we should
            // find which wire connects it
            if (direction == Direction.OUTPUT) {
                return this.getFullNameDash() + "_wire";
            } else if (direction == Direction.INPUT) {
                if (wires.size() == 1) {
                    return getConnectingWireName(wires.get(0));
                } else {
                    // TODO we CAN actually have multiple wires feeding this
                    // port if each of them only feeds part of the port and not
                    // the whole thing
                    assert wires.size() <= 1 : "Input port " + this.getFullNameDash() + " cannot have multiple (" + wires.size()
                            + ") drivers";
                    if (!(this.parentModule instanceof Noc))
                        log.warning("Input port " + this.getFullNameDash() + " is unconnected");
                }
            }
        }
        return "";
    }

    public String getConnectingWireName(Wire wire) {
        return wire.getDstPort().getFullNameDash() + "_wire";
    }

    @Override
    public Port clone() {
        Port por = new Port(name, direction, width, arrayWidth, type, parentModule, isBundled, globalPortName);
        // TODO find a way to clone wires as well
        return por;
    }

    @Override
    public String toString() {
        String s = "port: " + direction + " " + name + "(" + width + ")";
        return s;
    }

    public Wire getFeedingWire() {
        assert direction == Direction.INPUT : "Cannot get feeding port for an output port!";
        if (wires.size() == 1) {
            return wires.get(0);
        } else {
            assert wires.size() <= 1 : "Input port " + this.getFullNameDash() + " cannot have multiple (" + wires.size()
                    + ") drivers";
        }
        return null;
    }
}

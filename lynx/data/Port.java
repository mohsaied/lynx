package lynx.data;

import java.util.ArrayList;
import java.util.List;

import lynx.data.MyEnums.*;

/**
 * The input/output ports of a Verilog module.
 * 
 * @author Mohamed
 * 
 */
public class Port {

    private Direction direction;
    private String name;
    private int width;
    /** for 2D verilog ports -- not used for now */
    private int arrayWidth;
    private PortType type;

    private Module parentModule;
    private List<Port> wires;

    private boolean isBundled;
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
        this.wires = new ArrayList<Port>();
        this.isBundled = isBundled;
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

    public final List<Port> getWires() {
        return wires;
    }

    public final void addWire(Port wire) {
        assert ((wire.getDirection() != this.getDirection()) || (this.parentModule instanceof Design)) : "Attempting to connect "
                + wire.getFullNameDot() + " and " + getFullNameDot() + " of same direction " + this.getDirection();
        assert wire.getWidth() == this.getWidth() : "Attempting to connect " + wire.getFullNameDot() + " and " + getFullNameDot()
                + " of different widths " + wire.getWidth() + " and " + this.getWidth();
        this.wires.add(wire);
    }

    public boolean isBundled() {
        return isBundled;
    }

    public void setBundled(boolean isBundled) {
        this.isBundled = isBundled;
    }

    public final String getGlobalPortName() {
        return globalPortName;
    }

    public final void setGlobalPortName(String globalPortName) {
        this.globalPortName = globalPortName;
    }

    @Override
    public String toString() {
        String s = "port: " + direction + " " + name + "(" + width + ")";
        return s;
    }

}

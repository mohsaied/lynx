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
    /** for 2D verilog ports */
    private int arrayWidth;
    private PortType type;

    private Module parentModule;
    private List<Port> wires;

    private boolean isBundled;

    public Port() {
        this(null, Direction.UNKNOWN, 0, 1, PortType.UNKNOWN, null, false);
    }

    public Port(String name, Direction direction, int width, int arrayWidth, Module parentModule) {
        this(name, direction, width, arrayWidth, PortType.UNKNOWN, parentModule, false);
    }

    public Port(String name, Direction direction, int width, int arrayWidth, PortType type, Module parentModule) {
        this(name, direction, width, arrayWidth, type, parentModule, false);
    }

    public Port(String name, Direction direction, int width, int arrayWidth, PortType type, Module parentModule,
            boolean isBundled) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        this.arrayWidth = arrayWidth;
        this.type = type;
        this.parentModule = parentModule;
        this.wires = new ArrayList<Port>();
        this.isBundled = isBundled;
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

    public final void addWire(Port wires) {
        this.wires.add(wires);
    }

    public boolean isBundled() {
        return isBundled;
    }

    public void setBundled(boolean isBundled) {
        this.isBundled = isBundled;
    }

    @Override
    public String toString() {
        String s = "port: " + direction + " " + name + "(" + width + ")";
        return s;
    }

}

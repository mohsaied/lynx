package lynx.data;

import java.util.ArrayList;
import java.util.List;

/**
 * The input/output ports of a Verilog module.
 * 
 * @author Mohamed
 * 
 */
public class Port {

    private String direction;
    private String name;
    private int width;
    /**
     * for 2D verilog ports
     */
    private int arrayWidth;
    private Module parentModule;

    private List<Port> connections;

    public Port() {
        name = null;
        direction = null;
        width = 0;
        arrayWidth = 1;
        parentModule = null;
        connections = new ArrayList<Port>();
    }

    public Port(String name, String direction, int width) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        arrayWidth = 1;
        parentModule = null;
        connections = new ArrayList<Port>();
    }

    public Port(String name, String direction, int width, Module parentModule) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        arrayWidth = 1;
        this.parentModule = parentModule;
        connections = new ArrayList<Port>();
    }

    public Port(String name, String direction, int width, int arrayWidth) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        this.arrayWidth = arrayWidth;
        parentModule = null;
        connections = new ArrayList<Port>();
    }

    public Port(String name, String direction, int width, int arrayWidth, Module parentModule) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        this.arrayWidth = arrayWidth;
        this.parentModule = parentModule;
        connections = new ArrayList<Port>();
    }

    public final String getDirection() {
        return direction;
    }

    public final void setDirection(String direction) {
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

    public final Module getParentModule() {
        return parentModule;
    }

    public final void setParentModule(Module parentModule) {
        this.parentModule = parentModule;
    }

    public final List<Port> getConnections() {
        return connections;
    }

    public final void setConnections(List<Port> connections) {
        this.connections = connections;
    }

    public final void addConnection(Port por) {
        this.connections.add(por);
    }

    @Override
    public String toString() {
        String s = "port: " + direction + " " + name + "(" + width + ")";
        if (connections.size() != 0) {
            s += ", connects to: ";
            for (Port por : connections)
                s += por.getName() + " in " + por.getParentModule().getName();
        }
        return s;
    }

}

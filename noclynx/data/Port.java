package noclynx.data;

import java.util.ArrayList;
import java.util.List;

/**
 * The input/output ports of a Verilog module.
 * 
 * @author Mohamed
 * 
 */
public class Port {

    String direction;
    String name;
    int width;
    private List<Port> connections;
    Module parentModule;

    public Port() {
        name = null;
        direction = null;
        width = 0;
        parentModule = null;
        connections = new ArrayList<Port>();
    }

    public Port(String name, String direction, int width) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        parentModule = null;
        this.connections = new ArrayList<Port>();
    }

    public Port(String name, String direction, int width, Module parentModule) {
        this.name = name;
        this.direction = direction;
        this.width = width;
        this.parentModule = parentModule;
        this.connections = new ArrayList<Port>();
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Module getParentModule() {
        return parentModule;
    }

    public void setParentModule(Module parentModule) {
        this.parentModule = parentModule;
    }

    public List<Port> getConnections() {
        return connections;
    }

    public void setConnections(List<Port> connections) {
        this.connections = connections;
    }

    public void addConnection(Port por) {
        this.connections.add(por);
    }

    @Override
    public String toString() {
        String s = "port: " + direction + " " + name + "(" + width + ")";
        if (connections.size() != 0) {
            s += ", connects to: ";
            for (Port por : connections)
                s += por.getName() + " ";
        }
        return s;
    }
}

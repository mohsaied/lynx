package nocsys.data;

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
    List<Port> connections;

    public Port() {
        name = null;
        direction = null;
        width = 0;
        connections = new ArrayList<Port>();
    }

    public Port(String name, String direction, int width) {
        this.name = name;
        this.direction = direction;
        this.width = width;
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

    public List<Port> getConnections() {
        return connections;
    }

    public void setConnections(List<Port> connections) {
        this.connections = connections;
    }
    
    public void addConnection(Port por){
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

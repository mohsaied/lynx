package noclynx.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Module Ports are ports associated with a module and not the top level Design.
 * 
 * @author Mohamed
 *
 */
public class ModulePort extends Port {

    private List<ModulePort> connections;
    private Module parentModule;

    public ModulePort() {
        super();
        parentModule = null;
        connections = new ArrayList<ModulePort>();
    }

    public ModulePort(String name, String direction, int width) {
        super(name, direction, width);
        parentModule = null;
        connections = new ArrayList<ModulePort>();
    }

    public ModulePort(String name, String direction, int width, Module parentModule) {
        super(name, direction, width);
        this.parentModule = parentModule;
        this.connections = new ArrayList<ModulePort>();
    }

    public final Module getParentModule() {
        return parentModule;
    }

    public final void setParentModule(Module parentModule) {
        this.parentModule = parentModule;
    }

    public final List<ModulePort> getConnections() {
        return connections;
    }

    public final void setConnections(List<ModulePort> connections) {
        this.connections = connections;
    }

    public final void addConnection(ModulePort por) {
        this.connections.add(por);
    }

    public final String getFullName() {
        return parentModule.getName() + "." + getName();
    }

    @Override
    public String toString() {
        String s = "port: " + direction + " " + name + "(" + width + ")";
        if (connections.size() != 0) {
            s += ", connects to: ";
            for (ModulePort por : connections)
                s += por.getName() + " in " + por.getParentModule().getName();
        }
        return s;
    }
}

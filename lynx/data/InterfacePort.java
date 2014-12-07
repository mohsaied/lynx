package lynx.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface ports are pulled out to the top-level
 * 
 * @author Mohamed
 *
 */
public class InterfacePort extends Port {

    private List<ModulePort> physicalPorts;

    public InterfacePort() {
        super();
        physicalPorts = new ArrayList<ModulePort>();
    }

    public InterfacePort(String name, String direction, ModulePort physicalPort) {
        super(name, direction, physicalPort.width);
        physicalPorts = new ArrayList<ModulePort>();
        physicalPorts.add(physicalPort);
    }

    public final List<ModulePort> getPhysicalPorts() {
        return physicalPorts;
    }

    public final void setPhysicalPorts(List<ModulePort> physicalPorts) {
        this.physicalPorts = physicalPorts;
    }

    public final void addPhysicalPort(ModulePort physicalPort) {
        this.physicalPorts.add(physicalPort);
    }

    public String toString() {
        String s = "Interface port: " + direction + " " + name + "(" + width + ") // connects to ";
        for (ModulePort physicalPort : physicalPorts)
            s += physicalPort.getName() + " in " + physicalPort.getParentModule().getName();
        return s;
    }

}

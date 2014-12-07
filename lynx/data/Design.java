package lynx.data;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A top-level design.
 * 
 * @author Mohamed
 *
 */
public class Design {

    private static final Logger log = Logger.getLogger(Design.class.getName());

    private String name;
    private Map<String, Module> modules;
    private Map<String, InterfacePort> interfacePorts;

    public Design() {
        this.name = null;
        this.modules = new HashMap<String, Module>();
        this.interfacePorts = new HashMap<String, InterfacePort>();
        log.info("Creating new design with no name");
    }

    public Design(String name) {
        this.name = name;
        this.modules = new HashMap<String, Module>();
        this.interfacePorts = new HashMap<String, InterfacePort>();
        log.info("Creating new design: " + name);
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final Map<String, Module> getModules() {
        return modules;
    }

    public final void setModules(Map<String, Module> modules) {
        this.modules = modules;
    }

    public final void addModule(Module currModule) {
        this.modules.put(currModule.getName(), currModule);
    }

    public final int getNumModules() {
        return modules.size();
    }

    public final Module getModuleByName(String modName) {
        return this.modules.get(modName);
    }

    public final Map<String, InterfacePort> getInterfacePorts() {
        return interfacePorts;
    }

    public final void setInterfacePorts(Map<String, InterfacePort> interfacePorts) {
        this.interfacePorts = interfacePorts;
    }

    public final void addInterfacePort(InterfacePort intPort) {
        if (!this.interfacePorts.containsKey(intPort.getName()))
            this.interfacePorts.put(intPort.getName(), intPort);
    }

    public void addInterfacePort(String name, String direction, ModulePort physicalPort) {
        if (this.interfacePorts.containsKey(name)) {
            this.interfacePorts.get(name).addPhysicalPort(physicalPort);
        } else {
            InterfacePort intPort = new InterfacePort(name, direction, physicalPort);
            this.interfacePorts.put(name, intPort);
        }
    }

    @Override
    public String toString() {
        String s = "Design: " + name + "\n\n";
        for (InterfacePort por : this.interfacePorts.values())
            s += por + "\n";
        s += "\n";
        for (Module mod : this.modules.values())
            s += mod + "\n";
        return s;
    }

}

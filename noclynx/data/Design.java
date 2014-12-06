package noclynx.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    String name;
    Map<String, Module> modules;
    List<InterfacePort> interfacePorts;

    public Design() {
        this.name = null;
        this.modules = new HashMap<String, Module>();
        this.interfacePorts = new ArrayList<InterfacePort>();
        log.info("Creating new design with no name");
    }

    public Design(String name) {
        this.name = name;
        this.modules = new HashMap<String, Module>();
        this.interfacePorts = new ArrayList<InterfacePort>();
        log.info("Creating new design: " + name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Module> getModules() {
        return modules;
    }

    public void setModules(Map<String, Module> modules) {
        this.modules = modules;
    }

    public void addModule(Module currModule) {
        this.modules.put(currModule.getName(), currModule);
    }

    public int getNumModules() {
        return modules.size();
    }

    public Module getModuleByName(String modName) {
        return this.modules.get(modName);
    }

    public List<InterfacePort> getInterfacePorts() {
        return interfacePorts;
    }

    public void setInterfacePorts(List<InterfacePort> interfacePorts) {
        this.interfacePorts = interfacePorts;
    }

    public void addInterfacePort(InterfacePort intPort) {
        this.interfacePorts.add(intPort);
    }

    @Override
    public String toString() {
        String s = "Design: " + name + "\n\n";
        for (InterfacePort por : this.interfacePorts)
            s += por + "\n\n";
        for (Module mod : this.modules.values())
            s += mod + "\n";
        return s;
    }

}

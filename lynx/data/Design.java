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
public class Design extends Module {

    private static final Logger log = Logger.getLogger(Design.class.getName());

    private Map<String, Module> modules;

    public Design() {
        super();
        this.modules = new HashMap<String, Module>();
        log.info("Creating new design with no name");
    }

    public Design(String name) {
        super("top", name);
        this.modules = new HashMap<String, Module>();
        log.info("Creating new design: " + name);
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

    @Override
    public String toString() {
        String s = "Design: " + name + "\n\n";
        for (Port por : this.ports.values())
            s += por + "\n";
        s += "\n";
        for (Module mod : this.modules.values())
            s += mod + "\n";
        return s;
    }

}

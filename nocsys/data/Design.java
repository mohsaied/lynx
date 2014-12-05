package nocsys.data;

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
    Map<String,Module> modules;

    public Design() {
        this.name = null;
        this.modules = new HashMap<String,Module>();
        log.info("Creating new design with no name");
    }

    public Design(String name) {
        this.name = name;
        this.modules = new HashMap<String,Module>();
        log.info("Creating new design: " + name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String,Module> getModules() {
        return modules;
    }

    public void setModules(Map<String,Module> modules) {
        this.modules = modules;
    }

    public void addModule(Module currModule) {
        this.modules.put(currModule.getName(),currModule);
    }

    public int getNumModules() {
        return modules.size();
    }
    
    public Module getModuleByName(String modName){
        return this.modules.get(modName);
    }

    @Override
    public String toString() {
        String s = "Design: " + name + "\n\n";
        for (Module mod : this.modules.values())
            s += mod + "\n";
        return s;
    }

}

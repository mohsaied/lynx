package nocsys.data;

import java.util.ArrayList;
import java.util.List;
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
    List<Module> modules;

    public Design() {
        this.name = null;
        this.modules = new ArrayList<Module>();
        log.info("Creating new design with no name");
    }

    public Design(String name) {
        this.name = name;
        this.modules = new ArrayList<Module>();
        log.info("Creating new design: " + name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    public void addModule(Module currModule) {
        this.modules.add(currModule);
    }

    public int getNumModules() {
        return modules.size();
    }

    @Override
    public String toString() {
        String s = "Design: " + name + "\n\n";
        for (Module mod : this.modules)
            s += mod + "\n";
        return s;
    }

}

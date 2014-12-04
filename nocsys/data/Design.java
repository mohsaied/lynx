package nocsys.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A top-level design.
 * 
 * @author Mohamed
 *
 */
public class Design {

    String name;
    List<Module> modules;

    public Design() {
        this.name = null;
        this.modules = new ArrayList<Module>();
    }
    
    public Design(String name) {
        this.name = name;
        this.modules = new ArrayList<Module>();
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
    
    public int getNumModules(){
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

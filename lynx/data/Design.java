package lynx.data;

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
public class Design extends Module {

    private static final Logger log = Logger.getLogger(Design.class.getName());

    private Map<String, Module> modules;

    private Module fabricInterface;

    private List<Module> translators;

    public Design() {
        this(null);
    }

    public Design(String name) {
        super(name, name + "_inst");
        this.modules = new HashMap<String, Module>();
        this.fabricInterface = null;
        this.translators = new ArrayList<Module>();
        log.info("Creating new design: " + name);
    }

    public final Map<String, Module> getModules() {
        return modules;
    }

    public final void addModule(Module currModule) {
        this.modules.put(currModule.getName(), currModule);
    }

    public final int getNumModules() {
        return modules.size();
    }

    public final List<Module> getAllModules() {
        List<Module> allModules = new ArrayList<Module>();

        allModules.addAll(modules.values());
        allModules.add(fabricInterface);
        allModules.addAll(translators);

        return allModules;
    }

    public final Module getModuleByName(String modName) {
        return this.modules.get(modName);
    }

    public Module getFabricInterface() {
        return fabricInterface;
    }

    public void setFabricInterface(Module fabricInterface) {
        this.fabricInterface = fabricInterface;
    }

    public List<Module> getTranslators() {
        return translators;
    }

    public void addTranslator(Module translator) {
        this.translators.add(translator);
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

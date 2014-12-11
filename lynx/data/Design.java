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

    private Map<String, DesignModule> modules;

    private Noc nocInterface;

    private List<Translator> translators;

    public Design() {
        this(null);
    }

    public Design(String name) {
        super(name, name + "_inst");
        this.modules = new HashMap<String, DesignModule>();
        this.nocInterface = null;
        this.translators = new ArrayList<Translator>();
        log.info("Creating new design: " + name);
    }

    public final Map<String, DesignModule> getModules() {
        return modules;
    }

    public final void addModule(DesignModule currModule) {
        this.modules.put(currModule.getName(), currModule);
    }

    public final int getNumModules() {
        return modules.size();
    }

    public final List<Module> getAllModules() {
        List<Module> allModules = new ArrayList<Module>();

        allModules.addAll(modules.values());
        allModules.add(nocInterface);
        allModules.addAll(translators);

        return allModules;
    }

    public final Module getModuleByName(String modName) {
        return this.modules.get(modName);
    }

    public Noc getNoc() {
        return nocInterface;
    }

    public void setNoc(Noc nocInterface) {
        this.nocInterface = nocInterface;
    }

    public List<Translator> getTranslators() {
        return translators;
    }

    public void addTranslator(Translator translator) {
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

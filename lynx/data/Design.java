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
public final class Design extends Module {

    private static final Logger log = Logger.getLogger(Design.class.getName());

    private Map<String, DesignModule> modules;

    private Noc noc;

    private List<Translator> translators;

    public Design() {
        this(null);
    }

    public Design(String name) {
        super(name, name + "_inst");
        this.modules = new HashMap<String, DesignModule>();
        this.noc = null;
        this.translators = new ArrayList<Translator>();
        log.info("Creating new design: " + name);
    }

    public final Map<String, DesignModule> getDesignModules() {
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
        if (noc != null)
            allModules.add(noc);
        if (!translators.isEmpty())
            allModules.addAll(translators);

        return allModules;
    }

    public final Module getModuleByName(String modName) {
        return this.modules.get(modName);
    }

    public Noc getNoc() {
        return noc;
    }

    public void setNoc(Noc noc) {
        this.noc = noc;
    }

    public List<Translator> getTranslators() {
        return translators;
    }

    public void addTranslator(Translator translator) {
        this.translators.add(translator);
    }

    @Override
    public void addPort(Port wire) {
        // if we dont have a port with the same name
        // create a new one and add to it a wire
        if (!this.getPorts().containsKey(wire.getGlobalPortName())) {
            Port topPort = new Port(wire, this);
            topPort.addWire(wire);
            super.addPort(topPort);
        } else {
            // if we do have a port, we'll just add another wire to it
            Port topPort = this.getPortByName(wire.getGlobalPortName());
            topPort.addWire(wire);
        }
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

    public double[][] getAdjacencyMatrix() {
        int numModules = modules.size();
        double[][] matrix = new double[numModules][numModules];
        // init matrix
        for (int i = 0; i < numModules; i++)
            for (int j = 0; j < numModules; j++)
                matrix[i][j] = 0;

        // good idea to associate a number with a module name
        // the number is the matrix entry location
        Map<String, Integer> moduleNames = new HashMap<String, Integer>();
        int i = 0;
        for (String modName : getDesignModules().keySet()) {
            moduleNames.put(modName, new Integer(i++));
        }

        for (String modName : moduleNames.keySet()) {

            // current module
            DesignModule currModule = (DesignModule) getModuleByName(modName);
            // current module index
            i = moduleNames.get(modName);

            // find all modules connected to module i
            List<String> conMods = currModule.getConnectedModuleNames();
            for (String conModName : conMods) {
                int j = moduleNames.get(conModName);
                matrix[i][j] = 1;
            }
        }
        return matrix;
    }
}

package lynx.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A Verilog Module.
 * 
 * @author Mohamed
 *
 */
public abstract class Module {

    private static final Logger log = Logger.getLogger(Module.class.getName());

    protected String type;
    protected String name;
    protected List<Parameter> parameters;
    protected Map<String, Port> ports;

    public Module() {
        this(null, null);
    }

    public Module(String type) {
        this(type, null);
    }

    public Module(String type, String name) {
        this.type = type;
        this.name = name;
        this.parameters = new ArrayList<Parameter>();
        this.ports = new HashMap<String, Port>();
        log.fine("Creating new Module, name: " + name + ", type: " + type);
    }

    public final String getType() {
        return type;
    }

    public final void setType(String type) {
        this.type = type;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final List<Parameter> getParameters() {
        return parameters;
    }

    public final void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public final void editParameter(String parName, String parValue) {
        // find parameter in list and update its value
        for (Parameter par : this.parameters) {
            if (par.getName().equals(parName)) {
                par.setValue(parValue);
            }
        }
    }

    public final Map<String, Port> getPorts() {
        return ports;
    }

    public final Port getPortByName(String porName) {
        return ports.get(porName);
    }

    public void addPort(Port port) {
        this.ports.put(port.getName(), port);
    }

    public List<Port> getUsedPortList() {
        // want to filter out unconnected ports -- we don't need them
        Collection<Port> unfilteredPorList = getPorts().values();

        List<Port> porList = new ArrayList<Port>();
        for (Port por : unfilteredPorList) {
            if (por.getWires().size() != 0 || por.getGlobalPortName() != null)
                porList.add(por);
        }
        return porList;
    }

    @Override
    public String toString() {
        String s = "Module: " + type + " " + name + "\n";
        for (Parameter par : parameters)
            s += par + "\n";
        for (Port por : ports.values())
            s += por + "\n";
        return s;
    }
}

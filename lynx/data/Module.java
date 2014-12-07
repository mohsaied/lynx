package lynx.data;

import java.util.ArrayList;
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
public class Module {

    private static final Logger log = Logger.getLogger(Module.class.getName());

    protected String type;
    protected String name;
    protected List<Parameter> parameters;
    protected Map<String, Port> ports;

    public Module() {
        this.type = null;
        this.name = null;
        this.parameters = new ArrayList<Parameter>();
        this.ports = new HashMap<String, Port>();
        log.info("Creating new Module with no name");
    }

    public Module(String type) {
        this.type = type;
        this.name = type + "_inst";
        this.parameters = new ArrayList<Parameter>();
        this.ports = new HashMap<String, Port>();
        log.info("Creating new Module. name: " + name + ", type: " + type);
    }

    public Module(String type, String name) {
        this.type = type;
        this.name = name;
        this.parameters = new ArrayList<Parameter>();
        this.ports = new HashMap<String, Port>();
        log.info("Creating new Module. name: " + name + ", type: " + type);
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

    public final void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public final void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public final Map<String, Port> getPorts() {
        return ports;
    }

    public final void setPorts(Map<String, Port> ports) {
        this.ports = ports;
    }

    public final Port getPortByName(String porName) {
        return ports.get(porName);
    }

    public final void addPort(Port port) {
        this.ports.put(port.getName(), port);
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
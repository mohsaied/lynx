package nocsys.data;

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

    String type;
    String name;
    List<Parameter> parameters;
    Map<String,Port> ports;

    public Module() {
        this.type = null;
        this.name = null;
        this.parameters = new ArrayList<Parameter>();
        this.ports = new HashMap<String,Port>();
        log.info("Creating new Module with no name");
    }

    public Module(String type, String name) {
        this.type = type;
        this.name = name;
        this.parameters = new ArrayList<Parameter>();
        this.ports = new HashMap<String,Port>();
        log.info("Creating new Module. name: " + name + ", type: " + type);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public Map<String,Port> getPorts() {
        return ports;
    }

    public void setPorts(Map<String,Port> ports) {
        this.ports = ports;
    }
    
    public Port getPortByName(String porName){
        return ports.get(porName);
    }

    public void addPort(Port port) {
        this.ports.put(port.getName(),port);
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

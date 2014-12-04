package nocsys.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A Verilog Module
 * 
 * @author Mohamed
 *
 */
public class Module {

    String type;
    String name;
    List<Parameter> parameters;
    List<Port> ports;

    public Module() {
        this.type = null;
        this.name = null;
        this.parameters = new ArrayList<Parameter>();
        this.ports = new ArrayList<Port>();
    }

    public Module(String type, String name) {
        this.type = type;
        this.name = name;
        this.parameters = new ArrayList<Parameter>();
        this.ports = new ArrayList<Port>();
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

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    public void addPort(Port port) {
        this.ports.add(port);
    }

    @Override
    public String toString() {
        String s = "Module: " + type + " " + name + "\n";
        for (int i = 0; i < parameters.size(); i++)
            s += parameters.get(i) + "\n";
        for (int i = 0; i < ports.size(); i++)
            s += ports.get(i) + "\n";
        return s;
    }
}

package nocsys.data;

/**
 * A name-value pair defining a parameter of a Verilog module.
 * 
 * @author Mohamed
 * 
 */
public class Parameter {
    
    String name;
    String value;

    public Parameter() {
        this.name = null;
        this.value = null;
    }
    
    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "parameter: " + name + " = " + value;
    }
}

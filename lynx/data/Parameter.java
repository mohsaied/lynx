package lynx.data;

/**
 * A name-value pair defining a parameter of a Verilog module.
 * 
 * @author Mohamed
 * 
 */
public final class Parameter {

    private String name;
    private String value;

    public Parameter() {
        this(null, null);
    }

    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Parameter(String name, int value) {
        this.name = name;
        this.value = Integer.toString(value);
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final String getValue() {
        return value;
    }

    public final void setValue(String value) {
        this.value = value;
    }

    @Override
    public Parameter clone() {
        return new Parameter(this.name, this.value);
    }

    @Override
    public String toString() {
        return "parameter: " + name + " = " + value;
    }
}

package nocsys.data;

/**
 * The input/output ports of a Verilog module
 */
public class Port {

    String type;
    String name;
    int width;

    public Port() {
        name = null;
        type = null;
        width = 0;
    }

    public Port(String name, String type, int width) {
        this.name = name;
        this.type = type;
        this.width = width;
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

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public String toString() {
        return "port: " + type + " " + name + "(" + width + ")";
    }
}

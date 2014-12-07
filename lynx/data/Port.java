package lynx.data;

/**
 * The input/output ports of a Verilog module.
 * 
 * @author Mohamed
 * 
 */
public abstract class Port {

    protected String direction;
    protected String name;
    protected int width;

    public Port() {
        name = null;
        direction = null;
        width = 0;
    }

    public Port(String name, String direction, int width) {
        this.name = name;
        this.direction = direction;
        this.width = width;
    }

    public final String getDirection() {
        return direction;
    }

    public final void setDirection(String direction) {
        this.direction = direction;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final int getWidth() {
        return width;
    }

    public final void setWidth(int width) {
        this.width = width;
    }

}

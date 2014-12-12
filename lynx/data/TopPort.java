package lynx.data;

import java.util.ArrayList;
import java.util.List;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;

/**
 * Exported port of the top-level module.
 * 
 * @author Mohamed
 *
 */
public final class TopPort extends Port {

    List<Port> wires;

    public TopPort() {
        this(null, Direction.UNKNOWN, 0, 1, null);
    }

    public TopPort(String name, Direction direction, int width, int arrayWidth, Module<? extends Port> parentModule) {
        super(name, direction, width, arrayWidth, PortType.TOP, parentModule, false);
        wires = new ArrayList<Port>();
    }

    public final List<Port> getWires() {
        return wires;
    }

    public final void addWire(Port wires) {
        this.wires.add(wires);
    }

}

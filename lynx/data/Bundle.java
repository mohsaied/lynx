package lynx.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lynx.data.MyEnums.Direction;

/**
 * 
 * A bundle is a collection of ports that will be connected to the NoC
 * 
 * @author Mohamed
 *
 */
public class Bundle {

    private Map<String, Port> dataPorts;
    private Port validPort;
    private Port readyPort;

    private Direction direction;
    private int width;

    public Bundle() {
        dataPorts = new HashMap<String, Port>();
        validPort = null;
        readyPort = null;
        width = 0;
        direction = Direction.UNKNOWN;
    }

    public final Map<String, Port> getDataPorts() {
        return dataPorts;
    }

    public final void addDataPort(Port dataPort) {
        this.dataPorts.put(dataPort.getName(), dataPort);
        addToWidth(dataPort.getWidth());
        if (direction == Direction.UNKNOWN)
            direction = dataPort.getDirection();

        assert direction == dataPort.getDirection() : "Cannot bundle data ports with different directions";
    }

    public final Port getValidPort() {
        return validPort;
    }

    public final void setValidPort(Port validPort) {
        this.validPort = validPort;
    }

    public final Port getReadyPort() {
        return readyPort;
    }

    public final void setReadyPort(Port readyPort) {
        this.readyPort = readyPort;
    }

    public final Direction getDirection() {
        return direction;
    }

    public final int getWidth() {
        return width;
    }

    private final void addToWidth(int width) {
        this.width += width;
    }

    public List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<Port>();

        allPorts.addAll(dataPorts.values());
        if (validPort != null)
            allPorts.add(validPort);
        if (readyPort != null)
            allPorts.add(readyPort);

        return allPorts;
    }

}

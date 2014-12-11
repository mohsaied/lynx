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
public final class Bundle {

    private String name;

    private Map<String, Port> dataPorts;
    private Port validPort;
    private Port readyPort;
    private Port dstPort;

    private Direction direction;
    private int width;

    private Translator translator;

    private Module parentModule;

    private List<Bundle> connections;

    public Bundle() {
        this(null);
    }

    public Bundle(String name) {
        this(null, null);
    }

    public Bundle(String name, Module parentModule) {
        this.name = name;
        dataPorts = new HashMap<String, Port>();
        validPort = null;
        readyPort = null;
        dstPort = null;
        width = 0;
        direction = Direction.UNKNOWN;
        translator = null;
        this.parentModule = parentModule;
        this.connections = new ArrayList<Bundle>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        assert (direction == Direction.OUTPUT) || (direction == Direction.INPUT && dstPort == null) : "Input bundles cannot have a dst port";
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

    public final Port getDstPort() {
        return dstPort;
    }

    public final void setDstPort(Port addrPort) {
        assert direction == Direction.OUTPUT : "Input bundles cannot have a dst port";
        this.dstPort = addrPort;
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

    public final List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<Port>();

        allPorts.addAll(dataPorts.values());
        if (validPort != null)
            allPorts.add(validPort);
        if (readyPort != null)
            allPorts.add(readyPort);
        if (dstPort != null)
            allPorts.add(readyPort);

        return allPorts;
    }

    public final Translator getTranslator() {
        return translator;
    }

    public final void setTranslator(Translator translator) {
        this.translator = translator;
    }

    public Module getParentModule() {
        return parentModule;
    }

    public void setParentModule(Module parentModule) {
        this.parentModule = parentModule;
    }

    public List<Bundle> getConnections() {
        return connections;
    }

    public void addConnection(Bundle connection) {
        this.connections.add(connection);
    }

}

package lynx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lynx.data.MyEnums.Direction;

/**
 * 
 * A bundle is a collection of ports that will be connected to the NoC
 * 
 * @author Mohamed
 *
 */
public final class Bundle {

    private static final Logger log = Logger.getLogger(Bundle.class.getName());
    
    private String name;

    private Port dataPort;
    private Port validPort;
    private Port readyPort;
    private Port dstPort; // will be null for depkt

    private Direction direction;
    private int width;

    private Translator translator;

    private DesignModule parentModule;

    private List<Bundle> connections;

    public Bundle() {
        this(null);
    }

    public Bundle(String name) {
        this(null, null);
    }

    public Bundle(String name, DesignModule parentModule) {
        this.name = name;
        dataPort = null;
        validPort = null;
        readyPort = null;
        dstPort = null;
        width = 0;
        direction = Direction.UNKNOWN;
        translator = null;
        this.parentModule = parentModule;
        this.connections = new ArrayList<Bundle>();
        log.fine("Creating new bundle, name = "+name);;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return parentModule.getName() + "." + name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public final Port getDataPort() {
        return dataPort;
    }

    public final void addDataPort(Port dataPort) {
        this.dataPort = dataPort;
        addToWidth(dataPort.getWidth());
        if (direction == Direction.UNKNOWN)
            direction = dataPort.getDirection();

        assert direction == dataPort.getDirection() : "Cannot bundle data ports with different directions";
        assert (direction == Direction.OUTPUT) || (direction == Direction.INPUT && dstPort == null) : "Input bundles cannot have a dst port";
    }

    public final Port getValidPort() {
        return validPort;
    }

    // TODO add some asserts to double check directions whenever setting any
    // port of a bundle
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
        assert direction == Direction.OUTPUT || direction == Direction.UNKNOWN : "Input bundles cannot have a dst port";
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

        if (dataPort != null)
            allPorts.add(dataPort);
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

    public DesignModule getParentModule() {
        return parentModule;
    }

    public void setParentModule(DesignModule parentModule) {
        this.parentModule = parentModule;
    }

    public List<Bundle> getConnections() {
        return connections;
    }

    public void addConnection(Bundle connection) {
        this.connections.add(connection);
    }

    public void connectToRouter(int router) {
        // this bundle should already be connected to a translator (upon
        // translator creation)
        // so this method should connect the other end of the translator to the
        // NoC router specified
        
        translator.connectToRouter(router);

    }

}

package lynx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.MyEnums.BundleType;
import lynx.data.MyEnums.Direction;
import lynx.elaboration.ConnectionGroup;

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
    private Port dstPort; // will be null for input or singular conn
    private Port vcPort; // will be null for input or singular conn

    private Direction direction;
    private int width;

    private Translator translator;

    private DesignModule parentModule;

    private List<Bundle> connections;

    private ConnectionGroup connectionGroup;

    // this part is mainly for masters and slaves
    // as we parse bundles, we can set their type (master, or slave)
    // and if it qualifies for one of those, then it must have exactly one
    // sister-bundle which is either the sending/receiving
    private BundleType bundleType;
    private Bundle sisterBundle;

    public Bundle() {
        this(null);
    }

    public Bundle(String name) {
        this(null, null);
    }

    public Bundle(String name, DesignModule parentModule) {
        this(name, BundleType.OTHER, parentModule);
    }

    public Bundle(String name, BundleType bundleType, DesignModule parentModule) {
        this.name = name;
        this.dataPort = null;
        this.validPort = null;
        this.readyPort = null;
        this.dstPort = null;
        this.vcPort = null;
        this.width = 0;
        this.direction = Direction.UNKNOWN;
        this.translator = null;
        this.parentModule = parentModule;
        this.connections = new ArrayList<Bundle>();
        this.connectionGroup = null;
        this.bundleType = bundleType;
        this.sisterBundle = null;
        log.fine("Creating new bundle, name = " + name);
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return parentModule.getName() + "." + name;
    }

    public String getFullNameDash() {
        return parentModule.getName() + "_" + name;
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

    public final Port getVcPort() {
        return vcPort;
    }

    public final void setVcPort(Port vcPort) {
        assert direction == Direction.OUTPUT || direction == Direction.UNKNOWN : "Input bundles cannot have a vc dst port";
        this.vcPort = vcPort;
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

    public ConnectionGroup getConnectionGroup() {
        return connectionGroup;
    }

    public void setConnectionGroup(ConnectionGroup connectionGroup) {
        this.connectionGroup = connectionGroup;
    }

    public BundleType getBundleType() {
        return bundleType;
    }

    public void setBundleType(BundleType bundleType) {
        this.bundleType = bundleType;
    }

    public Bundle getSisterBundle() {
        return sisterBundle;
    }

    public void setSisterBundle(Bundle sisterBundle) {
        this.sisterBundle = sisterBundle;
    }

    @Override
    public String toString() {
        String s = "";
        s += this.getFullName();
        s += " - " + this.getBundleType();
        return s;
    }

    public Bundle clone(DesignModule mod, Set<String> scc) {
        Bundle bun = new Bundle(parentModule.getName() + "^" + name, mod);

        // add the ports in this bundle to the new one
        Port dPor = dataPort.clone();
        bun.dataPort = dPor;
        mod.addPort(dPor);

        Port vPor = validPort.clone();
        bun.validPort = vPor;
        mod.addPort(vPor);

        Port rPor = readyPort.clone();
        bun.readyPort = rPor;
        mod.addPort(rPor);

        if (dstPort != null) {
            Port dsPor = dstPort.clone();
            bun.dstPort = dsPor;
            mod.addPort(dsPor);
        }

        // copy the variables
        bun.width = width;
        bun.direction = direction;
        bun.translator = null;

        // initialize the connections, but the other bundles may not be made yet
        // so I cannot start adding them now
        bun.connections = new ArrayList<Bundle>();

        bun.bundleType = bundleType;
        return bun;
    }

}

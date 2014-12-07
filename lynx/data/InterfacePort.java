package lynx.data;

/**
 * Interface ports are pulled out to the top-level
 * 
 * @author Mohamed
 *
 */
public class InterfacePort extends Port {

    private ModulePort physicalPort;

    public InterfacePort() {
        super();
        physicalPort = null;
    }

    public InterfacePort(String name, String direction, ModulePort physicalPort) {
        super(name, direction, physicalPort.width);
        this.physicalPort = physicalPort;
    }

    public final ModulePort getPhysicalPort() {
        return physicalPort;
    }

    public final void setPhysicalPort(ModulePort physicalPort) {
        this.physicalPort = physicalPort;
    }

    public String toString() {
        String s = "Interface port: " + direction + " " + name + "(" + width + ") // connects to "
                + physicalPort.getName() + " in " + physicalPort.getParentModule().getName();
        return s;
    }

}

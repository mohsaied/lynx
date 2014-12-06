package noclynx.data;

public class InterfacePort extends Port {

    Port physicalPort;

    public InterfacePort() {
        super();
        physicalPort = null;
    }

    public InterfacePort(String name, String direction, Port physicalPort) {
        super(name, direction, physicalPort.width);
        this.physicalPort = physicalPort;
    }

    public String toString() {
        String s = "Interface port: " + direction + " " + name + "(" + width + ") // connects to "
                + physicalPort.getName() + " in " + physicalPort.getParentModule().getName();
        return s;
    }

}

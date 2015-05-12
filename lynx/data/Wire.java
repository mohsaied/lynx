package lynx.data;

/**
 * A wire is a connection between two ports - it consists of a port, and the
 * port range in the source/dest of the connection. A wire will exist in both
 * the src and dst points of a "wire" connection. Therefore, the "dstPort"
 * object stored here is whatever the other end of the wire is connected to, and
 * is not necessarily the sink of the wire
 * 
 * @author Mohamed
 *
 */
public class Wire {

    Port dstPort;
    int srcPortStart;
    int srcPortEnd;
    int dstPortStart;
    int dstPortEnd;

    public Wire(Port dstPort, int srcPortStart, int srcPortEnd, int dstPortStart, int dstPortEnd) {
        this.dstPort = dstPort;
        this.srcPortStart = srcPortStart;
        this.srcPortEnd = srcPortEnd;
        this.dstPortStart = dstPortStart;
        this.dstPortEnd = dstPortEnd;
    }

    public Wire(Port currPort, Port connectingPort) {
        this.dstPort = connectingPort;
        this.srcPortStart = 0;
        this.srcPortEnd = currPort.getWidth() - 1;
        this.dstPortStart = 0;
        this.dstPortEnd = connectingPort.getWidth() - 1;
    }

    public final Port getDstPort() {
        return dstPort;
    }

    public final int getSrcPortStart() {
        return srcPortStart;
    }

    public final int getSrcPortEnd() {
        return srcPortEnd;
    }

    public final int getDstPortStart() {
        return dstPortStart;
    }

    public final int getDstPortEnd() {
        return dstPortEnd;
    }
}

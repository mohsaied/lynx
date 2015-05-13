package lynx.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lynx.data.MyEnums.ConnectionType;

/**
 * A ConnectionGroup is a logical grouping of connections that ahsare a certain
 * connection pattern (defined in the connectionType) -- can either be
 * arbitration, broadcast of just p2p
 * 
 * @author Mohamed
 *
 */
public class ConnectionGroup {

    private Set<Bundle> fromBundles;
    private Set<Bundle> toBundles;

    private List<Connection> connections;

    private ConnectionType connectionType;

    public ConnectionGroup() {

        this.fromBundles = new HashSet<Bundle>();
        this.toBundles = new HashSet<Bundle>();

        this.connections = new ArrayList<Connection>();

        this.connectionType = ConnectionType.UNKNOWN;
    }

    public void addConnection(Connection con) {
        fromBundles.add(con.getFromBundle());
        toBundles.add(con.getToBundle());
        connections.add(con);
    }

    public Set<Bundle> getFromBundles() {
        return fromBundles;
    }

    public Set<Bundle> getToBundles() {
        return toBundles;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

}

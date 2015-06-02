package lynx.elaboration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.MyEnums.ConnectionType;

/**
 * A ConnectionGroup is a logical grouping of connections that share a certain
 * connection pattern (defined in the connectionType) -- can either be
 * arbitration, broadcast of just p2p
 * 
 * @author Mohamed
 *
 */
public class ConnectionGroup {

    private Set<Bundle> fromBundles;
    private Set<Bundle> toBundles;

    private Set<Bundle> masters;
    private Set<Bundle> slaves;

    private List<Connection> connections;

    private ConnectionType connectionType;

    public ConnectionGroup() {
        this(ConnectionType.UNKNOWN);
    }

    public ConnectionGroup(ConnectionType type) {
        this.fromBundles = new HashSet<Bundle>();
        this.toBundles = new HashSet<Bundle>();
        this.masters = new HashSet<Bundle>();
        this.slaves = new HashSet<Bundle>();
        this.connections = new ArrayList<Connection>();
        this.connectionType = type;
    }

    /**
     * add a non-arbitraction connection
     * 
     * @param con
     */
    public void addConnection(Connection con) {
        assert connectionType != ConnectionType.ARBITRATION : "Must specify master or slave connection type for an arbitration connection";
        if (fromBundles.add(con.getFromBundle()))
            con.getFromBundle().setConnectionGroup(this);

        if (toBundles.add(con.getToBundle()))
            con.getToBundle().setConnectionGroup(this);

        connections.add(con);
    }

    public void addMasterConnection(Connection con) {
        assert connectionType == ConnectionType.ARBITRATION : "Cannot specify master or slave connection type for a non-arbitration connection";
        if (fromBundles.add(con.getFromBundle()))
            con.getFromBundle().setConnectionGroup(this);

        if (toBundles.add(con.getToBundle()))
            con.getToBundle().setConnectionGroup(this);

        masters.add(con.getFromBundle());
        slaves.add(con.getToBundle());

        connections.add(con);
    }

    public void addSlaveConnection(Connection con) {
        assert connectionType == ConnectionType.ARBITRATION : "Cannot specify master or slave connection type for a non-arbitration connection";
        if (fromBundles.add(con.getFromBundle()))
            con.getFromBundle().setConnectionGroup(this);

        if (toBundles.add(con.getToBundle()))
            con.getToBundle().setConnectionGroup(this);

        slaves.add(con.getFromBundle());
        masters.add(con.getToBundle());

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

    /**
     * for arbitration connectiongroups, each bundle here has a master or slave
     * property based on which set it gets filtered into
     * 
     * @param bun
     * @return
     */
    public boolean isMaster(Bundle bun) {
        return masters.contains(bun);
    }

    /**
     * for arbitration connectiongroups, each bundle here has a master or slave
     * property based on which set it gets filtered into
     * 
     * @param bun
     * @return
     */
    public boolean isSlave(Bundle bun) {
        return slaves.contains(bun);
    }

}

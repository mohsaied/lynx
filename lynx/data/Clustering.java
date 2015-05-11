package lynx.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Similar to a Mapping object, this Clustering object keeps all info of the
 * design that comes out of the clustering algorithm(s)
 * 
 * @author Mohamed
 *
 */
public class Clustering {

    /**
     * A map of newly formed clusters to the old list of designModules
     */
    Map<DesignModule, List<DesignModule>> clusterModuleMap;

    /**
     * A map of the connections inside each cluster (they should all be p2p
     * links at this stage)
     */
    Map<DesignModule, List<Connection>> clusterConnectionMap;

    /**
     * Construct the clustering from the list of SCCs and the original design
     * 
     * @param design
     * @param sccs
     */
    public Clustering() {
        // init lists
        clusterModuleMap = new HashMap<DesignModule, List<DesignModule>>();
        clusterConnectionMap = new HashMap<DesignModule, List<Connection>>();
    }

    public void addCluster(DesignModule cluster, List<DesignModule> mods, Design originalDesign) {
        clusterModuleMap.put(cluster, mods);

        // parse out all the connections within this cluster of modules
        List<Connection> consInCluster = new ArrayList<Connection>();
        List<Connection> allConnections = originalDesign.getConnections();
        // for each connection, do its endpoints start and end in this cluster?
        for (Connection con : allConnections) {
            if (mods.contains(con.getFromModule()) && mods.contains(con.getToModule())) {
                consInCluster.add(con);
            }
        }
    }
}

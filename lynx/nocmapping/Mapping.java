package lynx.nocmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lynx.data.Connection;
import lynx.data.Design;

/**
 * A mapping of a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class Mapping {

    private BoolMatrix mapMatrix;
    Design design;

    // path for each connection
    // connection --> path
    Map<Connection, List<Integer>> connectionPaths;

    // who is using this NoC link (between 2 routers)
    // link --> list of connections
    Map<String, List<Connection>> linkUtilization;

    public Mapping(boolean[][] mapMatrixValues, Design design) {
        mapMatrix = new BoolMatrix(mapMatrixValues);
        this.design = design;
        findConnectionPaths();
        findLinkUtilization();
    }

    private void findConnectionPaths() {
        connectionPaths = new HashMap<Connection, List<Integer>>();
        List<Connection> allConnections = design.getConnections();
        for (Connection con : allConnections) {
            int fromRouter = getModuleRouterIndex(con.getFromModuleIndex());
            int toRouter = getModuleRouterIndex(con.getToModuleIndex());
            List<Integer> path = design.getNoc().getPath(fromRouter, toRouter);
            connectionPaths.put(con, path);
        }
    }

    private void findLinkUtilization() {

        linkUtilization = new HashMap<String, List<Connection>>();
        double[][] nocLinks = design.getNoc().getAdjacencyMatrix();

        // initialize empty utilizations
        for (int i = 0; i < design.getNoc().getNumRouters(); i++) {
            for (int j = 0; j < design.getNoc().getNumRouters(); j++) {
                if (nocLinks[i][j] == 1.0) {
                    List<Connection> emptyCons = new ArrayList<Connection>();
                    linkUtilization.put(linkString(i, j), emptyCons);
                }
            }
        }

        // loop over connections and populate utilization map
        for (Connection con : connectionPaths.keySet()) {
            List<Integer> path = connectionPaths.get(con);

            for (int i = 0; i < path.size() - 1; i++) {
                int fromRouter = path.get(i);
                int toRouter = path.get(i + 1);
                linkUtilization.get(linkString(fromRouter, toRouter)).add(con);
            }
        }
    }

    public static String linkString(int i, int j) {
        return i + "_" + j;
    }

    public static int getSrcRouterFromLinkString(String linkString) {
        return Integer.parseInt(linkString.split("_")[0]);
    }

    public static int getDstRouterFromLinkString(String linkString) {
        return Integer.parseInt(linkString.split("_")[1]);
    }

    /**
     * Compute the cost of the current mapping based on:
     * 
     * 1) Total latency above spec: Sum of the path latency that is above
     * requested latency of all connections in number of hops
     * 
     * TODO: change to cycles instead of #hops
     * 
     * 2) Path overlap: Whenever a hop has more than one connection mapped to
     * it, increment the cost by however many connections overlap
     * 
     * TODO: make the path overlap BW dependent = smarter
     * 
     * @return cost of this mapping
     */
    public int computeCost() {
        int cost = 1;

        // latency portion of the cost
        for (Connection con : design.getConnections()) {
            int numberOfHops = this.getConnectionPath(con).size() - 1;
            cost += (numberOfHops - con.getLatencySpec());
        }

        // path overlap portion of cost
        for (int i = 0; i < design.getNoc().getNumRouters(); i++) {
            for (int j = 0; j < design.getNoc().getNumRouters(); j++) {
                if (this.getLinkUtilization(linkString(i, j)) != null) {
                    int currUtil = this.getLinkUtilization(linkString(i, j)).size();
                    cost += currUtil == 0 ? 0 : currUtil - 1;
                }
            }
        }

        return cost;
    }

    public boolean equals(Mapping mapping2) {

        // two things to satisfy sim-equivalence
        // (1) number of hops between any two modules are the same
        // (2) traffic intersections on path between two modules are the same

        // System.out.println();

        // (1) verify number of hops
        for (Connection con : design.getConnections()) {

            if (this.getConnectionPath(con).size() != mapping2.getConnectionPath(con).size())
                return false;

        }

        // (2) verify identical traffic intersections
        for (Connection con : design.getConnections()) {

            // for each connection get the path
            // we already know it has the same number of hops

            List<Integer> path = this.getConnectionPath(con);
            List<Integer> path2 = mapping2.getConnectionPath(con);

            assert path.size() == path2.size() : "Paths should have the same number of hops if they got this far";

            for (int i = 0; i < path.size() - 1; i++) {

                String linkString = linkString(path.get(i), path.get(i + 1));
                String linkString2 = linkString(path2.get(i), path2.get(i + 1));

                List<Connection> link1 = this.getLinkUtilization(linkString);
                List<Connection> link2 = mapping2.getLinkUtilization(linkString2);

                // otherwise compare traffic on each link and return false if
                // it's non-identical
                if (!identicalTrafficOnTwoLinks(link1, link2)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean identicalTrafficOnTwoLinks(List<Connection> link1, List<Connection> link2) {

        if (link1.size() != link2.size())
            return false;

        for (Connection con : link1) {
            if (!link2.contains(con))
                return false;
        }

        return true;
    }

    public boolean compareCost(Mapping mapping2) {
        return this.computeCost() <= mapping2.computeCost();
    }

    public int getModuleRouterIndex(int modIndex) {

        boolean[] matrixRow = mapMatrix.getRow(modIndex);

        for (int i = 0; i < matrixRow.length; i++)
            if (matrixRow[i]) {
                return i;
            }

        assert false : "Cannot find any 1 in this matrix row";

        return 0;
    }

    public final List<Integer> getConnectionPath(Connection con) {
        return connectionPaths.get(con);
    }

    public final List<Connection> getLinkUtilization(String linkString) {
        return linkUtilization.get(linkString);
    }

    public BoolMatrix getMapMatrix() {
        return mapMatrix;
    }

    @Override
    public String toString() {
        return mapMatrix.toString();
    }

}

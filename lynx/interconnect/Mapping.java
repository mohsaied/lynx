package lynx.interconnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.Noc;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * A mapping of a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class Mapping {

    private RealMatrix mapMatrix;
    private Design design;

    // path for each connection
    // connection --> path
    Map<Connection, List<Integer>> connectionPaths;

    // who is using this NoC link (between 2 routers)
    // link --> list of connections
    Map<String, List<Connection>> linkUtilization;

    public Mapping(double[][] mapMatrixValues, Design design) {
        mapMatrix = MatrixUtils.createRealMatrix(mapMatrixValues);
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

    public boolean equals(Mapping mapping2) {

        // two things to satisfy sim-equivalence
        // (1) number of hops between any two modules are the same
        // (2) traffic intersections on path between two modules are the same

        int numModules = this.getMapMatrix().getRowDimension();

        // System.out.println();

        for (int i = 0; i < numModules; i++) {
            for (int j = 0; j < numModules; j++) {

                // elaborate path between module i and j
                // is there a connection from i and j?
                if (design.getAdjacencyMatrix()[i][j] == 1) {

                    // (1) get number of hops

                    // first find router indices for each mapping
                    int start1 = this.getModuleRouterIndex(i);
                    int start2 = mapping2.getModuleRouterIndex(i);
                    int end1 = this.getModuleRouterIndex(j);
                    int end2 = mapping2.getModuleRouterIndex(j);

                    // System.out.print("m1(" + start1 + "," + end1 + ")" +
                    // "m2(" + start2 + "," + end2 + ")");

                    if (Noc.getNumberOfHops(start1, end1, design.getNoc()) != Noc.getNumberOfHops(start2, end2, design.getNoc())) {
                        // System.out.println(" no");
                        return false;
                    } else {
                        // System.out.println(" yes");
                    }
                }
            }
        }

        return true;
    }

    public int getModuleRouterIndex(int modIndex) {

        double[] matrixRow = mapMatrix.getRow(modIndex);

        // prettyPrint("getonepos", matrix);

        for (int i = 0; i < matrixRow.length; i++)
            if (matrixRow[i] == 1.0) {
                // System.out.println("index"+i);
                return i;
            }

        assert false : "Cannot find any 1 in this matrix row";

        return 0;
    }

    public final Map<Connection, List<Integer>> getConnectionPaths() {
        return connectionPaths;
    }

    public final Map<String, List<Connection>> getLinkUtilization() {
        return linkUtilization;
    }

    public RealMatrix getMapMatrix() {
        return mapMatrix;
    }
}

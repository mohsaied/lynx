package lynx.nocmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.MyEnums.Direction;
import lynx.main.DesignData;
import lynx.main.ReportData;

/**
 * A mapping of a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class Mapping {

    private BoolMatrix mapMatrix;
    Design design;
    Noc noc;

    // path for each connection
    // connection --> path
    Map<Connection, List<Integer>> connectionPaths;

    // who is using this NoC link (between 2 routers)
    // link --> list of connections
    Map<String, List<Connection>> linkUtilization;

    // the mapping from bundle to nocbundle
    Map<Bundle, List<NocBundle>> bundleMap;

    public Mapping(boolean[][] mapMatrixValues, Design design) {
        mapMatrix = new BoolMatrix(mapMatrixValues);
        this.design = design;
        this.noc = DesignData.getInstance().getNoc();
        try {
            connectBundles();
        } catch (Exception e) {
            e.printStackTrace();
            ReportData.getInstance().writeToRpt("SCHMETTERLING");
            ReportData.getInstance().writeToRpt(e.getMessage());
            ReportData.getInstance().closeRpt();
        }
        findConnectionPaths();
        findLinkUtilization();
    }

    public Mapping(Map<Bundle, List<NocBundle>> bundleMap, Design design) {
        this.bundleMap = bundleMap;
        this.design = design;
        this.noc = DesignData.getInstance().getNoc();
        // create mapMatrix
        findConnectionPaths();
        findLinkUtilization();
    }

    /**
     * Loop over all modules and bundles and connect them to NocBundles
     * 
     * @throws Exception
     */
    private void connectBundles() throws Exception {

        bundleMap = new HashMap<Bundle, List<NocBundle>>();
        noc.clearNocBundleStatus();

        // loop over all modules
        for (DesignModule mod : design.getDesignModules().values()) {
            int modIndex = design.getModuleIndex(mod.getName());
            int router = getModuleRouterIndex(modIndex);
            ArrayList<NocBundle> nocInBundles = noc.getNocInBundles(router);
            ArrayList<NocBundle> nocOutBundles = noc.getNocOutBundles(router);
            // loop over all bundles in this module
            for (Bundle bun : mod.getBundles().values()) {

                int bunWidth = bun.getWidth();
                assert bunWidth <= noc.getInterfaceWidth() : "Cannot (currently) handle bundles that are larger than NoC interface width of "
                        + noc.getInterfaceWidth();
                if (bunWidth > noc.getInterfaceWidth())
                    throw new Exception();

                // how many noc bundles do I need?
                int numNocBundlesRequired = bunWidth / noc.getWidth() + 1;

                Direction bunDir = bun.getDirection();

                assert bunDir != Direction.UNKNOWN : "Bundle has no direction, what should I do?!";
                if (bunDir == Direction.UNKNOWN)
                    throw new Exception();

                // mark the nocbundles as used if they are available
                // mark the bundle as connected to NoC
                // add to bundlemap

                // list of nocbundles for this bundle
                List<NocBundle> nocBundles = new ArrayList<NocBundle>();
                for (NocBundle nocbun : bunDir == Direction.INPUT ? nocOutBundles : nocInBundles) {
                    if (!nocbun.isUsed()) {
                        nocbun.setUsed(true);
                        nocBundles.add(nocbun);
                        numNocBundlesRequired--;
                        if (numNocBundlesRequired == 0)
                            break;
                    }
                }

                // attribute this bundle to the nocbundles found for it in
                // the bundlemap
                if (numNocBundlesRequired == 0) {
                    bundleMap.put(bun, nocBundles);
                } else {
                    assert false : "Too many bundles in module " + mod.getName() + " currently unsupported";
                    throw new Exception();
                }

            }
        }
    }

    private void findConnectionPaths() {
        connectionPaths = new HashMap<Connection, List<Integer>>();
        List<Connection> allConnections = design.getConnections();
        for (Connection con : allConnections) {

            // don't use module granularity here -- use bundle granularity to
            // account for bundles that are spread over 2 modules
            // int fromRouter = getModuleRouterIndex(con.getFromModuleIndex());
            // int toRouter = getModuleRouterIndex(con.getToModuleIndex());

            List<Integer> path = new ArrayList<Integer>();

            // TODO only find path for connections mapped onto the NoC by
            // checking BundleStatus
            // if (con.getFromBundle().getBundleStatus() == BundleStatus.NOC) {
            int fromRouter = bundleMap.get(con.getFromBundle()).get(0).getRouter();
            int toRouter = bundleMap.get(con.getToBundle()).get(0).getRouter();

            path = noc.getPath(fromRouter, toRouter);
            // }
            // otherwise an empty list is returned

            connectionPaths.put(con, path);
        }
    }

    private void findLinkUtilization() {

        linkUtilization = new HashMap<String, List<Connection>>();
        double[][] nocLinks = noc.getAdjacencyMatrix();

        // initialize empty utilizations
        for (int i = 0; i < noc.getNumRouters(); i++) {
            for (int j = 0; j < noc.getNumRouters(); j++) {
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

        // off-noc portion of cost: add 5 penalty for each bundle off-noc
        // TODO this is arbitrary right now
        for (List<NocBundle> list : bundleMap.values()) {
            if (list.size() == 0)
                cost += 5;
        }

        // add penalty for any bundles that are split over more than one router
        // TODO this is arbitrary right now
        for (List<NocBundle> nocbunList : bundleMap.values()) {
            Set<Integer> routers = new HashSet<Integer>();
            for (NocBundle nocbun : nocbunList) {
                int currRouter = nocbun.getRouter();
                routers.add(currRouter);
            }
            if (routers.size() > 1)
                cost += 5 * (routers.size() - 1);
        }

        // latency portion of the cost
        for (Connection con : design.getConnections()) {
            int numberOfHops = this.getConnectionPath(con).size() - 1;
            cost += (numberOfHops - con.getLatencySpec());
        }

        // path overlap portion of cost
        for (int i = 0; i < noc.getNumRouters(); i++) {
            for (int j = 0; j < noc.getNumRouters(); j++) {
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

    public int getNumNoCBundlesIn() {
        int num = 0;
        // loop over bundle map and find how many input nocbundles are used
        for (List<NocBundle> nocbunList : bundleMap.values()) {
            if (nocbunList.get(0).getDirection() == Direction.INPUT)
                num += nocbunList.size();
        }
        return num;
    }

    public int getNumNoCBundlesOut() {
        int num = 0;
        // loop over bundle map and find how many input nocbundles are used
        for (List<NocBundle> nocbunList : bundleMap.values()) {
            if (nocbunList.get(0).getDirection() == Direction.OUTPUT)
                num += nocbunList.size();
        }
        return num;
    }

    @Override
    public String toString() {
        return mapMatrix.toString();
    }

}

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

    // anneal struct holding the bundlemap etc
    AnnealBundleStruct annealStruct;

    // path for each connection
    // connection --> path
    Map<Connection, List<Integer>> connectionPaths;

    // who is using this NoC link (between 2 routers)
    // link --> list of connections
    Map<String, List<Connection>> linkUtilization;

    public Mapping(boolean[][] mapMatrixValues, Design design, Noc noc) {
        mapMatrix = new BoolMatrix(mapMatrixValues);
        this.design = design;
        this.noc = noc;
        annealStruct = new AnnealBundleStruct();
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

    public Mapping(AnnealBundleStruct annealStruct, Design design, Noc noc) {
        this.annealStruct = annealStruct;
        this.design = design;
        this.noc = noc;
        // create mapMatrix
        boolean[][] mapMatrixValues = new boolean[design.getNumDesignModules()][noc.getNumRouters()];
        for (int i = 0; i < design.getNumDesignModules(); i++) {
            for (int j = 0; j < noc.getNumRouters(); j++) {
                mapMatrixValues[i][j] = false;
                // TODO find which router each module is mapped to
            }
        }
        this.mapMatrix = new BoolMatrix(mapMatrixValues);
        findConnectionPaths();
        findLinkUtilization();
    }

    /**
     * Loop over all modules and bundles and connect them to NocBundles
     * 
     * @throws Exception
     */
    private void connectBundles() throws Exception {

        annealStruct = new AnnealBundleStruct(design, noc);

        // loop over all modules
        for (DesignModule mod : design.getDesignModules().values()) {

            for (Bundle bun : mod.getBundles().values()) {

                int selectedRouter = getModuleRouterIndex(design.getModuleIndex(mod.getName()));

                annealStruct.disconnectBundle(bun);

                int requiredNocBundles = annealStruct.attemptMapping(bun, selectedRouter, noc);

                assert requiredNocBundles == 0 : "Too many bundles in module " + mod.getName() + " currently unsupported";
                if (requiredNocBundles != 0)
                    throw new Exception();
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
            if (annealStruct.bundleMap.get(con.getFromBundle()).size() != 0
                    && annealStruct.bundleMap.get(con.getToBundle()).size() != 0) {
                int fromRouter = annealStruct.bundleMap.get(con.getFromBundle()).get(0).getRouter();
                int toRouter = annealStruct.bundleMap.get(con.getToBundle()).get(0).getRouter();

                path = noc.getPath(fromRouter, toRouter);
            }
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
        int cost = 100;

        // off-noc portion of cost: add 5 penalty for each bundle off-noc
        // TODO this is arbitrary right now
        for (List<NocBundle> list : annealStruct.bundleMap.values()) {
            if (list.size() == 0)
                cost += 100;
        }

        // add penalty for any bundles that are split over more than one router
        // TODO this is arbitrary right now
        for (DesignModule mod : design.getDesignModules().values()) {
            Set<Integer> routers = new HashSet<Integer>();
            for (int i = 0; i <= noc.getNumRouters(); i++) {
                Set<Bundle> currRouterBundles = annealStruct.bundlesAtRouter.get(i);
                for (Bundle bun : mod.getBundles().values()) {
                    if (currRouterBundles.contains(bun))
                        routers.add(i);
                }
            }
            if (routers.size() > 1)
                cost += 20 * (routers.size() - 1);
        }

        // area of off-noc connections
        // have an area penalty per connection
        // TODO make this smarter by looking at connectiongroups -- what
        // requires arbitration and what is a simple connection?
        for (Connection con : design.getConnections()) {
            if (annealStruct.bundleMap.get(con.getFromBundle()).size() == 0
                    || annealStruct.bundleMap.get(con.getFromBundle()).size() == 0)
                cost += 4;
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
                    cost += currUtil == 0 ? 0 : (currUtil - 1);
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

    public Map<Bundle, List<NocBundle>> getBundleMap() {
        return annealStruct.bundleMap;
    }

    public int getNumNoCBundlesIn() {
        int num = 0;
        // loop over bundle map and find how many input nocbundles are used
        for (List<NocBundle> nocbunList : annealStruct.bundleMap.values()) {
            if (nocbunList.size() != 0 && nocbunList.get(0).getDirection() == Direction.INPUT)
                num += nocbunList.size();
        }
        return num;
    }

    public int getNumNoCBundlesOut() {
        int num = 0;
        // loop over bundle map and find how many input nocbundles are used
        for (List<NocBundle> nocbunList : annealStruct.bundleMap.values()) {
            if (nocbunList.size() != 0 && nocbunList.get(0).getDirection() == Direction.OUTPUT)
                num += nocbunList.size();
        }
        return num;
    }

    public ArrayList<HashSet<Bundle>> getBundlesAtRouters() {
        return annealStruct.bundlesAtRouter;
    }

    @Override
    public String toString() {
        return mapMatrix.toString();
    }

    public int getApproxRouterForModule(DesignModule mod) {
        // pick a random bundle from the module, and find it's router
        int router = this.noc.getNumRouters();
        for (Bundle bun : mod.getBundles().values()) {
            if (this.annealStruct.bundleMap.get(bun).size() == 0)
                break;
            router = this.annealStruct.bundleMap.get(bun).get(0).getRouter();
            if (router != this.noc.getNumRouters())
                break;
        }
        return router;
    }

    public int getRouter(Bundle bun) {
        // if we aren't mapped to any nocbundles
        if (annealStruct.bundleMap.get(bun).size() == 0)
            return noc.getNumRouters();
        return annealStruct.bundleMap.get(bun).get(0).getRouter();
    }

    public AnnealBundleStruct getAnnealBundleStruct() {
        return annealStruct;
    }
}

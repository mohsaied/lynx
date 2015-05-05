package lynx.nocmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.MyEnums.Direction;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.main.ReportData;

public class SimulatedAnnealingBundle {

    static class AnnealStruct {
        public Map<Bundle, List<NocBundle>> bundleMap;
        public Map<NocBundle, Boolean> usedNocBundle;
        public ArrayList<HashSet<Bundle>> bundlesAtRouter;

        // pass created maps to this struct
        public AnnealStruct(Map<Bundle, List<NocBundle>> bundleMap, Map<NocBundle, Boolean> usedNocBundle,
                ArrayList<HashSet<Bundle>> bundlesAtRouter) {
            this.bundleMap = bundleMap;
            this.usedNocBundle = usedNocBundle;
            this.bundlesAtRouter = bundlesAtRouter;
        }

        // copy constructor initializes the struct with copies of maps contents
        public AnnealStruct(AnnealStruct original) {
            // make a copy of the bundleMap
            bundleMap = new HashMap<Bundle, List<NocBundle>>();
            for (Bundle bun : original.bundleMap.keySet()) {
                bundleMap.put(bun, original.bundleMap.get(bun));
            }

            // make a copy of the used nocBundle map
            usedNocBundle = new HashMap<NocBundle, Boolean>();
            for (NocBundle nocbun : original.usedNocBundle.keySet()) {
                usedNocBundle.put(nocbun, original.usedNocBundle.get(nocbun));
            }

            // make a copy of the bundles at each router
            bundlesAtRouter = new ArrayList<HashSet<Bundle>>();
            for (HashSet<Bundle> bunList : original.bundlesAtRouter) {
                HashSet<Bundle> newBunSet = new HashSet<Bundle>();
                for (Bundle bun : bunList) {
                    newBunSet.add(bun);
                }
                bundlesAtRouter.add(newBunSet);
            }
        }

        public AnnealStruct() {
            bundleMap = new HashMap<Bundle, List<NocBundle>>();
            usedNocBundle = new HashMap<NocBundle, Boolean>();
            bundlesAtRouter = new ArrayList<HashSet<Bundle>>();
        }
    }

    private static final Logger log = Logger.getLogger(SimulatedAnnealingBundle.class.getName());

    private final static int SEED = 1;

    public static void findMappings(Design design, Noc noc) {

        log.info("Figuring out the best location of modules on the NoC using simulated annealing...");

        // time
        long startTime = System.nanoTime();

        // random seed
        Random rand = new Random(SEED);

        // mapping from bundle -> nocbundles
        Map<Bundle, List<NocBundle>> bundleMap = new HashMap<Bundle, List<NocBundle>>();

        // map of used nocbundles
        Map<NocBundle, Boolean> usedNocBundle = new HashMap<NocBundle, Boolean>();

        List<Bundle> bundleList = design.getAllBundles();

        // initial solution: just put everything off-noc: empty mapping
        for (Bundle bun : bundleList) {
            bundleMap.put(bun, new ArrayList<NocBundle>());
        }

        // and all nocbundles are unused
        for (int i = 0; i < noc.getNumRouters(); i++) {
            for (NocBundle nocbun : noc.getNocInBundles(i)) {
                usedNocBundle.put(nocbun, false);
            }
            for (NocBundle nocbun : noc.getNocOutBundles(i)) {
                usedNocBundle.put(nocbun, false);
            }
        }

        // all routers have zero bundles on them except the last one
        ArrayList<HashSet<Bundle>> bundlesAtRouter = new ArrayList<HashSet<Bundle>>();
        for (int i = 0; i < noc.getNumRouters(); i++) {
            HashSet<Bundle> bunSet = new HashSet<Bundle>();
            bundlesAtRouter.add(bunSet);
        }
        // put all bundles in the last router (off-noc)
        HashSet<Bundle> bunSet = new HashSet<Bundle>();
        for (Bundle bun : bundleList) {
            bunSet.add(bun);
        }
        bundlesAtRouter.add(bunSet);

        AnnealStruct annealStruct = new AnnealStruct(bundleMap, usedNocBundle, bundlesAtRouter);

        Mapping currMapping = new Mapping(annealStruct, design);

        double cost = currMapping.computeCost();

        // time
        long endTime = System.nanoTime();
        double elapsedSeconds = (endTime - startTime) / 1e9;

        // stats
        int totalMoves = 0;
        int takenMoves = 0;

        // annealing params
        double initialTemp = 100;
        double temp = initialTemp;
        double tempFac = 0.99;
        int tempInterval = 100;

        int stable_for = 0;

        List<Double> debugAnnealCost = new ArrayList<Double>();
        List<Double> debugAnnealTemp = new ArrayList<Double>();
        debugAnnealCost.add(cost);

        // start anneal
        while (stable_for < 5000 && elapsedSeconds < 100) {

            // decrement temperature
            if (totalMoves % tempInterval == 0) {
                temp = temp * tempFac;
                tempInterval = setTempInterval(temp);
            }

            // make a move
            AnnealStruct newAnnealStruct = null;
            try {
                newAnnealStruct = annealMove(annealStruct, bundleList, noc, rand);
            } catch (Exception e) {
                e.printStackTrace();
                ReportData.getInstance().writeToRpt("SCHMETTERLING");
                ReportData.getInstance().writeToRpt(e.getMessage());
                ReportData.getInstance().closeRpt();
            }

            // measure its cost
            currMapping = new Mapping(newAnnealStruct, design);
            double newCost = currMapping.computeCost();
            double oldCost = cost;
            boolean acceptMove = (((newCost - cost) / cost) < temp / initialTemp);

            log.finest(currMapping.toString());

            if (newCost < cost || acceptMove) {
                annealStruct = newAnnealStruct;
                cost = newCost;
                takenMoves++;
                log.fine("Cost = " + cost + ", temp = " + temp);
            }

            debugAnnealCost.add(cost);
            debugAnnealTemp.add(temp);

            // how long have we been at this cost?
            stable_for = cost == oldCost ? stable_for + 1 : 0;

            // time
            endTime = System.nanoTime();
            elapsedSeconds = (endTime - startTime) / 1e9;

            // stats
            totalMoves++;
        }

        log.info("Total number of moves = " + takenMoves + "/" + totalMoves);

        log.info("Final mapping cost = " + cost);
        ReportData.getInstance().writeToRpt("map_cost = " + cost);

        // debug print
        for (DesignModule mod : design.getDesignModules().values()) {
            String s = mod.getName() + ": ";
            for (Bundle bun : mod.getBundles().values()) {

                s += bun.getName() + " --> ";
                List<NocBundle> nocbunList = annealStruct.bundleMap.get(bun);

                if (nocbunList.size() == 0) {
                    s += "off-noc";
                } else {
                    for (NocBundle nocbun : nocbunList) {
                        s += nocbun.getRouter() + ",";
                    }
                }
                s += " / ";

            }
            log.info(s);
        }

        // export solution to the design
        currMapping = new Mapping(annealStruct, design);
        design.setSingleMapping(currMapping);
        design.setDebugAnnealCost(debugAnnealCost);
        design.setDebugAnnealTemp(debugAnnealTemp);

    }

    /**
     * 1- pick a bundle at random 2- map it to a random nocBundle (or off NoC)
     * -- each location with equal (?) probability 3- legalize, so that if the
     * new location is already used, the user is moved or put off-noc
     * 
     * @param annealStruct
     * @param bundleList
     * @param noc
     * @param rand
     * @return
     * @throws Exception
     */
    private static AnnealStruct annealMove(AnnealStruct annealStruct, List<Bundle> bundleList, Noc noc, Random rand)
            throws Exception {

        AnnealStruct newAnnealStruct = new AnnealStruct(annealStruct);

        // select a bundle at random
        int numBundles = newAnnealStruct.bundleMap.size();
        int selectedBundleIndex = rand.nextInt(numBundles);
        Bundle selectedBundle = bundleList.get(selectedBundleIndex);

        // select a router at random (+1 for off-noc)
        int numRouters = noc.getNumRouters() + 1;
        int selectedRouter = rand.nextInt(numRouters);

        // if this isn't really a move, choose another router
        while (newAnnealStruct.bundlesAtRouter.get(selectedRouter).contains(selectedBundle)) {
            selectedRouter = rand.nextInt(numRouters);
        }

        // try to map that bundle onto that router
        // if it is full then we'll have to move whatever is mapped to that
        // router to another router (or if full, then off-noc)

        // remove whatever mapping this bundle currently has
        // first get the used nocbundles
        List<NocBundle> nocbunList = newAnnealStruct.bundleMap.get(selectedBundle);
        // set these nocBundles as unused
        for (NocBundle nocbun : nocbunList) {
            newAnnealStruct.usedNocBundle.put(nocbun, false);
        }
        // insert empty list to indicate unconnectedness
        List<NocBundle> emptyNocBundleList = new ArrayList<NocBundle>();
        newAnnealStruct.bundleMap.put(selectedBundle, emptyNocBundleList);
        // remove bundle from the router or off-noc mapping
        if (nocbunList.size() > 0) {
            int oldRouterIndex = nocbunList.get(0).getRouter();
            newAnnealStruct.bundlesAtRouter.get(oldRouterIndex).remove(selectedBundle);
        } else {
            newAnnealStruct.bundlesAtRouter.get(noc.getNumRouters()).remove(selectedBundle);
        }

        // that means we're mapping to a target on-noc
        if (selectedRouter != noc.getNumRouters()) {

            // map to selected router if possible without removing other bundles
            // get the nocbundles at the selected router
            // how many target nocbuns do we need?
            int bunWidth = selectedBundle.getWidth();
            assert bunWidth <= noc.getInterfaceWidth() : "Cannot (currently) handle bundles that are larger than NoC interface width of "
                    + noc.getInterfaceWidth();
            if (bunWidth > noc.getInterfaceWidth())
                throw new Exception();

            // add selected Bundle to the current list of bundles at this router
            newAnnealStruct.bundlesAtRouter.get(selectedRouter).add(selectedBundle);

            // how many noc bundles do I need?
            int numNocBundlesRequired = bunWidth / noc.getWidth() + 1;

            // get the target nocbuns at the selected router
            Direction selectedBundleDirection = selectedBundle.getDirection();
            List<NocBundle> targetNocbuns = selectedBundleDirection == Direction.INPUT ? noc.getNocOutBundles(selectedRouter)
                    : noc.getNocInBundles(selectedRouter);

            // do we have enough nocbuns available in there?
            List<NocBundle> nocBundles = new ArrayList<NocBundle>();
            for (NocBundle nocbun : targetNocbuns) {
                if (!newAnnealStruct.usedNocBundle.get(nocbun)) {
                    nocBundles.add(nocbun);
                    numNocBundlesRequired--;
                    if (numNocBundlesRequired == 0)
                        break;
                }
            }

            // have we found a valid mapping?
            if (numNocBundlesRequired == 0) {
                newAnnealStruct.bundleMap.put(selectedBundle, nocBundles);
                for (NocBundle nocbun : nocBundles) {
                    newAnnealStruct.usedNocBundle.put(nocbun, true);
                }
                newAnnealStruct.bundlesAtRouter.get(selectedRouter).add(selectedBundle);
            }
            // if we haven't found a valid mapping, then we have to remove some
            // mappings (number = numNocBundlesRequired) from the selected
            // router
            else {
                List<Bundle> markedForRemoval = new ArrayList<Bundle>();
                // choose a bundle to rip out from this router
                for (Bundle bun : newAnnealStruct.bundlesAtRouter.get(selectedRouter)) {

                    // don't rip out a bundle of the opposite direction
                    if (bun.getDirection() != selectedBundleDirection)
                        continue;

                    // mark this bundle for removal from bundlesAtRouter
                    markedForRemoval.add(bun);

                    // mark its nocbundles as unused
                    for (NocBundle nocbun : newAnnealStruct.bundleMap.get(bun)) {
                        newAnnealStruct.usedNocBundle.put(nocbun, false);
                        numNocBundlesRequired--;
                    }
                    List<NocBundle> emptyNocBundleList1 = new ArrayList<NocBundle>();
                    newAnnealStruct.bundleMap.put(bun, emptyNocBundleList1);
                    // if we have removed enough -> exit
                    if (numNocBundlesRequired <= 0)
                        break;
                }

                for (Bundle bun : markedForRemoval) {
                    // remove this bundle from bundlesAtRouter
                    newAnnealStruct.bundlesAtRouter.get(selectedRouter).remove(bun);
                }

                // now that we have space on that router, map our bundle to it

                numNocBundlesRequired = bunWidth / noc.getWidth() + 1;
                targetNocbuns = selectedBundleDirection == Direction.INPUT ? noc.getNocOutBundles(selectedRouter) : noc
                        .getNocInBundles(selectedRouter);
                // do we have enough nocbuns available in there?
                nocBundles = new ArrayList<NocBundle>();
                for (NocBundle nocbun : targetNocbuns) {
                    if (!newAnnealStruct.usedNocBundle.get(nocbun)) {
                        nocBundles.add(nocbun);
                        numNocBundlesRequired--;
                        if (numNocBundlesRequired == 0)
                            break;
                    }
                }

                // have we found a valid mapping?
                if (numNocBundlesRequired == 0) {
                    newAnnealStruct.bundleMap.put(selectedBundle, nocBundles);
                    for (NocBundle nocbun : nocBundles) {
                        newAnnealStruct.usedNocBundle.put(nocbun, true);
                    }
                    newAnnealStruct.bundlesAtRouter.get(selectedRouter).add(selectedBundle);
                }

                assert numNocBundlesRequired == 0 : "Something's wrong! Could not assign " + numNocBundlesRequired
                        + " nocbundles, at router " + selectedRouter + " for bun: " + selectedBundle.getFullName();
                if (numNocBundlesRequired != 0)
                    throw new Exception();
            }
        }

        return newAnnealStruct;
    }

    private static int setTempInterval(double temp) {
        if (temp < 100 && temp >= 90)
            return 10;
        if (temp < 90 && temp >= 80)
            return 10;
        if (temp < 80 && temp >= 70)
            return 10;
        if (temp < 70 && temp >= 60)
            return 10;
        if (temp < 60 && temp >= 50)
            return 10;
        if (temp < 50 && temp >= 40)
            return 10;
        if (temp < 40 && temp >= 30)
            return 100;
        if (temp < 30 && temp >= 20)
            return 200;
        if (temp < 20 && temp >= 10)
            return 350;
        if (temp < 10 && temp >= 0)
            return 500;
        else
            return 0;
    }

}

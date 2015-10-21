package lynx.nocmapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.main.ReportData;

public class SimulatedAnnealingBundle {

    private static final Logger log = Logger.getLogger(SimulatedAnnealingBundle.class.getName());

    private final static int SEED = 1;
    private final static int ANNEAL_TIME = 1000000000;
    private final static int QUENCH_TIME = 500000000;
    private final static int STABLE_MOVES = 1000;
    private final static int STABLE_MOVES_QUENCH = 1000;
    private final static double INITIAL_TEMP = 100;
    private final static double TEMP_FAC = 0.99;
    private final static int INITIAL_TEMP_INTERVAL = 10;

    public static void findMappings(Design design, Noc noc) {

        log.info("Figuring out the best location of modules on the NoC using simulated annealing...");

        // time
        long startTime = System.nanoTime();

        // random seed
        Random rand = new Random(SEED);

        // initial solution -- all off noc
        AnnealBundleStruct annealStruct = new AnnealBundleStruct(design, noc);
        Mapping currMapping = new Mapping(annealStruct, design, noc);
        List<Bundle> bundleList = design.getAllBundles();

        double cost = currMapping.computeCost();

        // time
        long endTime = System.nanoTime();
        double elapsedSeconds = (endTime - startTime) / 1e9;

        // stats
        int totalMoves = 0;
        int takenMoves = 0;

        // annealing params
        double temp = INITIAL_TEMP;
        int tempInterval = INITIAL_TEMP_INTERVAL;
        int stable_for = 0;
        double mapOnlyCost = cost;

        List<Double> debugAnnealCost = new ArrayList<Double>();
        List<Double> debugAnnealTemp = new ArrayList<Double>();
        debugAnnealCost.add(cost);

        // early exit if we have no bundles
        if (bundleList.size() != 0) {

            // start anneal
            while (stable_for < STABLE_MOVES && elapsedSeconds < ANNEAL_TIME) {

                // decrement temperature
                if (totalMoves % tempInterval == 0) {
                    temp = temp * TEMP_FAC;
                    tempInterval = setTempInterval(temp);
                }

                // make a move
                AnnealBundleStruct newAnnealStruct = null;
                try {
                    newAnnealStruct = annealMove(annealStruct, bundleList, noc, rand);
                } catch (Exception e) {
                    e.printStackTrace();
                    ReportData.getInstance().writeToRpt("SCHMETTERLING");
                    ReportData.getInstance().writeToRpt(e.getMessage());
                    ReportData.getInstance().closeRpt();
                }

                // measure its cost
                currMapping = new Mapping(newAnnealStruct, design, noc);
                double newCost = currMapping.computeCost();
                double oldCost = cost;
                boolean acceptMove = ((newCost - cost) / cost) < (temp / INITIAL_TEMP);
                log.finest("temp = " + Math.ceil(temp * 100) / 100 + " - costs: " + newCost + " - " + cost + " - accept = "
                        + acceptMove);

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
            log.info("Mapping cost = " + cost);
            if (elapsedSeconds >= ANNEAL_TIME)
                log.severe("Mapping anneal ended because of timeout " + elapsedSeconds + " seconds!");
            mapOnlyCost = cost;

            takenMoves = 0;
            totalMoves = 0;

            // reset stable_for
            stable_for = 0;

            // find list of modules that have all their bundles on one router
            List<DesignModule> moduleList = findModulesWithGroupedBundles(design, noc, annealStruct);
            log.info("Found " + moduleList.size() + " thirsty modules");

            double elapsedSecondsQuench = 0;
            startTime = System.nanoTime();
            endTime = System.nanoTime();

            if (moduleList.size() > 0) {

                log.info("Starting quench with " + moduleList.size() + " modules");

                // start module quench
                while (stable_for < STABLE_MOVES_QUENCH && elapsedSecondsQuench < QUENCH_TIME) {

                    // make a move
                    AnnealBundleStruct newAnnealStruct = null;
                    try {
                        newAnnealStruct = annealModuleMove(annealStruct, moduleList, noc, rand);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ReportData.getInstance().writeToRpt("SCHMETTERLING");
                        ReportData.getInstance().writeToRpt(e.getMessage());
                        ReportData.getInstance().closeRpt();
                    }

                    // measure its cost
                    currMapping = new Mapping(newAnnealStruct, design, noc);
                    double newCost = currMapping.computeCost();
                    double oldCost = cost;

                    log.finest(currMapping.toString());

                    if (newCost <= cost) {
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
                    elapsedSecondsQuench = (endTime - startTime) / 1e9;

                    // stats
                    totalMoves++;
                }
            }

            log.info("Quench number of moves = " + takenMoves + "/" + totalMoves);

            log.info("Final mapping cost = " + cost);
            log.info("Quench reduction = " + (mapOnlyCost - cost));

            if (elapsedSecondsQuench >= QUENCH_TIME)
                log.warning("Mapping quench ended because of timeout " + elapsedSecondsQuench + " seconds!");

            ReportData.getInstance().writeToRpt("map_cost = " + cost);
            ReportData.getInstance().writeToRpt("quench = " + (mapOnlyCost - cost));

        }

        // print out problematic paths that need more bandwidth than they have
        int numOverUtilLinks = findOverUtilizedPaths(noc, currMapping);

        ReportData.getInstance().writeToRpt("overutil_links = " + numOverUtilLinks);

        debugPrintMapping(design, annealStruct);

        // export solution to the design
        currMapping = new Mapping(annealStruct, design, noc);
        design.setSingleMapping(currMapping);
        design.setDebugAnnealCost(debugAnnealCost);
        design.setDebugAnnealTemp(debugAnnealTemp);
    }

    private static int findOverUtilizedPaths(Noc noc, Mapping currMapping) {
        int numOverUtilLinks = 0;
        for (int i = 0; i < noc.getNumRouters(); i++) {
            for (int j = 0; j < noc.getNumRouters(); j++) {
                int totalWidth = 0;
                if (currMapping.getLinkUtilizationConnections(Mapping.linkString(i, j)) != null)
                    for (Connection con : currMapping.getLinkUtilizationConnections(Mapping.linkString(i, j))) {
                        totalWidth += Math.ceil(((double) con.getFromBundle().getWidth() / noc.getWidth())) * noc.getWidth();
                    }
                if (totalWidth > noc.getInterfaceWidth()) {
                    numOverUtilLinks++;
                    log.warning("Connection between router " + i + " and " + j + " may be overutilized "
                            + (int) ((double) totalWidth / noc.getInterfaceWidth() * 100) + "%");

                    for (Connection con : currMapping.getLinkUtilizationConnections(Mapping.linkString(i, j))) {
                        log.info("\t"
                                + (int) ((double) (Math.ceil(((double) con.getFromBundle().getWidth() / noc.getWidth())) * noc
                                        .getWidth()) / noc.getInterfaceWidth() * 100) + "% |Connection: "
                                + con.getFromBundle().getFullName() + " to " + con.getToBundle().getFullName());
                    }
                }
            }
        }
        return numOverUtilLinks;
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
    private static AnnealBundleStruct annealMove(AnnealBundleStruct annealStruct, List<Bundle> bundleList, Noc noc, Random rand)
            throws Exception {

        AnnealBundleStruct newAnnealStruct = new AnnealBundleStruct(annealStruct);

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

        // remove whatever mapping this bundle currently has and put it off-noc
        newAnnealStruct.disconnectBundle(selectedBundle);

        // attempt to connect selectedBundle to selectedRouter -- remove
        // existing bundles where necessary
        newAnnealStruct.connectBundle(selectedBundle, selectedRouter, noc);

        return newAnnealStruct;
    }

    private static AnnealBundleStruct annealModuleMove(AnnealBundleStruct annealStruct, List<DesignModule> moduleList, Noc noc,
            Random rand) throws Exception {

        AnnealBundleStruct newAnnealStruct = new AnnealBundleStruct(annealStruct);

        // select a bundle at random
        int numModules = moduleList.size();
        int selectedModuleIndex = rand.nextInt(numModules);
        DesignModule selectedModule = moduleList.get(selectedModuleIndex);

        // which router are we currently at?
        int oldRouter = annealStruct.getRouterForGroupedModule(selectedModule, noc);

        // select a router at random (+1 for off-noc)
        int numRouters = noc.getNumRouters() + 1;
        int selectedRouter = rand.nextInt(numRouters);

        // if this isn't really a move, choose another router
        while (selectedRouter == oldRouter) {
            selectedRouter = rand.nextInt(numRouters);
        }

        // try to map that bundle onto that router
        // if it is full then we'll have to move whatever is mapped to that
        // router to another router (or if full, then off-noc)

        // remove whatever mapping this bundle currently has and put it off-noc
        for (Bundle bun : selectedModule.getBundles().values())
            newAnnealStruct.disconnectBundle(bun);

        // attempt to connect selectedBundle to selectedRouter -- remove
        // existing bundles where necessary
        for (Bundle bun : selectedModule.getBundles().values())
            newAnnealStruct.connectBundle(bun, selectedRouter, noc);

        return newAnnealStruct;
    }

    /**
     * returns a set of modules that have all bundles mapped on the same router
     * 
     * @param design
     * @param noc
     * @param annealStruct
     * @return
     */
    private static List<DesignModule> findModulesWithGroupedBundles(Design design, Noc noc, AnnealBundleStruct annealStruct) {
        List<DesignModule> modList = new ArrayList<DesignModule>();

        for (DesignModule mod : design.getDesignModules().values()) {
            int router = -1;
            boolean sameRouter = true;
            for (Bundle bun : mod.getBundles().values()) {
                Set<Integer> routers = annealStruct.getRoutersForBundle(bun);
                if (routers.size() == 0) {
                    if (router == -1 || router == noc.getNumRouters()) {
                        router = noc.getNumRouters();
                    } else {
                        sameRouter = false;
                        break;
                    }
                } else if (routers.size() == 1) {
                    if (router == -1 || router == (int) routers.toArray()[0]) {
                        router = (int) routers.toArray()[0];
                    } else {
                        sameRouter = false;
                        break;
                    }
                } else {
                    sameRouter = false;
                    break;
                }
            }
            if (sameRouter)
                modList.add(mod);
        }

        return modList;
    }

    /**
     * nonlinear cooling schedule
     * 
     * @param temp
     * @return
     */
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
            return 10;
        if (temp < 30 && temp >= 20)
            return 10;
        if (temp < 20 && temp >= 10)
            return 10;
        if (temp < 10 && temp >= 5)
            return 10;
        if (temp < 5 && temp >= 3)
            return 100;
        if (temp < 3 && temp >= 2)
            return 200;
        if (temp < 2 && temp >= 1)
            return 1000;
        if (temp < 1 && temp >= 0)
            return 1000;
        else
            return 0;
    }

    /**
     * printout the mapping to the logger
     * 
     * @param design
     * @param annealStruct
     */
    private static void debugPrintMapping(Design design, AnnealBundleStruct annealStruct) {

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
    }

}

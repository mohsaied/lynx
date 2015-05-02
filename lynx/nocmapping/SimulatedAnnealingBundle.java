package lynx.nocmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.MyEnums.BundleStatus;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.main.ReportData;

public class SimulatedAnnealingBundle {

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

        List<Bundle> bundleList = design.getAllBundles();

        // initial solution: just put everything off-noc
        // want to assign each bundle to zero-or-more nocBundles
        for (Bundle bun : bundleList) {
            bun.setBundleStatus(BundleStatus.OTHER);
            bundleMap.put(bun, new ArrayList<NocBundle>());
        }

        Mapping currMapping = new Mapping(bundleMap, design);

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
        while (cost > 1 && stable_for < 5000 && elapsedSeconds < 100) {

            // decrement temperature
            if (totalMoves % tempInterval == 0) {
                temp = temp * tempFac;
                tempInterval = setTempInterval(temp);
            }

            // make a move
            Map<Bundle, List<NocBundle>> newBundleMap = annealMove(bundleMap, bundleList, noc, rand);

            // measure its cost
            currMapping = new Mapping(newBundleMap, design);
            double newCost = currMapping.computeCost();
            double oldCost = cost;
            boolean acceptMove = (((newCost - cost) / cost) < temp / initialTemp);

            log.finest(currMapping.toString());

            if (newCost < cost || acceptMove) {
                bundleMap = newBundleMap;
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

        // export solution to the design
        currMapping = new Mapping(bundleMap, design);
        design.setSingleMapping(currMapping);
        design.setDebugAnnealCost(debugAnnealCost);
        design.setDebugAnnealTemp(debugAnnealTemp);

    }

    private static int setTempInterval(double temp) {
        if (temp < 100 && temp >= 90)
            return 100;
        if (temp < 90 && temp >= 80)
            return 100;
        if (temp < 80 && temp >= 70)
            return 100;
        if (temp < 70 && temp >= 60)
            return 100;
        if (temp < 60 && temp >= 50)
            return 100;
        if (temp < 50 && temp >= 40)
            return 100;
        if (temp < 40 && temp >= 30)
            return 100;
        if (temp < 30 && temp >= 20)
            return 200;
        if (temp < 20 && temp >= 10)
            return 200;
        if (temp < 10 && temp >= 0)
            return 200;
        else
            return 0;
    }

    private static Map<Bundle, List<NocBundle>> annealMove(Map<Bundle, List<NocBundle>> bundleMap, List<Bundle> bundleList,
            Noc noc, Random rand) {

        Map<Bundle, List<NocBundle>> newBundleMap = new HashMap<Bundle, List<NocBundle>>();
        // 1- pick a bundle at random
        // 2- map it to a random nocBundle (or off NoC) -- each location with
        // equal (?) probability
        // 3- legalize, so that if the new location is already used, the user is
        // moved or put off-noc

        // select a bundle at random
        int numBundles = newBundleMap.size();
        int selectedBundleIndex = rand.nextInt(numBundles);
        Bundle selectedBundle = bundleList.get(selectedBundleIndex);

        // select a router at random (+1 for off-noc)
        int numRouters = noc.getNumRouters() + 1;
        int selectedRouter = rand.nextInt(numRouters);

        // try to map that bundle onto that router
        // if it is full then we'll have to move whatever is mapped to that
        // router to another router (or if full, then off-noc)

        // remove whatever mapping this noc currently has

        // that means we're off-noc
        if (selectedRouter == numRouters) {
            // set it to off-noc
            selectedBundle.setBundleStatus(BundleStatus.OTHER);
        }
        // we're mapping to a target on-noc
        else {
            // set to on-Noc
            selectedBundle.setBundleStatus(BundleStatus.NOC);

            // map to selected router if possible without removing other bundles

            // if not possible, then remove the minimum number of bundles first
            // and attempt to map them to another (random) router

            // or off-noc if not possible

            // now that we have space on that router, map our bundle to it

        }

        return newBundleMap;
    }
}

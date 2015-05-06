package lynx.nocmapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
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

        // initial solution -- all off noc
        AnnealBundleStruct annealStruct = new AnnealBundleStruct(design, noc);
        Mapping currMapping = new Mapping(annealStruct, design);
        List<Bundle> bundleList = design.getAllBundles();

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
        int tempInterval = 10;
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

        debugPrintMapping(design, annealStruct);

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
            return 100;
        if (temp < 30 && temp >= 20)
            return 200;
        if (temp < 20 && temp >= 10)
            return 350;
        if (temp < 10 && temp >= 5)
            return 500;
        if (temp < 5 && temp >= 0)
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

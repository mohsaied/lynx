package lynx.interconnect.mapping;

import java.util.Random;
import java.util.logging.Logger;

import lynx.data.Design;

public class SimulatedAnnealing {

    private static final Logger log = Logger.getLogger(SimulatedAnnealing.class.getName());

    private final static int SEED = 1;

    public static void findMappings(Design design) {

        // time
        long startTime = System.nanoTime();

        // random seed
        Random rand = new Random(SEED);

        int nocNumRouters = design.getNoc().getNumRouters();
        int numModules = design.getNumModules();

        assert numModules <= nocNumRouters : "Number of modules in design cannot (currently) exceed the number of routers in NoC";

        boolean[][] currPermMatrix = new boolean[numModules][nocNumRouters];

        for (int i = 0; i < numModules; i++) {
            for (int j = 0; j < nocNumRouters; j++) {
                currPermMatrix[i][j] = false;
            }
        }

        // initial solution is module i mapped to router i
        for (int i = 0; i < numModules; i++) {
            currPermMatrix[i][i] = true;
        }

        Mapping currMapping = new Mapping(currPermMatrix, design);
        int cost = currMapping.computeCost();

        // time
        long endTime = System.nanoTime();
        double elapsedSeconds = (endTime - startTime) / 1e9;

        // stats
        int totalMoves = 0;
        int takenMoves = 0;

        // start anneal
        while (elapsedSeconds < 2) {

            // make a move
            boolean[][] newPermMatrix = annelMove(currPermMatrix, rand);

            // measure its cost
            Mapping newMapping = new Mapping(newPermMatrix, design);
            int newCost = newMapping.computeCost();

            if (newCost < cost) {
                currPermMatrix = newPermMatrix;
                cost = newCost;
                takenMoves++;
                log.info("Cost = " + cost);
            }

            // time
            endTime = System.nanoTime();
            elapsedSeconds = (endTime - startTime) / 1e9;

            // stats
            totalMoves++;
        }

        log.info("Total number of moves = " + takenMoves + "/" + totalMoves);
    }

    private static boolean[][] annelMove(boolean[][] currPermMatrix, Random rand) {
        int numModules = currPermMatrix.length;
        int numRouters = currPermMatrix[0].length;

        // pick a module and router at random
        int srcMod = rand.nextInt(numModules);
        int dstRouter = rand.nextInt(numRouters);

        // make sure we're actually making a move
        int srcRouter = getModuleRouter(srcMod, currPermMatrix);
        if (srcRouter == dstRouter) {
            if (dstRouter == numRouters - 1)
                dstRouter--;
            else
                dstRouter++;
        }

        // check if there is already a module at this router
        int dstMod = numModules;
        for (int i = 0; i < numModules; i++) {
            if (currPermMatrix[i][dstRouter])
                dstMod = i;
        }

        boolean[][] newPermMatrix = new boolean[numModules][numRouters];

        // now create the new permuted matrix
        for (int i = 0; i < numModules; i++) {
            for (int j = 0; j < numRouters; j++) {
                if (i == srcMod) {
                    if (j == dstRouter) {
                        newPermMatrix[i][j] = true;
                    } else {
                        newPermMatrix[i][j] = false;
                    }
                } else if (i == dstMod) {
                    if (j == srcRouter) {
                        newPermMatrix[i][j] = true;
                    } else {
                        newPermMatrix[i][j] = false;
                    }
                } else {
                    newPermMatrix[i][j] = currPermMatrix[i][j];
                }
            }
        }

        return newPermMatrix;
    }

    private static int getModuleRouter(int srcMod, boolean[][] currPermMatrix) {
        for (int i = 0; i < currPermMatrix.length; i++) {
            if (currPermMatrix[srcMod][i])
                return i;
        }
        return currPermMatrix.length;
    }
}

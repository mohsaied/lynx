package lynx.nocmapping;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.Noc;

public class Ullman {

    private static final Logger log = Logger.getLogger(Ullman.class.getName());

    // counters
    private static long numSols;
    private static long numRecs;
    private static long numPrune;
    private static int bestTotalLatency;

    public static void findMappings(Design design) {

        log.info("Figuring out the best location of modules on the NoC using exact Ullman...");

        // get adjacency matrices of design and NoC
        boolean[][] designMatrixValues = design.getAdjacencyMatrix();
        int[][] nocMatrixValues = design.getNoc().getFullAdjacencyMatrix();
        BoolMatrix designMatrix = new BoolMatrix(designMatrixValues);
        BoolMatrix nocMatrix; // is subset of nocMatrixValues (assigned later)

        // array holding the used columns
        boolean[] usedColumns = new boolean[design.getNoc().getNumRouters()];
        for (int i = 0; i < usedColumns.length; i++)
            usedColumns[i] = false;

        // list of valid mappings found
        List<Mapping> validMappings = new ArrayList<Mapping>();

        for (int maxLegalHops = 1; maxLegalHops <= design.getNoc().getMaxHops(); maxLegalHops++) {

            // NoC adjacency matrix with specified #hops
            nocMatrix = createNocMatrixMaxHops(nocMatrixValues, maxLegalHops);

            // initial permMatrix
            BoolMatrix permMatrix = createPermMatrix(designMatrix, nocMatrix);
            BoolMatrix origPermMatrix = permMatrix.clone();

            // reset counters
            validMappings.clear();
            numSols = 0;
            numRecs = 0;
            numPrune = 0;
            bestTotalLatency = 9999;

            // search for valid mappings
            ullmanRecurse(usedColumns, 0, designMatrix, nocMatrix, permMatrix, origPermMatrix, validMappings, design);

            log.info("Number of solutions found = " + validMappings.size() + "(" + numSols + ")" + ", at maxHops = "
                    + maxLegalHops + ", numRecs = " + numRecs + ", numPrune = " + numPrune);

            // break if we found some solutions at this #hops
            if (numSols > 0)
                break;

        }

        // at this point all the solutions we want are stored in validMappings
        // create new list , each entry has another list of equivalent-sim
        // mappings
        List<ArrayList<Mapping>> equivSimMappings = binMappings(validMappings, designMatrix, design);

        log.info("Uniquified mappings from " + validMappings.size() + " to " + equivSimMappings.size());

        // sort the unique mappings by latency and traffic
        rankMappings(equivSimMappings);

        log.info("Ranked " + equivSimMappings.size() + " unique mappings");

        // add these mappings to the design
        design.setMappings(equivSimMappings);
    }

    private static void ullmanRecurse(boolean[] usedColumns, int currRow, BoolMatrix designMatrix, BoolMatrix nocMatrix,
            BoolMatrix permMatrix, BoolMatrix origPermMatrix, List<Mapping> validMappings, Design design) {

        numRecs++;
        // System.out.println("---------\ncurrRow = " + currRow);
        // prettyPrint("permMatrix", permMatrix);

        // check permMatrix validity all rows were permuted
        if (currRow >= (permMatrix.getNumRows())) {
            if (isValidMapping(designMatrix, nocMatrix, permMatrix)) {
                // System.out.println("Found a valid mapping ^^");
                // update best_total_latency and
                int currLatency;
                if (design != null)
                    currLatency = computeLatency(permMatrix, designMatrix, design.getNoc());
                else
                    currLatency = computeLatency(permMatrix, designMatrix, new Noc());

                if (currLatency < bestTotalLatency) {
                    log.info("best latency: " + bestTotalLatency + " -> " + currLatency);
                    bestTotalLatency = currLatency;
                }
                if (numSols % 10000 == 0)
                    log.info("numsols: " + numSols + " - numrecs: " + numRecs + " - numprune: " + numPrune + " - bestlatency: "
                            + bestTotalLatency);
                if (design != null) {// && validMappings.size() < 10000) {
                    Mapping permMatrixMapping = new Mapping(permMatrix.clone().getData(), design);
                    validMappings.add(permMatrixMapping);
                }
                numSols++;
                return;
            }
        } else {

            BoolMatrix permMatrixCopy = permMatrix.clone();

            // prettyPrint("bp: permMatrix", permMatrixCopy);

            // prune permMatrix
            ullmanPrune(permMatrixCopy, designMatrix, nocMatrix);

            // prettyPrint("ap: permMatrix", permMatrixCopy);

            // recursion
            for (int i = 0; i < usedColumns.length; i++) {
                if (!usedColumns[i]) {

                    if (!permMatrix.getEntry(currRow, i))
                        continue;

                    // for this row, set the current (row,column) to 1 and all
                    // (row,other_columns) to 0
                    permMatrixCopy.setOneColInRow(currRow, i);

                    usedColumns[i] = true;
                    ullmanRecurse(usedColumns, currRow + 1, designMatrix, nocMatrix, permMatrixCopy, origPermMatrix,
                            validMappings, design);
                    usedColumns[i] = false;
                }
            }
        }
        return;
    }

    private static int computeLatency(BoolMatrix permMatrix, BoolMatrix designMatrix, Noc noc) {

        int totLatency = 0;
        // for each connection

        // find connections from designmatrix
        for (int fromMod = 0; fromMod < designMatrix.getNumRows(); fromMod++)
            for (int toMod = 0; toMod < designMatrix.getNumCols(); toMod++)
                if (designMatrix.getEntry(fromMod, toMod)) {

                    // find the routers
                    int fromRouter = permMatrix.getOnePosFromRow(fromMod);
                    int toRouter = permMatrix.getOnePosFromRow(toMod);

                    totLatency += noc.getNumberOfHops(fromRouter, toRouter);

                }

        return totLatency;
    }

    private static void ullmanPrune(BoolMatrix permMatrixCopy, BoolMatrix designMatrix, BoolMatrix nocMatrix) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < permMatrixCopy.getNumRows(); i++)
                for (int j = 0; j < permMatrixCopy.getNumCols(); j++)
                    if (permMatrixCopy.getEntry(i, j)) {

                        // prune based on outgoing connections
                        for (int x = 0; x < designMatrix.getRow(i).length; x++)
                            if (designMatrix.getRow(i)[x]) {
                                boolean found = false;
                                for (int y = 0; y < nocMatrix.getRow(j).length; y++)
                                    if (nocMatrix.getRow(j)[y]) {
                                        if (permMatrixCopy.getEntry(x, y)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                if (!found) {
                                    permMatrixCopy.setEntry(i, j, false);
                                    changed = true;
                                    numPrune++;
                                }
                            }

                        // prune based on incoming connections
                        for (int x = 0; x < designMatrix.getColumn(i).length; x++)
                            if (designMatrix.getColumn(i)[x]) {
                                boolean found = false;
                                for (int y = 0; y < nocMatrix.getColumn(j).length; y++)
                                    if (nocMatrix.getColumn(j)[y]) {
                                        if (permMatrixCopy.getEntry(x, y)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                if (!found) {
                                    permMatrixCopy.setEntry(i, j, false);
                                    changed = true;
                                    numPrune++;
                                }
                            }
                    }
        }

    }

    private static boolean isValidMapping(BoolMatrix designMatrix, BoolMatrix nocMatrix, BoolMatrix permMatrix) {

        // first check if there is more than one 1 in any column
        // TODO (does this ever happen? shouldn't they be
        // correct-by-construction)
        for (int i = 0; i < permMatrix.getNumCols(); i++) {
            assert !permMatrix.moreThanOneOnePerColumn(i) : "Invalid matrix checked for solution!";
        }

        // validation matrix
        BoolMatrix valMatrix = permMatrix.multiply((permMatrix.multiply(nocMatrix)).transpose());

        // is this a valid permMatrix? must cover designMatrix
        for (int i = 0; i < designMatrix.getNumRows(); i++)
            for (int j = 0; j < designMatrix.getNumCols(); j++)
                if ((designMatrix.getEntry(i, j) == true) && (valMatrix.getEntry(i, j) != true)) {
                    return false;
                }

        return true;
    }

    private static List<ArrayList<Mapping>> binMappings(List<Mapping> validMappings, BoolMatrix designMatrix, Design design) {

        List<ArrayList<Mapping>> equivSimMappings = new ArrayList<ArrayList<Mapping>>();

        for (Mapping currMapping : validMappings) {

            boolean foundEquiv = false;

            // first search through equivSimMappings
            for (ArrayList<Mapping> mappingList : equivSimMappings) {

                // group with identical solns
                if (currMapping.equals(mappingList.get(0))) {
                    foundEquiv = true;
                    mappingList.add(currMapping);
                    break;
                }
            }

            // if no equivalent mapping was found, create a new entry
            if (!foundEquiv) {
                ArrayList<Mapping> newMappingList = new ArrayList<Mapping>();
                newMappingList.add(currMapping);
                equivSimMappings.add(newMappingList);
            }
        }

        return equivSimMappings;
    }

    private static void rankMappings(List<ArrayList<Mapping>> equivSimMappings) {
        // bubble sort the solutions found from best to worst
        for (int i = 0; i < equivSimMappings.size(); i++) {
            for (int j = i + 1; j < equivSimMappings.size(); j++) {

                if (equivSimMappings.get(j).get(0).compareCost(equivSimMappings.get(i).get(0))) {

                    ArrayList<Mapping> temp = equivSimMappings.get(j);
                    equivSimMappings.set(j, equivSimMappings.get(i));
                    equivSimMappings.set(i, temp);
                }
            }
        }
    }

    private static BoolMatrix createNocMatrixMaxHops(int[][] nocMatrixValues, int maxLegalHops) {
        int numRouters = nocMatrixValues.length;
        boolean[][] newNocMatrixValues = new boolean[numRouters][numRouters];
        // control legal # hops
        for (int i = 0; i < numRouters; i++) {
            for (int j = 0; j < numRouters; j++) {
                if (nocMatrixValues[i][j] > maxLegalHops || nocMatrixValues[i][j] == 0)
                    newNocMatrixValues[i][j] = false;
                else
                    newNocMatrixValues[i][j] = true;
            }
        }

        return new BoolMatrix(newNocMatrixValues);
    }

    private static BoolMatrix createPermMatrix(BoolMatrix designMatrix, BoolMatrix nocMatrix) {

        final int numModules = designMatrix.getNumRows();
        final int numRouters = nocMatrix.getNumRows();

        boolean[][] permMatrixValues = new boolean[numModules][numRouters];

        // create initial permMatrix
        // initial matrix should have 1s if deg(modMati) >= deg(nocMati)
        for (int i = 0; i < numModules; i++)
            for (int j = 0; j < numRouters; j++)
                // check both incoming and outgoing edges
                if (designMatrix.sumRow(i) <= nocMatrix.sumRow(j) && designMatrix.sumCol(i) <= nocMatrix.sumCol(j))
                    permMatrixValues[i][j] = true;
                else
                    permMatrixValues[i][j] = false;

        return new BoolMatrix(permMatrixValues);
    }

    public static void prettyPrint(String name, BoolMatrix m) {
        System.out.println(name);
        System.out.println(m);
    }

    public static void main(String[] args) {

        // get adjacency matrices of design and NoC
        boolean[][] designMatrixValues = { { false, true, false }, { false, false, true }, { false, false, false } };

        boolean[][] nocMatrixValues = { { false, true, false }, { true, false, true }, { false, true, false } };

        BoolMatrix designMatrix = new BoolMatrix(designMatrixValues);
        BoolMatrix nocMatrix = new BoolMatrix(nocMatrixValues);

        // prettyPrint("designMatrix", designMatrix);
        // prettyPrint("nocMatrix", nocMatrix);

        // create the permutation matrix which specifies which module is mapped
        // onto which NoC router
        final int n = 3;
        final int m = 3;
        boolean[][] permMatrixValues = new boolean[n][m];

        // create initial permMatrix
        // initial matrix should have 1s if deg(modMati) >= deg(nocMati)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                if (designMatrix.sumRow(i) <= nocMatrix.sumRow(j)) {
                    permMatrixValues[i][j] = true;
                } else {
                    permMatrixValues[i][j] = false;
                }
        BoolMatrix permMatrix = new BoolMatrix(permMatrixValues);
        BoolMatrix origPermMatrix = permMatrix.clone();

        // current row we are investigating
        int currRow = 0;
        // array holding the used columns
        boolean[] usedColumns = new boolean[permMatrix.getNumCols()];
        for (int i = 0; i < usedColumns.length; i++)
            usedColumns[i] = false;

        // list of valid mappings found
        List<Mapping> validMappings = new ArrayList<Mapping>();

        numSols = 0;
        numRecs = 0;
        numPrune = 0;
        System.out.println("Starting recursion with:");

        ullmanRecurse(usedColumns, currRow, designMatrix, nocMatrix, permMatrix, origPermMatrix, validMappings, null);

        System.out.println("Number of solutions found = " + numSols);
        System.out.println("Number of recursions = " + numRecs);
        System.out.println("Number of pruning = " + numPrune);
    }

}

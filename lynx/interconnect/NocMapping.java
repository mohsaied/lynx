package lynx.interconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import lynx.data.Design;

public class NocMapping {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    public static void findMappings(Design design) {

        log.setLevel(Level.ALL);

        log.info("Figuring out the best location of modules on the NoC...");

        // initialize the number of solutions to 0

        // get adjacency matrices of design and NoC
        boolean[][] designMatrixValues = design.getAdjacencyMatrix();
        int[][] nocMatrixValues = design.getNoc().getFullAdjacencyMatrix();
        BoolMatrix designMatrix = new BoolMatrix(designMatrixValues);
        BoolMatrix nocMatrix;

        // prettyPrint("nocMatrix", nocMatrix);

        // create the permutation matrix which specifies which module is mapped
        // onto which NoC router
        final int n = design.getNumModules();
        final int m = design.getNoc().getNumRouters();
        boolean[][] permMatrixValues = new boolean[n][m];

        // create initial permMatrix
        // initial matrix should have 1s if deg(modMati) >= deg(nocMati)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                if (design.getModuleInDegree(i) <= design.getNoc().getRouterDegree(j)
                        && design.getModuleOutDegree(i) <= design.getNoc().getRouterDegree(j))
                    permMatrixValues[i][j] = true;
                else
                    permMatrixValues[i][j] = false;
        BoolMatrix permMatrix = new BoolMatrix(permMatrixValues);
        BoolMatrix origPermMatrix = new BoolMatrix(permMatrixValues);

        // current row we are investigating
        int currRow = 0;
        // array holding the used columns
        boolean[] usedColumns = new boolean[permMatrix.getNumCols()];
        for (int i = 0; i < usedColumns.length; i++)
            usedColumns[i] = false;

        // list of valid mappings found
        List<Mapping> validMappings = new ArrayList<Mapping>();

        int maxLegalHops;
        for (maxLegalHops = 1; maxLegalHops <= design.getNoc().getMaxHops(); maxLegalHops++) {

            boolean[][] newNocMatrixValues = new boolean[design.getNoc().getNumRouters()][design.getNoc().getNumRouters()];
            // control legal # hops
            for (int i = 0; i < design.getNoc().getNumRouters(); i++) {
                for (int j = 0; j < design.getNoc().getNumRouters(); j++) {
                    if (nocMatrixValues[i][j] > maxLegalHops || nocMatrixValues[i][j] == 0)
                        newNocMatrixValues[i][j] = false;
                    else
                        newNocMatrixValues[i][j] = true;
                }
            }

            nocMatrix = new BoolMatrix(newNocMatrixValues);
            // prettyPrint("nocMatrix",nocMatrix);

            validMappings.clear();

            // search!
            ullmanRecurse(usedColumns, currRow, designMatrix, nocMatrix, permMatrix, origPermMatrix, validMappings, design);

            log.info("Number of solutions found = " + validMappings.size() + ", at maxHops = " + maxLegalHops + ", numrecs = "
                    + numRecs);

            if (maxLegalHops == design.getNoc().getMaxHops())
                break;
        }

        // at this point all the solutions we want are stored in validMappings
        // create new list , each entry has another list of equiv-sim mappings
        List<ArrayList<Mapping>> equivSimMappings = binMappings(validMappings, designMatrix, design);

        log.info("Uniquified mappings from " + validMappings.size() + " to " + equivSimMappings.size());

        // sort the unique mappings by latency and traffic
        rankMappings(equivSimMappings);

        log.info("Ranked " + equivSimMappings.size() + " unique mappings");

        design.setMappings(equivSimMappings);

    }

    private static void rankMappings(List<ArrayList<Mapping>> equivSimMappings) {
        // rank the solutions found from best to worst
        for (int i = 0; i < equivSimMappings.size(); i++) {
            for (int j = i + 1; j < equivSimMappings.size(); j++) {

                if (equivSimMappings.get(j).get(0).compare(equivSimMappings.get(i).get(0))) {

                    /*
                     * // find the latencies of mapping at i and j Mapping first
                     * = equivSimMappings.get(i).get(0); Connection con1 =
                     * first.design.getConnections().get(0); Connection con2 =
                     * first.design.getConnections().get(1);
                     * 
                     * int firstOne = first.getConnectionPath(con1).size() - 1;
                     * int firsttwo = first.getConnectionPath(con2).size() - 1;
                     * 
                     * Mapping second = equivSimMappings.get(j).get(0); int
                     * secondOne = second.getConnectionPath(con1).size() - 1;
                     * int secondtwo = second.getConnectionPath(con2).size() -
                     * 1;
                     * 
                     * System.out.println("swap (" + firstOne + "," + firsttwo +
                     * ") with the better (" + secondOne + "," + secondtwo +
                     * ")");
                     */

                    ArrayList<Mapping> temp = equivSimMappings.get(j);
                    equivSimMappings.set(j, equivSimMappings.get(i));
                    equivSimMappings.set(i, temp);
                }
            }
        }

        /*
         * for (int i = 0; i < equivSimMappings.size(); i++) { Mapping first =
         * equivSimMappings.get(i).get(0); Connection con1 =
         * first.design.getConnections().get(0); Connection con2 =
         * first.design.getConnections().get(1);
         * 
         * int firstOne = first.getConnectionPath(con1).size() - 1; int firsttwo
         * = first.getConnectionPath(con2).size() - 1;
         * System.out.println("sorted " + i + " (" + firstOne + "," + firsttwo +
         * ")"); }
         */
    }

    private static List<ArrayList<Mapping>> binMappings(List<Mapping> validMappings, BoolMatrix designMatrix, Design design) {

        List<ArrayList<Mapping>> equivSimMappings = new ArrayList<ArrayList<Mapping>>();

        for (Mapping currMapping : validMappings) {

            // prettyPrint("", currMapping);

            boolean foundEquiv = false;

            // first search through equivSimMappings and check if there's an
            for (ArrayList<Mapping> mappingList : equivSimMappings) {

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

    public static void main(String[] args) {

        // get adjacency matrices of design and NoC
        boolean[][] designMatrixValues = { { false, true, false }, { true, false, true }, { false, true, false } };
        boolean[][] nocMatrixValues = { { false, true, false, false }, { true, false, true, true },
                { false, true, false, false }, { false, true, false, false } };
        BoolMatrix designMatrix = new BoolMatrix(designMatrixValues);
        BoolMatrix nocMatrix = new BoolMatrix(nocMatrixValues);

        // prettyPrint("designMatrix", designMatrix);
        // prettyPrint("nocMatrix", nocMatrix);

        // create the permutation matrix which specifies which module is mapped
        // onto which NoC router
        final int n = 3;
        final int m = 4;
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
        System.out.println("Starting recursion with:");

        ullmanRecurse(usedColumns, currRow, designMatrix, nocMatrix, permMatrix, origPermMatrix, validMappings, null);

        System.out.println("Number of solutions found = " + numSols);
        System.out.println("Number of recursions = " + numRecs);
    }

    static int numSols;
    static int numRecs;

    private static void ullmanRecurse(boolean[] usedColumns, int currRow, BoolMatrix designMatrix, BoolMatrix nocMatrix,
            BoolMatrix permMatrix, BoolMatrix origPermMatrix, List<Mapping> validMappings, Design design) {

        numRecs++;
        //System.out.println("---------\ncurrRow = " + currRow);
        //prettyPrint("permMatrix", permMatrix);

        // check the permMatrix if it is a valid isomorphism if we permuted all
        // the rows
        if (currRow >= (permMatrix.getNumRows())) {
            if (isValidMapping(designMatrix, nocMatrix, permMatrix)) {
                //System.out.println("Found a valid mapping ^^");
                if (design != null) {
                    Mapping permMatrixMapping = new Mapping(permMatrix.clone().getData(), design);
                    validMappings.add(permMatrixMapping);
                }
                numSols++;
                return;
            }
        } else {

            BoolMatrix permMatrixCopy = permMatrix.clone();

            // prune permMatrix
            ullmanPrune(permMatrixCopy, designMatrix, nocMatrix);

            // recurse
            for (int i = 0; i < usedColumns.length; i++) {
                if (!usedColumns[i]) {

                    if (!origPermMatrix.getEntry(currRow, i))
                        continue;

                    // for this row, set the current (row,column) to 1 and all
                    // (row,other_columns) to 0
                    for (int j = 0; j < usedColumns.length; j++)
                        permMatrixCopy.setEntry(currRow, j, i == j ? true : false);

                    usedColumns[i] = true;
                    ullmanRecurse(usedColumns, currRow + 1, designMatrix, nocMatrix, permMatrixCopy, origPermMatrix,
                            validMappings, design);
                    usedColumns[i] = false;
                }
            }
        }
        return;
    }

    private static void ullmanPrune(BoolMatrix permMatrixCopy, BoolMatrix designMatrix, BoolMatrix nocMatrix) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < permMatrixCopy.getNumRows(); i++) {
                for (int j = 0; j < permMatrixCopy.getNumCols(); j++) {
                    if (permMatrixCopy.getEntry(i, j)) {
                        // for all neighbours of mod i in designMatrix
                        for (int x = 0; x < designMatrix.getRow(i).length; x++) {
                            // means we have a connection here
                            if (designMatrix.getRow(i)[x]) {
                                // now check nocMatrix to see if it has a
                                // valid neighbour
                                // that means that x (which is a neighbour
                                // of i)
                                // should have a 1 in the permMatrix to j

                                // check permMatrix in all neighbours to j,
                                // in row x
                                boolean found = false;
                                for (int y = 0; y < nocMatrix.getRow(j).length; y++) {
                                    if (permMatrixCopy.getEntry(x, y)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    permMatrixCopy.setEntry(i, j, false);
                                    System.out.println("prune");
                                    changed = true;
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isValidMapping(BoolMatrix designMatrix, BoolMatrix nocMatrix, BoolMatrix permMatrix) {

        // prettyPrint("permMatrix", permMatrix);

        // first check if there is more than one 1 in any column
        for (int i = 0; i < permMatrix.getNumCols(); i++) {
            if (permMatrix.moreThanOneOnePerColumn(i)) {
                return false;
            }
        }

        // validation matrix is allowed to have more 1s (edges) than the
        // subgraph we're trying to map
        BoolMatrix valMatrix = permMatrix.multiply((permMatrix.multiply(nocMatrix)).transpose());

        // prettyPrint("permMatrix", permMatrix);
        // prettyPrint("nocMatrix", nocMatrix);
        // prettyPrint("permMatrix.multiply(nocMatrix)",
        // permMatrix.multiply(nocMatrix));
        // prettyPrint("valMatrix", valMatrix);

        // is this a valid permMatrix?
        for (int i = 0; i < designMatrix.getNumRows(); i++)
            for (int j = 0; j < designMatrix.getNumCols(); j++)
                if ((designMatrix.getEntry(i, j) == true) && (valMatrix.getEntry(i, j) != true)) {
                    // System.out.println("invalid");
                    return false;
                }

        // System.out.println("valid!");
        return true;
    }

    public static void prettyPrint(String name, BoolMatrix m) {
        System.out.println(name);
        System.out.println(m);
    }

}

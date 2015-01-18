package lynx.interconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import lynx.data.Design;
import lynx.data.Noc;

public class NocMapping {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    public static void ullman(Design design) {

        log.setLevel(Level.ALL);

        // initialize the number of solutions to 0

        // get adjacency matrices of design and NoC
        double[][] designMatrixValues = design.getAdjacencyMatrix();
        double[][] nocMatrixValues = design.getNoc().getFullAdjacencyMatrix();
        RealMatrix designMatrix = MatrixUtils.createRealMatrix(designMatrixValues);
        RealMatrix nocMatrix = MatrixUtils.createRealMatrix(nocMatrixValues);

        // create the permutation matrix which specifies which module is mapped
        // onto which NoC router
        final int n = design.getNumModules();
        final int m = design.getNoc().getNumRouters();
        double[][] permMatrixValues = new double[n][m];

        // create initial permMatrix
        // initial matrix should have 1s if deg(modMati) >= deg(nocMati)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                if (design.getModuleInDegree(i) <= design.getNoc().getRouterDegree(j)
                        && design.getModuleOutDegree(i) <= design.getNoc().getRouterDegree(j))
                    permMatrixValues[i][j] = 1;
                else
                    permMatrixValues[i][j] = 0;
        RealMatrix permMatrix = MatrixUtils.createRealMatrix(permMatrixValues);

        // current row we are investigating
        int currRow = 0;
        // array holding the used columns
        boolean[] usedColumns = new boolean[permMatrix.getColumnDimension()];
        for (int i = 0; i < usedColumns.length; i++)
            usedColumns[i] = false;

        // list of valid mappings found
        List<Mapping> validMappings = new ArrayList<Mapping>();

        int maxLegalHops;
        for (maxLegalHops = 1; maxLegalHops <= design.getNoc().getMaxHops(); maxLegalHops++) {

            double[][] newNocMatrixValues = new double[design.getNoc().getNumRouters()][design.getNoc().getNumRouters()];
            // control legal # hops
            for (int i = 0; i < design.getNoc().getNumRouters(); i++) {
                for (int j = 0; j < design.getNoc().getNumRouters(); j++) {
                    if (nocMatrixValues[i][j] > maxLegalHops)
                        newNocMatrixValues[i][j] = 0;
                    else
                        newNocMatrixValues[i][j] = 1;
                }
            }

            nocMatrix = MatrixUtils.createRealMatrix(newNocMatrixValues);

            validMappings.clear();

            // search!
            ullmanRecurse(usedColumns, currRow, designMatrix, nocMatrix, permMatrix, validMappings);

            log.info("Number of solutions found = " + validMappings.size() + ", at maxHops = " + maxLegalHops);

            if (maxLegalHops == design.getNoc().getMaxHops())
                break;
        }

        // at this point all the solutions we want are stored in validMappings
        // create new list , each entry has another list of equiv-sim mappings
        List<ArrayList<Mapping>> equivSimMappings = sortMappings(validMappings, designMatrix, design);

        log.info("Uniquified mappings from " + validMappings.size() + " to " + equivSimMappings.size());

        // rank the solutions found from best to worst

    }

    private static List<ArrayList<Mapping>> sortMappings(List<Mapping> validMappings, RealMatrix designMatrix, Design design) {

        List<ArrayList<Mapping>> equivSimMappings = new ArrayList<ArrayList<Mapping>>();

        for (Mapping currMapping : validMappings) {

            // prettyPrint("", currMapping);

            boolean foundEquiv = false;

            // first search through equivSimMappings and check if there's an
            for (ArrayList<Mapping> mappingList : equivSimMappings) {

                if (isEquivMapping(currMapping, mappingList.get(0), designMatrix, design)) {
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

    private static boolean isEquivMapping(Mapping mapping1, Mapping mapping2, RealMatrix designMatrix, Design design) {

        // two things to satisfy sim-equivalence
        // (1) number of hops between any two modules are the same
        // (2) traffic intersections on path between two modules are the same

        int numModules = mapping1.getMapMatrix().getRowDimension();

        // System.out.println();

        for (int i = 0; i < numModules; i++) {
            for (int j = 0; j < numModules; j++) {

                // elaborate path between module i and j
                // is there a connection from i and j?
                if (designMatrix.getEntry(i, j) == 1) {

                    // (1) get number of hops

                    // first find router indices for each mapping
                    int start1 = getOnePosFromRow(i, mapping1.getMapMatrix());
                    int start2 = getOnePosFromRow(i, mapping2.getMapMatrix());
                    int end1 = getOnePosFromRow(j, mapping1.getMapMatrix());
                    int end2 = getOnePosFromRow(j, mapping2.getMapMatrix());

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

    private static int getOnePosFromRow(int rowIndex, RealMatrix matrix) {

        double[] matrixRow = matrix.getRow(rowIndex);

        // prettyPrint("getonepos", matrix);

        for (int i = 0; i < matrixRow.length; i++)
            if (matrixRow[i] == 1.0) {
                // System.out.println("index"+i);
                return i;
            }

        assert false : "Cannot find any 1 in this matrix row";

        return 0;
    }

    private static void ullmanRecurse(boolean[] usedColumns, int currRow, RealMatrix designMatrix, RealMatrix nocMatrix,
            RealMatrix permMatrix, List<Mapping> validMappings) {

        // prettyPrint("permMatrix", permMatrix);

        // check the permMatrix if it is a valid isomorphism if we permuted all
        // the rows
        if (currRow >= (permMatrix.getRowDimension())) {
            if (isValidMapping(designMatrix, nocMatrix, permMatrix)) {
                // System.out.println("Found a valid mapping!");
                // prettyPrint("permMatrix", permMatrix);
                Mapping permMatrixMapping = new Mapping(permMatrix.getData());
                validMappings.add(permMatrixMapping);
                return;
            }
        } else { // recurse
            for (int i = 0; i < usedColumns.length; i++) {
                if (!usedColumns[i]) {

                    // for this row, set the current (row,column) to 1 and the
                    // rest
                    // (row,other_columns) to 0
                    for (int j = 0; j < usedColumns.length; j++)
                        permMatrix.setEntry(currRow, j, i == j ? 1 : 0);

                    usedColumns[i] = true;
                    ullmanRecurse(usedColumns, currRow + 1, designMatrix, nocMatrix, permMatrix, validMappings);
                    usedColumns[i] = false;
                }
            }
        }
    }

    private static boolean isValidMapping(RealMatrix designMatrix, RealMatrix nocMatrix, RealMatrix permMatrix) {

        // prettyPrint("permMatrix", permMatrix);

        // first check if there is more than one 1 in any column, in which case
        // it's invalid
        for (int i = 0; i < permMatrix.getColumnDimension(); i++) {
            double[] currCol = permMatrix.getColumn(i);
            boolean firstOne = false;
            for (int j = 0; j < currCol.length; j++) {
                if (currCol[j] == 1.0 && !firstOne) {
                    firstOne = true;
                } else if (currCol[j] == 1.0 && firstOne) {
                    // System.out.println("Too many ones");
                    return false;
                }
            }
        }

        // validation matrix is allowed to have more 1s (edges) than the
        // subgraph we're trying to map
        RealMatrix valMatrix = permMatrix.multiply((permMatrix.multiply(nocMatrix)).transpose());

        // is this a valid permMatrix?
        for (int i = 0; i < designMatrix.getColumnDimension(); i++)
            for (int j = 0; j < designMatrix.getRowDimension(); j++)
                if ((designMatrix.getEntry(i, j) == 1) && (valMatrix.getEntry(i, j) != 1)) {
                    // System.out.println("invalid");
                    return false;
                }

        // prettyPrint("valMatrix", valMatrix);

        // System.out.println("valid!");
        return true;
    }

    public static void prettyPrint(String matName, RealMatrix m) {
        System.out.println(matName);
        System.out.print("   ");
        for (int i = 0; i < m.getColumnDimension(); i++) {
            System.out.print(i);
            if (i >= 9)
                System.out.print(" ");
            else
                System.out.print("  ");
        }
        System.out.println();
        System.out.print("   ");
        for (int i = 0; i < m.getColumnDimension(); i++) {
            System.out.print("|  ");
        }
        System.out.println();
        for (int i = 0; i < m.getRowDimension(); i++) {
            if (i <= 9)
                System.out.print(i + "--");
            else
                System.out.print(i + "-");
            for (int j = 0; j < m.getColumnDimension(); j++) {
                System.out.print((int) (m.getEntry(i, j)) + "  ");
            }
            System.out.println();
        }
    }
}

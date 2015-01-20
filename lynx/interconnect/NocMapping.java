package lynx.interconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import lynx.data.Design;

public class NocMapping {

    private static final Logger log = Logger.getLogger(NocInterconnect.class.getName());

    public static void findMappings(Design design) {

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
            ullmanRecurse(usedColumns, currRow, designMatrix, nocMatrix, permMatrix, validMappings, design);

            log.info("Number of solutions found = " + validMappings.size() + ", at maxHops = " + maxLegalHops);

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
                    ArrayList<Mapping> temp = equivSimMappings.get(j);
                    equivSimMappings.set(j, equivSimMappings.get(i));
                    equivSimMappings.set(i, temp);
                }
            }
        }
    }

    private static List<ArrayList<Mapping>> binMappings(List<Mapping> validMappings, RealMatrix designMatrix, Design design) {

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

    private static void ullmanRecurse(boolean[] usedColumns, int currRow, RealMatrix designMatrix, RealMatrix nocMatrix,
            RealMatrix permMatrix, List<Mapping> validMappings, Design design) {

        // prettyPrint("permMatrix", permMatrix);

        // check the permMatrix if it is a valid isomorphism if we permuted all
        // the rows
        if (currRow >= (permMatrix.getRowDimension())) {
            if (isValidMapping(designMatrix, nocMatrix, permMatrix)) {
                // System.out.println("Found a valid mapping!");
                // prettyPrint("permMatrix", permMatrix);
                Mapping permMatrixMapping = new Mapping(permMatrix.getData(), design);
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
                    ullmanRecurse(usedColumns, currRow + 1, designMatrix, nocMatrix, permMatrix, validMappings, design);
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

package lynx.interconnect;

import lynx.data.Design;
import lynx.data.Noc;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * A mapping of a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class Mapping {

    private RealMatrix mapMatrix;
    private Design design;

    public Mapping(double[][] mapMatrixValues, Design design) {
        mapMatrix = MatrixUtils.createRealMatrix(mapMatrixValues);
        this.design = design;
    }

    public RealMatrix getMapMatrix() {
        return mapMatrix;
    }

    public boolean equals(Mapping mapping2) {
        
        // two things to satisfy sim-equivalence
        // (1) number of hops between any two modules are the same
        // (2) traffic intersections on path between two modules are the same

        int numModules = this.getMapMatrix().getRowDimension();

        // System.out.println();

        for (int i = 0; i < numModules; i++) {
            for (int j = 0; j < numModules; j++) {

                // elaborate path between module i and j
                // is there a connection from i and j?
                if (design.getAdjacencyMatrix()[i][j] == 1) {

                    // (1) get number of hops

                    // first find router indices for each mapping
                    int start1 = getOnePosFromRow(i, this.getMapMatrix());
                    int start2 = getOnePosFromRow(i, mapping2.getMapMatrix());
                    int end1 = getOnePosFromRow(j, this.getMapMatrix());
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

    public static int getOnePosFromRow(int rowIndex, RealMatrix matrix) {

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
}

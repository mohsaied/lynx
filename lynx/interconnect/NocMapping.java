package lynx.interconnect;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import lynx.data.Design;

public class NocMapping {

    public static void Ullman(Design design) {
        // find candidate NoC placements
        double[][] designMatrixValues = design.getAdjacencyMatrix();
        double[][] nocMatrixValues = design.getNoc().getAdjacencyMatrix();

        RealMatrix designMatrix = MatrixUtils.createRealMatrix(designMatrixValues);
        RealMatrix nocMatrix = MatrixUtils.createRealMatrix(nocMatrixValues);

        System.out.println(designMatrix.toString());
        System.out.println(nocMatrix.toString());
    }

}

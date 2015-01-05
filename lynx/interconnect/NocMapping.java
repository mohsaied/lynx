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

        final int n = design.getNumModules();
        final int m = design.getNoc().getNumRouters();
        double[][] permMatrix = new double[n][m];

        // initial matrix should have 1s if deg(modMati) >= deg(nocMati)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                if (design.getModuleInDegree(i) <= design.getNoc().getRouterDegree(j)
                        && design.getModuleOutDegree(i) <= design.getNoc().getRouterDegree(j))
                    permMatrix[i][j] = 1;
                else
                    permMatrix[i][j] = 0;

        System.out.println(designMatrix.toString());
        System.out.println(nocMatrix.toString());
    }

}

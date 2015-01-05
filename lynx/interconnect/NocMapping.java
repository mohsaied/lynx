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
        double[][] permMatrixValues = new double[n][m];

        // initial matrix should have 1s if deg(modMati) >= deg(nocMati)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                if (design.getModuleInDegree(i) <= design.getNoc().getRouterDegree(j)
                        && design.getModuleOutDegree(i) <= design.getNoc().getRouterDegree(j))
                    permMatrixValues[i][j] = 1;
                else
                    permMatrixValues[i][j] = 0;

        RealMatrix permMatrix = MatrixUtils.createRealMatrix(permMatrixValues);

        System.out.println(designMatrix);
        System.out.println(nocMatrix);
        System.out.println(permMatrix);
        prettyPrint("designMatrix", designMatrix);
        prettyPrint("nocMatrix", nocMatrix);
        prettyPrint("permMatrix", permMatrix);
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

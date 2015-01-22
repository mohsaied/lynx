package lynx.interconnect;

public class BoolMatrix {

    private boolean[][] boolMatrix;
    private long[] efficientMatrixRows;
    private long[] efficientMatrixCols;

    private int numRows;
    private int numCols;

    public BoolMatrix(boolean[][] matrixValues) {

        this.boolMatrix = matrixValues;

        numRows = matrixValues.length;
        numCols = matrixValues[0].length;

        efficientMatrixRows = new long[numRows];
        efficientMatrixCols = new long[numCols];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if (boolMatrix[i][j])
                    efficientMatrixRows[i] = efficientMatrixRows[i] | (1 << j);
            }
        }

        for (int i = 0; i < numCols; i++) {
            for (int j = 0; j < numRows; j++) {
                if (boolMatrix[j][i])
                    efficientMatrixCols[i] = efficientMatrixCols[i] | (1 << j);
            }
        }
    }

    public static void main(String[] args) {
        boolean[][] testMatrix = { { true, false, true }, { false, true, false }, { false, false, true } };
        BoolMatrix m = new BoolMatrix(testMatrix);
        boolean[][] testMatrix2 = { { true, false, false }, { false, true, false }, { false, false, true } };
        BoolMatrix m2 = new BoolMatrix(testMatrix2);
        System.out.println(m);
        System.out.println(m2);
        System.out.println(m.multiply(m2).transpose());
    }

    public boolean[][] getMatrixValues() {
        return boolMatrix;
    }

    public void setEntry(int xpos, int ypos, boolean value) {
        this.boolMatrix[xpos][ypos] = value;
    }

    public boolean getEntry(int xpos, int ypos) {
        return this.boolMatrix[xpos][ypos];
    }

    public int getNumRows() {
        return numRows;
    }

    public int getNumCols() {
        return numCols;
    }

    public BoolMatrix transpose() {

        boolean[][] matrixValues = new boolean[this.getNumCols()][this.getNumRows()];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                matrixValues[j][i] = this.getEntry(i, j);
            }
        }

        return new BoolMatrix(matrixValues);
    }

    public BoolMatrix multiply(BoolMatrix m) {

        assert this.getNumRows() == m.getNumCols() && this.getNumCols() == m.getNumRows() : "Matrix dimensions ("
                + this.getNumRows() + "," + this.getNumCols() + ")" + " and (" + m.getNumRows() + "," + m.getNumCols() + ")"
                + " don't match for multiplication";

        int numRows = this.getNumRows();
        int numCols = m.getNumCols();

        boolean[][] matrixValues = new boolean[numRows][numCols];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                long row = this.efficientMatrixRows[i];
                long col = m.efficientMatrixCols[j];
                matrixValues[i][j] = rowXColumn(row, col);
            }
        }
        return new BoolMatrix(matrixValues);
    }

    private boolean rowXColumn(long row, long col) {
        // only works for matrices in which each row/column has a single one
        return ((row & col) > 0);
    }

    @Override
    public String toString() {
        String s = "   ";
        for (int i = 0; i < numCols; i++) {
            s += i;
            if (i <= 9)
                s += "  ";
            else
                s += " ";
        }
        s += "\n   ";
        for (int i = 0; i < numCols; i++) {
            s += "|  ";
        }
        s += "\n";
        for (int i = 0; i < numRows; i++) {
            if (i <= 9)
                s += (i + "--");
            else
                s += (i + "-");
            for (int j = 0; j < numCols; j++) {
                s += this.getEntry(i, j) ? 1 : 0;
                s += "  ";
            }
            s += "\n";
        }
        return s;
    }

}

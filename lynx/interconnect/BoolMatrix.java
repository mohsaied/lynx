package lynx.interconnect;


/**
 * A binary (1/0) 2D matrix class
 *
 * @author Mohamed
 * 
 */
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

    public boolean[][] getData() {
        return boolMatrix;
    }

    public void setEntry(int i, int j, boolean value) {
        this.boolMatrix[i][j] = value;
        // update efficient matrix representation
        if (value) {
            efficientMatrixRows[i] = efficientMatrixRows[i] | (1 << j);
            efficientMatrixCols[j] = efficientMatrixCols[j] | (1 << i);
        } else {
            efficientMatrixRows[i] = efficientMatrixRows[i] & ~(1 << j);
            efficientMatrixCols[j] = efficientMatrixCols[j] & ~(1 << i);
        }
    }

    public boolean getEntry(int i, int j) {
        return this.boolMatrix[i][j];
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

        assert this.getNumCols() == m.getNumRows() : "Matrix dimensions (" + this.getNumRows() + "," + this.getNumCols() + ")"
                + " and (" + m.getNumRows() + "," + m.getNumCols() + ")" + " don't match for multiplication";

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

    public boolean[] getRow(int i) {
        return boolMatrix[i];
    }

    public boolean[] getColumn(int j) {
        boolean[] col = new boolean[numRows];
        for (int i = 0; i < numRows; i++) {
            col[i] = boolMatrix[i][j];
        }
        return col;
    }

    public int sumRow(int i) {
        boolean[] row = getRow(i);
        int sum = 0;
        for (int j = 0; j < row.length; j++) {
            if (row[j])
                sum++;
        }
        return sum;
    }

    public int sumCol(int i) {
        boolean[] col = getColumn(i);
        int sum = 0;
        for (int j = 0; j < col.length; j++) {
            if (col[j])
                sum++;
        }
        return sum;
    }

    public boolean moreThanOneOnePerColumn(int i) {
        boolean[] col = getColumn(i);
        boolean firstOne = false;
        for (int j = 0; j < col.length; j++) {
            if (col[j] && !firstOne)
                firstOne = true;
            else if (col[j] && firstOne)
                return true;
        }
        return false;
    }

    @Override
    public BoolMatrix clone() {

        boolean[][] origArray = this.getData();
        // deep copy
        boolean[][] copyArray = new boolean[origArray.length][origArray[0].length];

        for (int i = 0; i < origArray.length; i++) {
            for (int j = 0; j < origArray[0].length; j++) {
                copyArray[i][j] = origArray[i][j];
            }
        }

        return new BoolMatrix(copyArray);
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

    public void setOneColInRow(int currRow, int i) {

        for (int j = 0; j < getNumCols(); j++){
            setEntry(currRow, j, i == j ? true : false);
        }

    }

}

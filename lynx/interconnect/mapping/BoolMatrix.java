package lynx.interconnect.mapping;

/**
 * A binary (1/0) 2D matrix class
 *
 * @author Mohamed
 * 
 */
public class BoolMatrix {

    private boolean[][] boolMatrix;

    private int numRows;
    private int numCols;

    public BoolMatrix(boolean[][] matrixValues) {

        this.boolMatrix = matrixValues;

        numRows = matrixValues.length;
        numCols = matrixValues[0].length;

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
                boolean[] row = this.getRow(i);
                boolean[] col = m.getColumn(j);
                matrixValues[i][j] = rowXColumnBool(row, col);
            }
        }
        return new BoolMatrix(matrixValues);
    }

    private boolean rowXColumnBool(boolean[] row, boolean[] col) {
        for (int i = 0; i < row.length; i++)
            if (row[i])
                if (col[i])
                    return true;
                else
                    return false;
        return false;
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

        for (int j = 0; j < getNumCols(); j++) {
            setEntry(currRow, j, i == j ? true : false);
        }

    }

    public int getOnePosFromRow(int row) {
        boolean[] currRow = getRow(row);
        for (int i = 0; i < currRow.length; i++) {
            if (currRow[i])
                return i;
        }
        return currRow.length;
    }
}

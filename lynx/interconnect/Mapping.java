/**
 * 
 */
package lynx.interconnect;

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

    public Mapping(double[][] mapMatrixValues) {
        mapMatrix = MatrixUtils.createRealMatrix(mapMatrixValues);
    }

    public RealMatrix getMapMatrix() {
        return mapMatrix;
    }

}

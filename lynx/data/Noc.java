package lynx.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;
import lynx.xml.XmlNoc;

/**
 * NoC class is a special module that instantiates an NoC interface (a.k.a.
 * fabricinterface)
 * 
 * @author Mohamed
 *
 */
public class Noc extends Module {

    private static final String nocName = "fabric_interface";
    private static final String nocInstName = "fi_inst";
    private static final String xmlWidth = "width";
    private static final String xmlNumrouters = "num_routers";
    private static final String xmlNumVcs = "num_vcs";
    private static final String xmlVcDepth = "vc_depth";

    private static final int defaultNocWidth = 150;
    private static final int defaultNocNumRouters = 16;
    private static final int defaultNocNumVcs = 2;
    private static final int defaultNocVcDepth = 16;

    private int nocWidth;
    private int nocNumRouters;
    private int nocNumVcs;
    private int nocVcDepth;

    private int nocInterfaceWidth;
    private int nocAddressWidth;
    private int nocVcAddressWidth;
    private int nocNumRoutersPerDimension;

    public Noc() {
        super(nocName, nocInstName);

        configureNoC(defaultNocWidth, defaultNocNumRouters, defaultNocNumVcs, defaultNocVcDepth);
        calculateDerivedParameters();
        addNocParameters();
        addNocPorts();
    }

    public Noc(String nocPath) throws ParserConfigurationException, SAXException, IOException {
        super(nocName, nocInstName);

        configureNoC(nocPath);
        calculateDerivedParameters();
        addNocParameters();
        addNocPorts();
    }

    public void configureNoC(String nocPath) throws ParserConfigurationException, SAXException, IOException {
        Map<String, Integer> varMap = XmlNoc.readXMLNoC(nocPath);

        if (varMap.containsKey(xmlWidth))
            nocWidth = varMap.get(xmlWidth);
        else
            nocWidth = defaultNocWidth;

        if (varMap.containsKey(xmlNumrouters))
            nocNumRouters = varMap.get(xmlNumrouters);
        else
            nocNumRouters = defaultNocNumRouters;

        if (varMap.containsKey(xmlNumVcs))
            nocNumVcs = varMap.get(xmlNumVcs);
        else
            nocNumVcs = defaultNocNumVcs;

        if (varMap.containsKey(xmlVcDepth))
            nocVcDepth = varMap.get(xmlVcDepth);
        else
            nocVcDepth = defaultNocVcDepth;
    }

    private void configureNoC(int nocWidth, int nocNumRouters, int nocNumVcs, int nocVcDepth) {
        this.nocWidth = nocWidth;
        this.nocNumRouters = nocNumRouters;
        this.nocNumVcs = nocNumVcs;
        this.nocVcDepth = nocVcDepth;
    }

    public Port getPort(PortType type, Direction direction, int router) {
        return getPortByName(buildNocPortName(type, direction, router));
    }

    public int getWidth() {
        return this.nocWidth;
    }

    public int getNumRouters() {
        return this.nocNumRouters;
    }

    public int getNumVcs() {
        return this.nocNumVcs;
    }

    public int getVcDepth() {
        return this.nocVcDepth;
    }

    public int getInterfaceWidth() {
        return this.nocInterfaceWidth;
    }

    public int getAddressWidth() {
        return this.nocAddressWidth;
    }

    public int getVcAddressWidth() {
        return this.nocVcAddressWidth;
    }

    public int getNumRoutersPerDimension() {
        return this.nocNumRoutersPerDimension;
    }

    private void calculateDerivedParameters() {
        // derived parameters
        nocInterfaceWidth = 4 * nocWidth;
        nocAddressWidth = clog2(nocNumRouters);
        nocVcAddressWidth = clog2(nocNumVcs);
        this.nocNumRoutersPerDimension = (int) Math.sqrt(nocNumRouters);
        assert Math.ceil(Math.sqrt(nocNumRouters)) == Math.sqrt(nocNumRouters) : "Number of routers must be a square (2, 4, 9, 16, etc..)";
    }

    public static int clog2(double num) {
        return (int) Math.ceil((Math.log(num) / Math.log(2)));
    }

    private void addNocParameters() {
        // parameters
        this.addParameter(new Parameter("WIDTH_NOC", nocWidth));
        this.addParameter(new Parameter("WIDTH_RTL", nocInterfaceWidth));
        this.addParameter(new Parameter("N", nocNumRouters));
        this.addParameter(new Parameter("NUM_VC", nocNumVcs));
        this.addParameter(new Parameter("DEPTH_PER_VC", nocVcDepth));
        this.addParameter(new Parameter("VERBOSE", "1"));
        this.addParameter(new Parameter("VC_ADDRESS_WIDTH", "$clog2(NUM_VC)"));
        this.addParameter(new Parameter("[VC_ADDRESS_WIDTH-1:0] ASSIGNED_VC [0:N-1]", "'{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}"));
    }

    private void addNocPorts() {
        // ports
        this.addPort(new Port("clk", Direction.INPUT, PortType.CLK, this, "noc.clk"));
        this.addPort(new Port("rst", Direction.INPUT, PortType.RST, this, "noc.rst"));

        for (int i = 0; i < nocNumRouters; i++) {
            this.addPort(new Port(buildNocPortName(PortType.CLK, Direction.INPUT, i), Direction.INPUT, 1, this));
            this.addPort(new Port(buildNocPortName(PortType.CLKINT, Direction.INPUT, i), Direction.INPUT, 1, this));

            this.addPort(new Port(buildNocPortName(PortType.DATA, Direction.INPUT, i), Direction.INPUT, nocInterfaceWidth, this));
            this.addPort(new Port(buildNocPortName(PortType.VALID, Direction.INPUT, i), Direction.INPUT, 1, this));
            this.addPort(new Port(buildNocPortName(PortType.READY, Direction.OUTPUT, i), Direction.OUTPUT, 1, this));

            this.addPort(new Port(buildNocPortName(PortType.DATA, Direction.OUTPUT, i), Direction.OUTPUT, nocInterfaceWidth, this));
            this.addPort(new Port(buildNocPortName(PortType.VALID, Direction.OUTPUT, i), Direction.OUTPUT, 1, this));
            this.addPort(new Port(buildNocPortName(PortType.READY, Direction.INPUT, i), Direction.INPUT, 1, this));
        }
    }

    private String buildNocPortName(PortType type, Direction direction, int router) {
        return "r" + router + "_" + type + "_" + direction.toShortString();
    }

    public final double[][] getAdjacencyMatrix() {
        double[][] matrix = new double[nocNumRouters][nocNumRouters];
        // init matrix
        for (int i = 0; i < nocNumRouters; i++)
            for (int j = 0; j < nocNumRouters; j++)
                matrix[i][j] = 0;
        for (int i = 0; i < nocNumRouters; i++)
            for (int j = 0; j < nocNumRouters; j++) {
                // we only have a mesh for now
                // each router (i) is connected to four other routers
                if ((j == (i - 1)) || (j == (i + 1)) || (j == (i + nocNumRoutersPerDimension))
                        || (j == (i - nocNumRoutersPerDimension)))
                    matrix[i][j] = 1;
            }
        return matrix;
    }

    public final double[][] getFullAdjacencyMatrix() {
        double[][] matrix = new double[nocNumRouters][nocNumRouters];
        // init matrix
        for (int i = 0; i < nocNumRouters; i++)
            for (int j = 0; j < nocNumRouters; j++)
                matrix[i][j] = 0;
        for (int i = 0; i < nocNumRouters; i++)
            for (int j = 0; j < nocNumRouters; j++) {
                // router connectivity is an int representing # hops
                matrix[i][j] = getNumberOfHops(i, j, this);
            }
        return matrix;
    }

    public static double getNumberOfHops(int i, int j, Noc noc) {
        return Math.floor((Math.abs(i - j) / noc.getNumRoutersPerDimension())) + Math.abs(i - j)
                % noc.getNumRoutersPerDimension();
    }

    public final int getRouterDegree(int routerIndex) {
        // corner router?
        if (routerIndex == 0 || routerIndex == nocNumRoutersPerDimension - 1 || routerIndex == nocNumRouters - 1
                || routerIndex == nocNumRouters - nocNumRoutersPerDimension)
            return 2;
        // edge router?
        else if (routerIndex < nocNumRoutersPerDimension || routerIndex % nocNumRoutersPerDimension == 0
                || routerIndex % nocNumRoutersPerDimension == nocNumRoutersPerDimension - 1
                || routerIndex >= nocNumRouters - nocNumRoutersPerDimension)
            return 3;
        // center router
        else
            return 4;
    }

    public final int getMaxHops() {
        return (int) getNumberOfHops(0, nocNumRouters - 1, this);
    }

    public final List<Integer> getPath(int fromRouter, int toRouter) {

        List<Integer> path = new ArrayList<Integer>();

        // TODO configurable routing function
        // will hard-code XY routing for now

        int fromRow = getNocRow(fromRouter);
        int fromCol = getNocCol(fromRouter);
        int toRow = getNocRow(toRouter);
        int toCol = getNocCol(toRouter);

        // find the path that constitutes this XY route
        int currRouter = fromRouter;
        path.add(currRouter);
        int currCol = fromCol;
        // X direction first
        while (currCol != toCol) {

            if (currCol < toCol)
                currRouter = currRouter + 1;
            else
                currRouter = currRouter - 1;

            currCol = getNocCol(currRouter);

            path.add(currRouter);
        }

        int currRow = fromRow;
        // Y direction second
        while (currRow != toRow) {

            if (currRow < toRow)
                currRouter = currRouter + nocNumRoutersPerDimension;
            else
                currRouter = currRouter - nocNumRoutersPerDimension;

            currRow = getNocRow(currRouter);

            path.add(currRouter);
        }

        return path;
    }

    private int getNocCol(int router) {
        return router / nocNumRoutersPerDimension;
    }

    private int getNocRow(int router) {
        return router % nocNumRoutersPerDimension;
    }
}

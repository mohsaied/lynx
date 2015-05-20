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

    private static final String nocName = "noc_wrapper";
    private static final String nocInstName = "noc";

    private static final String xmlWidth = "width";
    private static final String xmlNumrouters = "num_routers";
    private static final String xmlNumVcs = "num_vcs";
    private static final String xmlVcDepth = "vc_depth";
    private static final String xmlTdmFactor = "tdm_factor";

    private static final int defaultNocWidth = 150;
    private static final int defaultNocNumRouters = 16;
    private static final int defaultNocNumVcs = 2;
    private static final int defaultNocVcDepth = 16;
    private static final int defaultNocTdmFactor = 4;

    private int nocWidth;
    private int nocNumRouters;
    private int nocNumVcs;
    private int nocVcDepth;
    private int nocTdmFactor;

    private int nocInterfaceWidth;
    private int nocAddressWidth;
    private int nocVcAddressWidth;
    private int nocNumRoutersPerDimension;

    private ArrayList<ArrayList<NocBundle>> nocInBundles;
    private ArrayList<ArrayList<NocBundle>> nocOutBundles;

    public Noc() {
        super(nocName, nocInstName);

        configureNoC(defaultNocWidth, defaultNocNumRouters, defaultNocNumVcs, defaultNocVcDepth, defaultNocTdmFactor);
        calculateDerivedParameters();
        addNocParameters();
        addNocPorts();
        addNocBundles();
    }

    public Noc(int nocWidth, int nocNumRouters, int nocNumVcs, int nocVcDepth, int nocTdmFactor) {
        super(nocName, nocInstName);

        configureNoC(nocWidth, nocNumRouters, nocNumVcs, nocVcDepth, nocTdmFactor);
        calculateDerivedParameters();
        addNocParameters();
        addNocPorts();
        addNocBundles();
    }

    public Noc(String nocPath) throws ParserConfigurationException, SAXException, IOException {
        super(nocName, nocInstName);

        configureNoC(nocPath);
        calculateDerivedParameters();
        addNocParameters();
        addNocPorts();
        addNocBundles();
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

        if (varMap.containsKey(xmlTdmFactor))
            nocTdmFactor = varMap.get(xmlTdmFactor);
        else
            nocTdmFactor = defaultNocTdmFactor;
    }

    private void configureNoC(int nocWidth, int nocNumRouters, int nocNumVcs, int nocVcDepth, int nocTdmFactor) {
        this.nocWidth = nocWidth;
        this.nocNumRouters = nocNumRouters;
        this.nocNumVcs = nocNumVcs;
        this.nocVcDepth = nocVcDepth;
        this.nocTdmFactor = nocTdmFactor;
    }

    private void addNocBundles() {

        nocInBundles = new ArrayList<ArrayList<NocBundle>>();
        nocOutBundles = new ArrayList<ArrayList<NocBundle>>();

        // add a bundles for each NoC router
        for (int i = 0; i < nocNumRouters; i++) {

            // init arraylist
            ArrayList<NocBundle> nocbunInList = new ArrayList<NocBundle>();
            ArrayList<NocBundle> nocbunOutList = new ArrayList<NocBundle>();

            // number of bundles per router is equivalent to the tdm factor
            // each bundle has the NoC's width
            // input
            for (int j = 0; j < getNumNocBundlesInPerPort(); j++) {
                NocBundle nocbun = new NocBundle(i, j, Direction.INPUT, getNocBundleInWidth());
                nocbunInList.add(nocbun);
            }
            // output
            for (int j = 0; j < getNumNocBundlesOutPerPort(); j++) {
                NocBundle nocbun = new NocBundle(i, j, Direction.OUTPUT, getNocBundleOutWidth());
                nocbunOutList.add(nocbun);
            }

            nocInBundles.add(nocbunInList);
            nocOutBundles.add(nocbunOutList);
        }
    }

    private int getNocBundleOutWidth() {
        return getInterfaceWidth() / getNumNocBundlesOutPerPort();
    }

    private int getNocBundleInWidth() {
        return getInterfaceWidth() / getNumNocBundlesInPerPort();
    }

    private int getNumNocBundlesInPerPort() {
        return nocTdmFactor;
    }

    private int getNumNocBundlesOutPerPort() {
        return (nocNumVcs < nocTdmFactor ? nocNumVcs : nocTdmFactor);
    }

    public ArrayList<NocBundle> getNocInBundles(int router) {
        return nocInBundles.get(router);
    }

    public ArrayList<NocBundle> getNocOutBundles(int router) {
        return nocOutBundles.get(router);
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

    public int getTdmFactor() {
        return this.nocTdmFactor;
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
        nocInterfaceWidth = nocTdmFactor * nocWidth;
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
        String vcs = "'{";
        for (int i = 0; i < nocNumRouters; i++)
            if (i == nocNumRouters - 1)
                vcs += "0}";
            else
                vcs += "0,";
        this.addParameter(new Parameter("ASSIGNED_VC", vcs));
    }

    private void addNocPorts() {
        // ports
        this.addPort(new Port("clk_noc", Direction.INPUT, PortType.CLK, this, "clk_noc"));
        this.addPort(new Port("rst", Direction.INPUT, PortType.RST, this, "rst"));
        this.addPort(new Port("clk_rtl", Direction.INPUT, getNumRouters(), PortType.CLKRTL, this, "clk_rtl"));
        this.addPort(new Port("clk_int", Direction.INPUT, getNumRouters(), PortType.CLKINT, this, "clk_int"));

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

    public final int[][] getFullAdjacencyMatrix() {
        int[][] matrix = new int[nocNumRouters][nocNumRouters];
        // init matrix
        for (int i = 0; i < nocNumRouters; i++)
            for (int j = 0; j < nocNumRouters; j++)
                matrix[i][j] = 0;
        for (int i = 0; i < nocNumRouters; i++)
            for (int j = 0; j < nocNumRouters; j++) {
                // router connectivity is an int representing # hops
                matrix[i][j] = getNumberOfHops(i, j);
            }
        return matrix;
    }

    public int getNumberOfHops(int i, int j) {
        return getPath(i, j).size() - 1;
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
        return (int) getNumberOfHops(0, nocNumRouters - 1);
    }

    public final List<Integer> getPath(int fromRouter, int toRouter) {

        List<Integer> path = new ArrayList<Integer>();

        // TODO configurable routing function
        // will hard-code XY routing for now

        int fromRow = getNocRow(fromRouter);
        int fromCol = getNocCol(fromRouter);
        int toRow = getNocRow(toRouter);
        int toCol = getNocCol(toRouter);

        // System.out.println("from "+fromRouter+"("+fromCol+","+fromRow+")"+" to "+toRouter+"("+toCol+","+toRow+")");

        // find the path that constitutes this XY route
        int currRouter = fromRouter;
        path.add(currRouter);
        int currCol = fromCol;
        // X direction first
        while (currCol != toCol) {

            // System.out.println("col " + currCol + " router " + currRouter);

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

            // System.out.println("row " + currRow + " router " + currRouter);

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
        return router % nocNumRoutersPerDimension;
    }

    private int getNocRow(int router) {
        return router / nocNumRoutersPerDimension;
    }

    public void clearNocBundleStatus() {
        for (ArrayList<NocBundle> nocbunList : nocInBundles) {
            for (NocBundle nocbun : nocbunList) {
                nocbun.setUsed(false);
            }
        }
        for (ArrayList<NocBundle> nocbunList : nocOutBundles) {
            for (NocBundle nocbun : nocbunList) {
                nocbun.setUsed(false);
            }
        }
    }

    public String getModuleGlobalClockName(int router) {
        for (Port por : getPorts().values()) {
            if (por.getType() == PortType.CLKRTL)
                return por.getName() + "[" + router + "]";
        }
        assert false : "Can't find clkrtl for router " + router;
        return null;
    }

    public Port getNocClock() {
        for (Port por : getPorts().values()) {
            if (por.getType() == PortType.CLK)
                return por;
        }
        assert false : "Can't find clknoc in noc!";
        return null;
    }

    public Port getIntClock() {
        for (Port por : getPorts().values()) {
            if (por.getType() == PortType.CLKINT)
                return por;
        }
        assert false : "Can't find clkint in noc!";
        return null;
    }

    public Port getRtlClock() {
        for (Port por : getPorts().values()) {
            if (por.getType() == PortType.CLKRTL)
                return por;
        }
        assert false : "Can't find clkrtl in noc!";
        return null;
    }

    public Port getNocRst() {
        for (Port por : getPorts().values()) {
            if (por.getType() == PortType.RST)
                return por;
        }
        assert false : "Can't find rst in noc!";
        return null;
    }

    public Noc clone() {
        return new Noc(nocWidth, nocNumRouters, nocNumVcs, nocVcDepth, nocTdmFactor);
    }

}

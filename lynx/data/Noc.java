package lynx.data;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import lynx.data.MyEnums.Direction;
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

    private void calculateDerivedParameters() {
        // derived parameters
        nocInterfaceWidth = 4 * nocWidth;
        nocAddressWidth = (int) Math.ceil(Math.log10(nocNumRouters));
        nocVcAddressWidth = (int) Math.ceil(Math.log10(nocNumVcs));
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
        this.addParameter(new Parameter("[VC_ADDRESS_WIDTH-1:0] ASSIGNED_VC [0:N-1]",
                "'{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}"));
    }

    private void addNocPorts() {
        // ports
        this.addPort(new Port("clk_noc", Direction.INPUT, 1, 1, this));
        this.addPort(new Port("rst", Direction.INPUT, 1, 1, this));
        this.addPort(new Port("clk_rtl", Direction.INPUT, nocNumRouters, 1, this));
        this.addPort(new Port("clk_int", Direction.INPUT, nocNumRouters, 1, this));

        this.addPort(new Port("i_packets_in", Direction.INPUT, nocInterfaceWidth, nocNumRouters, this));
        this.addPort(new Port("i_valids_in", Direction.INPUT, 1, nocNumRouters, this));
        this.addPort(new Port("i_readys_out", Direction.OUTPUT, 1, nocNumRouters, this));

        this.addPort(new Port("o_packets_out", Direction.OUTPUT, nocInterfaceWidth, nocNumRouters, this));
        this.addPort(new Port("o_valids_out", Direction.OUTPUT, 1, nocNumRouters, this));
        this.addPort(new Port("o_readys_in", Direction.INPUT, 1, nocNumRouters, this));
    }

}

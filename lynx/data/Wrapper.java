package lynx.data;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;

/**
 * Wrappers are modules that aren't part of the application logic but are
 * inserted to the design to do things like traffic management
 * 
 * @author Mohamed
 *
 */
public class Wrapper extends Module {

    DesignModule parentModule;

    public Wrapper(String type, String name, DesignModule parentModule) {
        super(type, name);
        this.parentModule = parentModule;

        Port clk = new Port("clk", Direction.INPUT, PortType.CLK, this, parentModule.getPortByName("clk").getGlobalPortName());
        clk.setGlobalOnNoc(true);
        this.addPort(clk);
        Port rst = new Port("rst", Direction.INPUT, PortType.RST, this, parentModule.getPortByName("rst").getGlobalPortName());
        rst.setGlobalOnNoc(true);
        this.addPort(rst);
    }

    public DesignModule getParentModule() {
        return parentModule;
    }

}

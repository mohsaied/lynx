package lynx.data;

public class MyEnums {
    
    public static final String NOCLYNX = "NOCLYNX";

    public enum Direction {
        INPUT("input"), 
        OUTPUT("output"), 
        UNKNOWN("unknown");

        private final String name;

        private Direction(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }
        
        public String toShortString() {
            switch (name) {
            case "input":
                return "in";
            case "output":
                return "out";
            default:
                return "un";
            }
        }
    }

    public enum PortType {
        DATA("data"), 
        VALID("valid"), 
        READY("ready"),  
        DST("dst"),
        CLK("clk"), 
        CLKINT("clkint"),
        RST("rst"), 
        TOP("top"),
        UNKNOWN("unknown");

        private final String name;

        private PortType(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum TranslatorType {
        PACKETIZER("packetizer"), 
        DEPACKETIZER("depacketizer");

        private final String name;

        private TranslatorType(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }

        public String toShortString() {
            switch (name) {
            case "packetizer":
                return "pkt";
            case "depacketizer":
                return "dpkt";
            default:
                return "error";
            }
        }
    }
    
    public enum BundleStatus {
        UNCONNECTED("unconnected"), 
        NOC("noc"), 
        OTHER("other");

        private final String name;

        private BundleStatus(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }
        
        public String toShortString() {
            switch (name) {
            case "input":
                return "in";
            case "output":
                return "out";
            default:
                return "un";
            }
        }
    }

}

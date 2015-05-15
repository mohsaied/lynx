package lynx.data;

public class MyEnums {

    public static final String NOCLYNX = "NOCLYNX";

    public enum Direction {
        INPUT("input"), OUTPUT("output"), UNKNOWN("unknown");

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
        DATA("data"), VALID("valid"), READY("ready"), DST("dst"), CLK("clk"), CLKINT("clkint"), CLKRTL("clkrtl"), RST("rst"), TOP(
                "top"), UNKNOWN("unknown");

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
        PACKETIZER_1("packetizer_1"), PACKETIZER_2("packetizer_2"), PACKETIZER_3("packetizer_3"), PACKETIZER_4("packetizer_4"), DEPACKETIZER_2(
                "depacketizer_2"), DEPACKETIZER_4("depacketizer_4");

        private final String name;

        private TranslatorType(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }

        public String toShortString() {
            if (name.contains("packetizer"))
                return "pkt";
            else if (name.contains("depacketizer"))
                return "depkt";
            else
                return "error";
        }
    }

    public enum BundleStatus {
        UNCONNECTED("unconnected"), NOC("noc"), OTHER("other");

        private final String name;

        private BundleStatus(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ConnectionType {
        P2P("p2p"), ARBITRATION("arbitration"), BROADCAST("broadcast"), UNKNOWN("unknown");

        private final String name;

        private ConnectionType(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

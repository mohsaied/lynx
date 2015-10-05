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
        DATA("data"), VALID("valid"), READY("ready"), DST("dst"), VC("vc"), CLK("clk"), CLKINT("clkint"), CLKRTL("clkrtl"), RST(
                "rst"), TOP("top"), DONE("done"), WAITFORREPLY("wait_for_reply"), UNKNOWN("unknown"), CONSTANT("constant"), QUARTERCLK(
                "clk");

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
        PACKETIZER_STD("packetizer_std"), DEPACKETIZER_STD("depacketizer_std");

        private final String name;

        private TranslatorType(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }

        public String toShortString() {
            if (name.contains("depacketizer"))
                return "depkt";
            else if (name.contains("packetizer"))
                return "pkt";
            else
                return "error";
        }
    }

    public enum SimModType {
        SRC("SRC"), SINK("SINK");

        private final String name;

        private SimModType(String s) {
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

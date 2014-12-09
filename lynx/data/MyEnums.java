package lynx.data;

public class MyEnums {

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
    }

    public enum PortType {
        DATA("data"), 
        VALID("valid"), 
        READY("ready"), 
        CLK("clk"), 
        RST("rst"), 
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

}
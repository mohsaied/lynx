package lynx.data;

import lynx.data.MyEnums.Direction;

public class NocBundle {

    private Noc noc;

    private int router;
    private int index;

    private Direction direction;

    private int width;

    public NocBundle(Noc noc, int router, int index, Direction direction, int width) {
        this.noc = noc;
        this.router = router;
        this.index = index;
        this.direction = direction;
        this.width = width;
    }

    public int getRouter() {
        return router;
    }

    /**
     * The index indicates where on the noc Port this noc bundle resides For
     * example: index 0 means it resides on flit 0, or bits 0..width-1
     * 
     * @return the int index which ranges from 0 to numNocBundlesAtThisRouter-1
     */
    public int getIndex() {
        return index;
    }

    public Noc getNoc() {
        return this.noc;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public String toString() {
        String s = "router = " + router + ", index = " + index + ", direction" + direction + ", width = " + width;
        return s;
    }

}

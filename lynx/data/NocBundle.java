package lynx.data;

import lynx.data.MyEnums.Direction;

public class NocBundle {

    private boolean used;

    private int router;

    private Direction direction;

    private int width;

    public NocBundle(int router, Direction direction, int width) {
        this.used = false;
        this.router = router;
        this.direction = direction;
        this.width = width;
    }

    public int getRouter() {
        return router;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getWidth() {
        return width;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean status) {
        this.used = status;
    }
}

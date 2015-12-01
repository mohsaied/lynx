package lynx.graphics;

import com.mxgraph.view.mxGraph;

public class mxGraphEdited extends mxGraph {
    @Override
    public boolean isCellSelectable(Object cell) {
        if (model.isEdge(cell)) {
            return false;
        }
        return isCellsSelectable();
    }
}

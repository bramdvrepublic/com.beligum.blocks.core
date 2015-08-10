base.plugin("blocks.core.Overlay", ["constants.blocks.core", function (CoreConstants)
{
    var Overlay = this;

    //-----VARIABLES-----
    var hoveredBlock = null;
    var hoveredProperty = null;
    var oldLayoutTree = null;
    var oldContainerParent = null;

    //-----METHODS-----
    this.setHoveredBlock = function (layoutElement)
    {
        hoveredBlock = layoutElement;
    };
    this.getHoveredBlock = function ()
    {
        return hoveredBlock;
    };

    this.setHoveredProperty = function (layoutElement)
    {
        hoveredProperty = layoutElement;
    };
    this.getHoveredProperty = function ()
    {
        return hoveredProperty;
    };
    /*
     * Container is the block IN which we are dragging.
     * If we set this to null then then the top level block(s) are the container
     * */
    this.setContainer = function (value)
    {
        layoutTree = value
    };
    this.getContainer = function ()
    {
        return layoutTree;
    };

    /*
     We create some sort of a heat map. We define boxes for all draggable templates
     we can add left and right from each column
     and left and right from container if container has more than 1 row
     select each row and add bottom
     if row has +1 colunms, we can add also to bottom of columns
     except if column has +1 rows
     */
    this.buildLayoutTree = function ()
    {
        oldLayoutTree = null;
        oldContainerParent = null;
        layoutTree = new blocks.elements.Page();
    };

    Overlay.showOverlays = function ()
    {
        if (Overlay.getContainer() != null) {
            var elements = Overlay.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i].canDrag) {
                    elements[i].showOverlay();
                }
            }
        }
    };

    Overlay.removeOverlays = function ()
    {
        if (Overlay.getContainer() != null) {
            var elements = Overlay.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                elements[i].removeOverlay();
            }
        }
    };

    Overlay.removeResizehandles = function ()
    {
        if (Overlay.getContainer() != null) {
            var elements = Overlay.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i] instanceof blocks.elements.Row) {
                    elements[i].removeOverlay();
                }
            }
        }
    };

    Overlay.showResizehandles = function ()
    {
        if (Overlay.getContainer() != null) {
            var elements = Overlay.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i] instanceof blocks.elements.Row) {
                    elements[i].showOverlay();
                }
            }
        }
    };

}]);
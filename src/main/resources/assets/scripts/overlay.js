base.plugin("blocks.core.Overlay", ["blocks.core.Broadcaster", function (Broadcaster)
{
    var Overlay = this;

    Overlay.showOverlays = function ()
    {
        if (Broadcaster.getContainer() != null) {
            var elements = Broadcaster.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i].canDrag) {
                    elements[i].showOverlay();
                }
            }
        }
    };

    Overlay.removeOverlays = function ()
    {
        if (Broadcaster.getContainer() != null) {
            var elements = Broadcaster.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                elements[i].removeOverlay();
            }
        }
    };

    Overlay.removeResizehandles = function ()
    {
        if (Broadcaster.getContainer() != null) {
            var elements = Broadcaster.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i] instanceof blocks.elements.Row) {
                    elements[i].removeOverlay();
                }
            }
        }
    };

    Overlay.showResizehandles = function ()
    {
        if (Broadcaster.getContainer() != null) {
            var elements = Broadcaster.getContainer().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i] instanceof blocks.elements.Row) {
                    elements[i].showOverlay();

                }
            }
        }
    };

}]);
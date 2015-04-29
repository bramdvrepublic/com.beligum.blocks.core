base.plugin("blocks.core.Overlay", ["blocks.core.Constants", "blocks.core.Broadcaster", function (Constants, Broadcaster)
{
    var Overlay = this;

    Overlay.showOverlays = function ()
    {
        var elements = Broadcaster.getContainer().findElements(0, 9);
        for (var i = 0; i < elements.length; i++) {
            if (elements[i].editType != Constants.EDIT_NONE || elements[i].canDrag) {
                elements[i].showOverlay();
            }
        }
    };

    Overlay.removeResizehandles = function ()
    {
        var elements = Broadcaster.getContainer().findElements(0, 9);
        for (var i = 0; i < elements.length; i++) {
            if (elements[i] instanceof blocks.elements.Row) {
                elements[i].removeOverlay();

            }
        }
    };

    Overlay.showResizehandles = function ()
    {
        var elements = Broadcaster.getContainer().findElements(0, 9);
        for (var i = 0; i < elements.length; i++) {
            if (elements[i] instanceof blocks.elements.Row) {
                elements[i].showOverlay();

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

    //Overlay.showHover = function(block) {
    //    block.overlay.addClass(Constants.BLOCK_HOVER_CLASS);
    //};
    //
    //Overlay.removeHover = function(block) {
    //    block.overlay.removeClass(Constants.BLOCK_HOVER_CLASS);
    //};


    var hideAll = function (element)
    {
        if (element.prop("tagName") != "BODY") {
            var siblings = element.siblings().addClass("not-visible");
            hideAll(element.parent());
        }
    };


    var overlayList = [];

    $(document).on("mouseup.blocks_overlay", function (event)
    {
        if (overlayList.length > 0) {
            var overlayItem = overlayList.pop();

            if (outsideElement(overlayItem.element, event) && overlayItem.allowRemove(event)) {
                $(".not-visible").removeClass("not-visible");
                var overlayListLength = overlayList.length;
                if (overlayListLength > 0) {
                    hideAll(overlayList[overlayListLength - 1].element);
                    Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, overlayList[overlayListLength - 1].element);
                } else {
                    Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, null);
                }
                overlayItem.callback();
            } else {
                overlayList.push(overlayItem);
            }
        }
    });


}]);
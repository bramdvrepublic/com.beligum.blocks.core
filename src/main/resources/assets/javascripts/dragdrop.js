/**
 * Created by wouter on 8/10/14.
 */

blocks.plugin("blocks.core.DragDrop", ["blocks.core.Broadcaster", "blocks.core.Layouter", "blocks.core.Mouse", "blocks.core.Constants", function (Broadcaster, Layouter, Mouse, Constants) {


    var dragStarted = function (blockEvent) {
        createDraggedOverlay(blockEvent.drag.surface);
        createDropPointer("anchor");
        createDropPointer("other");
        dragging = true;
        $("body").css("cursor", "crosshair");
    };

    var dragEnded = function (blockEvent) {
        $("body").css("cursor", "auto");
        removeDropPointer("anchor");
        removeDropPointer("other");
        removeDraggedOverlay();
        if (blockEvent.block.current != null) {
            Logger.debug("Drop block");
            var dropSpot = blockEvent.block.current.getTriggeredHotspot(blockEvent.direction, blockEvent.event);
            if (!dropSpotIsDraggedBlock(dropSpot, blockEvent)) {
                Layouter.changeBlockLocation(blockEvent.drag.surface, dropSpot.anchor, dropSpot.side);
            } else {
                // do nothing. You can not drop on the block you're dragging
            }
        } else {
            Logger.debug("No drop for block");
        }
        currentBlock = null;
        dragging = false;
    };

    var dropSpotIsDraggedBlock = function(dropSpot, blockEvent) {
        var retVal = false;
        if (dropSpot.other() == null || dropSpot.anchor == null || blockEvent == null) return retVal;

        if ((blockEvent.drag.surface === dropSpot.anchor || blockEvent.drag.surface === dropSpot.other())) {
            retVal = true
        }
        else if (dropSpot.anchor.children.length === 1 && dropSpot.anchor.children[0] === blockEvent.drag.surface) {
            retVal = true;
        } else if (dropSpot.other().children.length === 1 && dropSpot.other().children[0] === blockEvent.drag.surface) {
            retVal = true;
        }
        return retVal;
    }

    var dragLeaveBlock = function (blockEvent) {
        if (dragging && blockEvent.block.current == null) {
            hideDropPointer("anchor");
            hideDropPointer("other")
        }
    };

    var dragOverBlock = function(blockEvent) {
        if (dragging) {
            Logger.debug("direction: " + blockEvent.direction);

            var dropSpot = blockEvent.block.current.getTriggeredHotspot(blockEvent.direction, blockEvent.event);
            if (lastDropLocation != dropSpot) {
                lastDropLocation = dropSpot;
                if (dropSpotIsDraggedBlock(dropSpot, blockEvent)) {
                    hideDropPointer("anchor");
                    hideDropPointer("other")
                } else if (lastDropLocation != null) {
                    drawDropPointer("anchor", lastDropLocation.anchor, lastDropLocation.side);
                    drawDropPointer("other", lastDropLocation.other(), Constants.OPPOSITE_SIDE[lastDropLocation.side]);
                }
            } else {
                Logger.debug("Did not change droplocation");
            }

        }
    };

    var createDropPointer = function (name) {
        if (dropPointerElements[name] == null) {
            dropPointerElements[name] = $("<div class='blocks-dropspot' />");
            dropPointerElements[name].append(
                $("<div style='position:absolute; top: 50%; left:50%'/>")
                    .append($("<div style='position:relative; color: white; font-size:48px; top: -24px; left:-24px; '></div>"))
            ); // elemnet for arrow
            $("body").append(dropPointerElements[name]);
            dropPointerElements[name].css("position", "absolute");
            dropPointerElements[name].css("background-color", "rgba(119, 119, 119, 0.5)");
        }
        hideDropPointer(name);
    };

    var removeDropPointer = function (name) {
        if (dropPointerElements[name] != null) {
            dropPointerElements[name].remove();
            dropPointerElements[name] = null;
        }
    };

    var drawDropPointer = function (name, surface, side) {
        if (surface != null) {
            dropPointerElements[name].css("top", surface.top + "px");
            dropPointerElements[name].css("left", surface.left + "px");
            dropPointerElements[name].css("width", surface.right - surface.left + "px");
            dropPointerElements[name].css("height", surface.bottom - surface.top + "px");
            dropPointerElements[name].css("border", "");
            if (side != null) {
                dropPointerElements[name].css(cssSide[side], "2px solid rgba(0, 0, 119, 1)");
            }
            showArrow(dropPointerElements[name], Constants.OPPOSITE_SIDE[side]);

        } else {
            hideDropPointer(name);
        }
    };

    var createDraggedOverlay = function(surface) {
        if (draggedOverlay == null) {
            draggedOverlay = $("<div style=\"position: absolute; left:"+surface.left+"px; top:"+surface.top+"px; height: "+(surface.bottom - surface.top)+"px; width:"+(surface.right-surface.left)+"px; background: url('/assets/images/blocks/stripe.png');\" />");
            $("body").append(draggedOverlay);
        }
    };

    var removeDraggedOverlay = function() {
        if (draggedOverlay != null) {
            draggedOverlay.remove();
            draggedOverlay = null;
        }
    };

    var showArrow= function(droppointer, side) {
        var arrow = $($(droppointer.children()[0]).children()[0]).removeClass();
        Logger.debug(" arrow height: " + arrow.height() + "  arrow width: " + arrow.width());
        arrow.addClass("glyphicon");
        arrow.addClass(cssArrowClass[side]);
    };


    var hideDropPointer = function(name) {
        dropPointerElements[name].css("top", "0px");
        dropPointerElements[name].css("left", "0px");
        dropPointerElements[name].css("width", "0px");
        dropPointerElements[name].css("height", "0px");
    }

    var cssSide = {};
    cssSide[Constants.SIDE.TOP] = "border-top";
    cssSide[Constants.SIDE.BOTTOM] = "border-bottom";
    cssSide[Constants.SIDE.LEFT] = "border-left";
    cssSide[Constants.SIDE.RIGHT] = "border-right";

    var cssArrowClass = {};
    cssArrowClass[Constants.SIDE.TOP] = "glyphicon-arrow-up";
    cssArrowClass[Constants.SIDE.BOTTOM] = "glyphicon-arrow-down";
    cssArrowClass[Constants.SIDE.LEFT] = "glyphicon-arrow-left";
    cssArrowClass[Constants.SIDE.RIGHT] = "glyphicon-arrow-right";


    var dragging = false;
    var draggedOverlay = null;
    var dropPointerElements = {};
    var lastDropLocation = null;
    var currentBlock = null;

    Broadcaster.on(Mouse.config.EVENT.START_DRAG_BLOCK, function (event) {
        dragStarted(event)
    });
    Broadcaster.on(Mouse.config.EVENT.END_DRAG_BLOCK, function (event) {
        dragEnded(event)
    });
//    Broadcaster.on(Mouse.config.EVENT.DRAG_ENTER_BLOCK, function (event) {
//        enterBlock(event)
//    });
    Broadcaster.on(Mouse.config.EVENT.DRAG_LEAVE_BLOCK, function (event) {
        dragLeaveBlock(event)
    });
    Broadcaster.on(Mouse.config.EVENT.DRAG_OVER_BLOCK, function (event) {
        dragOverBlock(event)
    });
}]);




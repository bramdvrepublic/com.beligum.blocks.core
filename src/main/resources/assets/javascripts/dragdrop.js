/**
 * Created by wouter on 8/10/14.
 */

blocks.module("blocks.dragdrop", ["blocks.mouseEvent", "blocks.elements"])
    .service("DragDropBlocks", ["BlockMouseEvents", "Broadcaster", "Elements", "Layouter", function(BlockMouseEvents, Broadcaster, Elements, Layouter) {


        var dragStarted = function(blockEvent) {
            checkDropspotExists();
            dragging = true;
            $("body").css("cursor", "crosshair");
        };

        var dragEnded = function(blockEvent) {
            $("body").css("cursor", "auto");
            removeDropSpot();
            if (blockEvent.dropSpot.current != null) {
                Logger.debug("Drop block");
                Layouter.changeBlockLocation(blockEvent.drag.surface, blockEvent.dropSpot.current.parent, blockEvent.dropSpot.current.side);
            } else {
                Logger.debug("No drop for block");
            }
            dragging = false;
        };

        var enterDropspot = function(blockEvent) {
            if (dragging) {
                checkDropspotExists();
                Logger.debug('Show Dropspot');
                dropspot.css("top", blockEvent.dropSpot.current.drawSurface.top + "px");
                dropspot.css("left", blockEvent.dropSpot.current.drawSurface.left + "px");
                dropspot.css("width", blockEvent.dropSpot.current.drawSurface.right - blockEvent.dropSpot.current.drawSurface.left + "px");
                dropspot.css("height", blockEvent.dropSpot.current.drawSurface.bottom - blockEvent.dropSpot.current.drawSurface.top + "px");
            }
        };

        var leaveDropspot = function(blockEvent) {
            if (dragging && blockEvent.dropSpot.current == null) {
                removeDropSpot();
            }
        };

        var hideBlock = function(blockEvent) {
            if (blockEvent.dragging == true) {
                blockEvent.drag.surface.element.fadeOut(300, function() {changeBlockLocation(blockEvent)});
            }
        }

        var changeBlockLocation = function(blockEvent) {
            var draggedBlock = blockEvent.drag.surface;
            var draggedElement = draggedBlock.element.remove();
            var blockParent = blockEvent.drag.surface.parent;
            var dropSpot = blockEvent.dropSpot.current;
            var dropParent = dropSpot.parent;
            if (draggedBlock != dropParent) {
                if (dropParent instanceof Elements.Block) {
                    if (dropSpot.side == Elements.DropSpot.SIDE.TOP) {
                        dropParent.element.before(draggedElement);
                    } else if (dropSpot.side == Elements.DropSpot.SIDE.BOTTOM) {
                        dropParent.element.after(draggedElement);
                    }
                }
            }
            showBlock(blockEvent);
        }

        var showBlock = function(blockEvent) {
            blockEvent.drag.surface.element.fadeIn(300);
            Broadcaster.send(BlockMouseEvents.REFRESH_LAYOUT);
        }

        var checkDropspotExists = function() {
            if (dropspot == null) {
                $(".dropspot").remove();
                dropspot = $("<div class='blocks-dropspot' />");
                $("body").append(dropspot);
                dropspot.css("position", "absolute");
                dropspot.css("background-color", "black");
            }
        }

        var removeDropSpot = function() {
            if (dropspot != null) {
                dropspot.remove();
                dropspot = null;
            }
        }

        var dragging = false;
        var dropspot = null;

        Broadcaster.on(BlockMouseEvents.START_DRAG_BLOCK, function(event) {
            dragStarted(event)});
        Broadcaster.on(BlockMouseEvents.END_DRAG_BLOCK, function(event) {dragEnded(event)});
        Broadcaster.on(BlockMouseEvents.DRAG_ENTER_DROPSPOT, function(event) {enterDropspot(event)});
        Broadcaster.on(BlockMouseEvents.DRAG_LEAVE_DROPSPOT, function(event) {leaveDropspot(event)});
}]);




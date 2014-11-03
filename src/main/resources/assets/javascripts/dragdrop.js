/**
 * Plugin that handles the dragging of blocks
 *
 * While dragging we create 2 dropPointers, "anchor" and "other".
 * Droppointers are Dom elements that overlay the block that takes the drop. (anchor) If we
 * drop between 2 blocks, we also overlay the other block (other)
 * We show arrows in the overlay to indicate the direction the block will move.
 *
 */

blocks.plugin("blocks.core.DragDrop", ["blocks.core.Broadcaster", "blocks.core.Elements", "blocks.core.Layouter", "blocks.core.Constants", function (Broadcaster, Elements, Layouter, Constants) {

    var dragging = false;
    var draggedOverlay = null;
    var dropPointerElements = {};
    var lastDropLocation = null;
    var currentBlock = null;

    /*
    * METHODS CALLED WHILE DRAGGING
    * */

    var dragStarted = function (blockEvent) {
        if (blockEvent.draggingOptions.surface instanceof Elements.Block) {
            currentBlock = blockEvent.draggingOptions.surface;
            createDraggedOverlay(blockEvent.draggingOptions.surface);
            createDropPointerElement("anchor");
            createDropPointerElement("other");
            dragging = true;
            $("body").css("cursor", "crosshair");
        }
    };

    var dragLeaveBlock = function (blockEvent) {
        if (dragging && blockEvent.block.current == null) {
            hideDropPointerElement("anchor");
            hideDropPointerElement("other")
        }
    };


    var dragOverBlock = function(blockEvent) {
        if (dragging) {
            Logger.debug("direction: " + blockEvent.direction);
            // find the triggered dropspot
            // dropspot has an "anchor" block and sometimes "other" (when dropping between 2 columns, 2 rows, 2 blocks)
            var dropSpot = blockEvent.block.current.getTriggeredDropspot(blockEvent.direction, blockEvent.event.pageX, blockEvent.event.pageY);
            if (lastDropLocation != dropSpot) {
                lastDropLocation = dropSpot;
                // We can not drop on ourselves so skip
                if (dropSpotIsDraggedBlock(dropSpot, blockEvent)) {
                    hideDropPointerElement("anchor");
                    hideDropPointerElement("other")
                } else if (lastDropLocation != null) {
                    // show overlays for our droplocation(s)
                    drawDropPointerElement("anchor", lastDropLocation.anchor, lastDropLocation.side);
                    drawDropPointerElement("other", lastDropLocation.other(), Constants.OPPOSITE_SIDE[lastDropLocation.side]);
                }
            } else {
                Logger.debug("Did not change droplocation");
            }

        }
    };

    var dragEnded = function (blockEvent) {
        if (dragging) {
            $("body").css("cursor", "auto");
            removeDropPointerElement("anchor");
            removeDropPointerElement("other");
            removeDraggedOverlay();

            // Dragged out of window (drag should've send an ABORT_DRAG event)
            if (blockEvent.event.pageX > $(document).innerWidth() || blockEvent.event.pageX < 0 || blockEvent.event.pageY > $(document).innerHeight() || blockEvent.event.pageY < 0) {
                currentBlock = null;
                dragging = false;
            };

            if (blockEvent.block.current != null) {
                Logger.debug("Drop block");
                var dropSpot = blockEvent.block.current.getTriggeredDropspot(blockEvent.direction, blockEvent.event.pageX, blockEvent.event.pageY);
                // If we did not drop on ourself, change location
                if (!dropSpotIsDraggedBlock(dropSpot, blockEvent)) {
                    Layouter.changeBlockLocation(blockEvent.draggingOptions.surface, dropSpot.anchor, dropSpot.side);
                } else {
                    // do nothing. You can not drop on the block you're dragging
                }
            } else {
                Logger.debug("No drop for block");
            }
            currentBlock = null;
            dragging = false;
        }
    };


    /*
    * checks if the droplocation (or dropspot) equals the block we are dragging
    *
    * */
    var dropSpotIsDraggedBlock = function(dropSpot, blockEvent) {
        var retVal = false;
        if (blockEvent.draggingOptions.surface == null || dropSpot == null) return retVal;
        // dragged block equals anchor or other
        if ((blockEvent.draggingOptions.surface === dropSpot.anchor || blockEvent.draggingOptions.surface === dropSpot.other())) {
            retVal = true
        } else if (dropSpot.anchor != null && dropSpot.anchor.children.length == 1 && dropSpot.anchor.children[0] === blockEvent.draggingOptions.surface) {
            // We drop a
            retVal = true;
        } else if (dropSpot.other() != null && dropSpot.other().children.length == 1 && dropSpot.other().children[0] === blockEvent.draggingOptions.surface) {
            retVal = true;
        }
        return retVal;
    };



    // create drop pointer as element in DOM
    var createDropPointerElement = function (name) {
        if (dropPointerElements[name] == null) {
            dropPointerElements[name] = $("<div class='blocks-dropspot' />");
            // TODO position close to blue lin
            // TODO make drop line thicker
            dropPointerElements[name].append(
                $("<div style='position:absolute; top: 50%; left:50%'/>")
                    .append($("<div style='position:relative; color: white; font-size:48px; top: -24px; left:-24px; '></div>"))
            ); // element for arrow
            $("body").append(dropPointerElements[name]);
            dropPointerElements[name].css("position", "absolute");
            dropPointerElements[name].css("background-color", "rgba(119, 119, 119, 0.5)");
        }
        hideDropPointerElement(name);
    };

    // remove droppointer as element in dom
    var removeDropPointerElement = function (name) {
        if (dropPointerElements[name] != null) {
            dropPointerElements[name].remove();
            dropPointerElements[name] = null;
        }
    };

    // update droppointer in dom
    // name = anchor or other
    var drawDropPointerElement = function (name, surface, side) {
        if (surface != null) {
            dropPointerElements[name].css("top", surface.top + "px");
            dropPointerElements[name].css("left", surface.left + "px");
            dropPointerElements[name].css("width", surface.right - surface.left + "px");
            dropPointerElements[name].css("height", surface.bottom - surface.top + "px");
            dropPointerElements[name].css("border", "");
            if (side != null) {
                dropPointerElements[name].css(cssSide[side], "2px solid rgba(0, 0, 119, 1)");
            }
            showArrowInDroppointerElement(dropPointerElements[name], Constants.OPPOSITE_SIDE[side]);

        } else {
            hideDropPointerElement(name);
        }
    };

    // show arrow in droppointer overlay
    var showArrowInDroppointerElement= function(droppointer, side) {
        var arrow = $($(droppointer.children()[0]).children()[0]).removeClass();
        Logger.debug(" arrow height: " + arrow.height() + "  arrow width: " + arrow.width());
        arrow.addClass("glyphicon");
        arrow.addClass(cssArrowClass[side]);
    };


    var hideDropPointerElement = function(name) {
        dropPointerElements[name].css("top", "0px");
        dropPointerElements[name].css("left", "0px");
        dropPointerElements[name].css("width", "0px");
        dropPointerElements[name].css("height", "0px");
    }

    // Creates an overlay for the block we are dragging to show that we can not drop there
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

    // Simple object to translate SIDE in css border side
    var cssSide = {};
    cssSide[Constants.SIDE.TOP] = "border-top";
    cssSide[Constants.SIDE.BOTTOM] = "border-bottom";
    cssSide[Constants.SIDE.LEFT] = "border-left";
    cssSide[Constants.SIDE.RIGHT] = "border-right";

    // Simple object to translate SIDE in correct glyphicon class for arrow
    // TODO put this in config? but how?
    var cssArrowClass = {};
    cssArrowClass[Constants.SIDE.TOP] = "glyphicon-arrow-up";
    cssArrowClass[Constants.SIDE.BOTTOM] = "glyphicon-arrow-down";
    cssArrowClass[Constants.SIDE.LEFT] = "glyphicon-arrow-left";
    cssArrowClass[Constants.SIDE.RIGHT] = "glyphicon-arrow-right";




    // on hoover block, can start draggingOptions with priority
    // on leave block, can_not_start draggingOptions
    var allowDrag = function(blockEvent) {
        if (blockEvent.block.current != null) {
            if (blockEvent.block.current != currentBlock) {
                currentBlock = blockEvent.block.current;
                Broadcaster.send(Broadcaster.EVENTS.CAN_START_DRAG, {surface: currentBlock, priority: 200});
            } else if (blockEvent.draggingStatus == Constants.DRAGGING.CAN_NOT_START_DRAG) {
                Broadcaster.send(Broadcaster.EVENTS.CAN_START_DRAG, {surface: currentBlock, priority: 200});
            }
        } else if (blockEvent.block.current != currentBlock) {
            Broadcaster.send(Broadcaster.EVENTS.CAN_NOT_START_DRAG, {surface: currentBlock, priority: 200});
            currentBlock = null;
        }
    };


    Broadcaster.on(Broadcaster.EVENTS.HOOVER_OVER_BLOCK, function (event) {
        allowDrag(event)
    });
    Broadcaster.on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, function (event) {
        allowDrag(event)
    });


    Broadcaster.on(Broadcaster.EVENTS.START_DRAG, function (event) {
        dragStarted(event)
    });
    Broadcaster.on(Broadcaster.EVENTS.END_DRAG, function (event) {
        dragEnded(event)
    });
    Broadcaster.on(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK, function (event) {
        dragLeaveBlock(event)
    });
    Broadcaster.on(Broadcaster.EVENTS.DRAG_OVER_BLOCK, function (event) {
        dragOverBlock(event)
    });
}]);




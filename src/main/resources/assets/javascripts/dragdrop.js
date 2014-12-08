/**
 * Plugin that handles the dragging of blocks
 *
 * While dragging we create 2 dropPointers, "anchor" and "other".
 * Droppointers are Dom elements that overlay the block that takes the drop. (anchor) If we
 * drop between 2 blocks, we also overlay the other block (other)
 * We show arrows in the overlay to indicate the direction the block will move.
 *
 */

blocks.plugin("blocks.core.DragDrop", ["blocks.core.Broadcaster", "blocks.core.Elements", "blocks.core.Layouter", "blocks.core.Constants", "blocks.core.Overlay", function (Broadcaster, Elements, Layouter, Constants, Overlay) {

    var draggingEnabled = false;
    var dragging = false;
    var draggedOverlay = null;
    var dropPointerElements = {};
    var lastDropLocation = null;
    var currentDraggedBlock = null;

    /*
    * METHODS CALLED WHILE DRAGGING
    **/

    var dragStarted = function (blockEvent) {
        Logger.debug("drag started");
        currentDraggedBlock = Broadcaster.getHooveredBlockForPosition(blockEvent.pageX, blockEvent.pageY).current;
        if (draggingEnabled && currentDraggedBlock != null) {
            createDraggedOverlay(currentDraggedBlock);
            createDropPointerElement("anchor");
            createDropPointerElement("other");
            dragging = true;
            // we have to set both
            // html for undefined area and baody to override default cursor of body.
            $("html").css("cursor", "pointer");
            $("body").css("cursor", "pointer");
        }
    };

    // TODO check necessity canLayout
    var dragLeaveBlock = function (blockEvent) {
        if (dragging && (blockEvent.block.current == null || !blockEvent.block.current.canDrag)) {
            hideDropPointerElement("anchor");
            hideDropPointerElement("other");
        }
    };


    var dragOverBlock = function(blockEvent) {
        if (dragging && blockEvent.block.current != null && blockEvent.block.current.canDrag) {
            // find the triggered dropspot
            // dropspot has an "anchor" block and sometimes "other" (when dropping between 2 columns, 2 rows, 2 blocks)
            var dropSpot = blockEvent.block.current.getTriggeredDropspot(blockEvent.direction, blockEvent.pageX, blockEvent.pageY);
            if (lastDropLocation != dropSpot) {
                lastDropLocation = dropSpot;
                // We can not drop on ourselves so skip
                if (lastDropLocation != null) {
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
            // Dragged out of window (drag should've send an ABORT_DRAG event)
//            if (blockEvent.event.pageX > $(document).innerWidth() || blockEvent.event.pageX < 0 || blockEvent.event.pageY > $(document).innerHeight() || blockEvent.event.pageY < 0) {
//                currentDraggedBlock = null;
//                dragging = false;
//            };

            // check for null (e.g during abort_drag)
            if (currentDraggedBlock != null && blockEvent.block.current != null && blockEvent.block.current.canDrag) {
                Logger.debug("Drop block");
                var dropSpot = blockEvent.block.current.getTriggeredDropspot(blockEvent.direction, blockEvent.pageX, blockEvent.pageY);
                // If we did not drop on ourself, change location
                if (!dropSpotIsDraggedBlock(dropSpot, blockEvent)) {
                    Layouter.changeBlockLocation(currentDraggedBlock, dropSpot.anchor, dropSpot.side);
                } else {
                    // do nothing. You can not drop on the block you're dragging
                }
            } else {
                Logger.debug("No drop for block");
            }
            dragAborted();
        }
    };

    var dragAborted = function() {
        if (dragging) {
                $("body").css("cursor", "auto");
                $("html").css("cursor", "auto");
                removeDropPointerElement("anchor");
                removeDropPointerElement("other");
                removeDraggedOverlay();
//                currentDraggedBlock = null;
                draggingEnabled = false;
                dragging = false;
                //Broadcaster.send(new Broadcaster.EVENTS.DISABLE_DRAG(200, "blocks.core.DragDrop", dragEnabled));
        }
    }

    /*
    * checks if the droplocation (or dropspot) equals the block we are dragging
    *
    * */
    var dropSpotIsDraggedBlock = function(dropSpot, blockEvent) {
        var retVal = false;
        // This is an error. Return true to prevent drop
        // TODO: How can this happen
        if (currentDraggedBlock == null || dropSpot == null) return true;
        // dragged block equals anchor or other
        if ((currentDraggedBlock === dropSpot.anchor || currentDraggedBlock === dropSpot.other())) {
            retVal = true
        } else if (dropSpot.anchor != null && dropSpot.anchor.children.length == 1 && dropSpot.anchor.children[0] === currentDraggedBlock) {
            // We drop a
            retVal = true;
        } else if (dropSpot.other() != null && dropSpot.other().children.length == 1 && dropSpot.other().children[0] === currentDraggedBlock) {
            retVal = true;
        }
        return retVal;
    };



    // create drop pointer as element in DOM
    var createDropPointerElement = function (name) {
        var zindex = Overlay.maxIndex() + 1;
        if (dropPointerElements[name] == null) {
            dropPointerElements[name] = $("<div class='blocks-dropspot' />");
            dropPointerElements[name].css("z-index", zindex);
            // TODO position close to blue lin
            // TODO make drop line thicker
            dropPointerElements[name].append(
                $("<div class='droppointer-arrow-container' style='position:absolute;'/>")
                    .append($("<div class='droppointer-arrow' style='position:relative;'></div>"))
            ); // element for arrow
            $("body").append(dropPointerElements[name]);
            dropPointerElements[name].css("position", "absolute");
//            dropPointerElements[name].css("background-color", "rgba(119, 119, 119, 0.5)");
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
            dropPointerElements[name].show();
            if (side != null) {
                dropPointerElements[name].css(cssSide[side], "2px solid rgba(0, 0, 119, 1)");
            }
            showArrowInDroppointerElement(dropPointerElements[name], side);

        } else {
            hideDropPointerElement(name);
            Logger.debug("Hide dropporinter other")
        }
    };

    // show arrow in droppointer overlay
    var showArrowInDroppointerElement= function(droppointer, side) {
        var arrowContainer = $(droppointer.children()[0]);
        var arrow = $(arrowContainer.children()[0]).removeClass();
        var maxSize = 48;
        var minSize = 5;
        var height = droppointer.height() - 5;
        if (height > maxSize) {
            height = maxSize;
        } else if (height < minSize) {
            height = minSize;
        }
        var width = droppointer.width() - 5;
        if (width > maxSize) {
            width = maxSize;
        } else if (width < minSize) {
            width = minSize;
        }

        arrowContainer.css("left", "");
        arrowContainer.css("right", "");
        arrowContainer.css("top", "");
        arrowContainer.css("bottom", "");
        arrow.css("top", "");
        arrow.css("left", "");
        arrow.css("font-size", height + "px");

        Logger.debug("CW: " + arrow.width() + " CH:" + arrow.height());
        if (side == Constants.SIDE.TOP) {
            arrowContainer.css("left", "50%");
            arrowContainer.css("top", "0px");

            arrow.css("left", -(width/2) + "px");
        } else if (side == Constants.SIDE.BOTTOM) {
            arrowContainer.css("left", "50%");
            arrowContainer.css("bottom", "0px");
            arrow.css("left", -(width/2) + "px");
        } else if (side == Constants.SIDE.LEFT) {
            arrowContainer.css("top", "50%");
            arrowContainer.css("left", "0px");
            arrow.css("top", -(height/2) + "px");
        } else if (side == Constants.SIDE.RIGHT) {
            arrowContainer.css("top", "50%");
            arrowContainer.css("right", "0px");
            arrow.css("top", -(height/2) + "px");
        }

        Logger.debug(" arrow height: " + arrow.height() + "  arrow width: " + arrow.width());
        arrow.addClass("glyphicon");
        arrow.addClass(cssArrowClass[side]);
    };


    var hideDropPointerElement = function(name) {
        dropPointerElements[name].hide();
    }

    // Creates an overlay for the block we are dragging to show that we can not drop there
    var createDraggedOverlay = function(surface) {
        if (draggedOverlay == null) {
            draggedOverlay = $("<div class='dragged-block' style=\"position: absolute; left:"+surface.left+"px; top:"+surface.top+"px; height: "+(surface.bottom - surface.top)+"px; width:"+(surface.right-surface.left)+"px; \" />");
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
//    var allowDrag = function(blockEvent) {
//        if (currentDraggedBlockChanged(blockEvent)) {
//            Logger.debug("Try to enable drag for blocks");
//            if (!draggingEnabled && currentDraggedBlock != null && currentDraggedBlock.canLayout)  {
//                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.ENABLE_DRAG(200, "blocks.core.DragDrop", dragEnabled));
//            } else if (draggingEnabled && (currentDraggedBlock == null || !currentDraggedBlock.canLayout)) {
//                Broadcaster.send(new Broadcaster.EVENTS.DISABLE_DRAG(200, "blocks.core.DragDrop", dragEnabled));
//            }
//        }
//    };
//
//    // callback function after request for drag
//    var dragEnabled = function(drag) {
//        draggingEnabled = true
//        $("body").css("cursor", "pointer");
//    };


    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event) {
        dragStarted(event)
    });
    $(document).on(Broadcaster.EVENTS.END_DRAG, function (event) {
        dragEnded(event)
    });
    $(document).on(Broadcaster.EVENTS.ABORT_DRAG, function (event) {
        dragAborted(event);

    });

    $(document).on(Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG, function (event) {
        Logger.debug("dragging not allowed");
        currentDraggedBlock = null;
            draggingEnabled = true;
    });

    $(document).on(Broadcaster.EVENTS.DO_ALLOW_DRAG, function (event) {
        Logger.debug("dragging allowed");
        if (!draggingEnabled) {
            draggingEnabled = false;
        }
    });

    $(document).on(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK, function (event) {
        dragLeaveBlock(event)
    });
    $(document).on(Broadcaster.EVENTS.DRAG_OVER_BLOCK, function (event) {
        dragOverBlock(event)
    });

    $(document).on(Broadcaster.EVENTS.ENABLE_BLOCK_DRAG, function (event) {
        draggingEnabled = true;
        Logger.debug("dragging enabled");
        $("body").css("cursor", "pointer");;
    });

    $(document).on(Broadcaster.EVENTS.DISABLE_BLOCK_DRAG, function (event) {
        Logger.debug("dragging disabled");
        draggingEnabled = false;
        currentDraggedBlock = null;
        $("body").css("cursor", "auto");
    });


}]);




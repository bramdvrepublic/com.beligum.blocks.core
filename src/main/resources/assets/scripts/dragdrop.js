/**
 * Plugin that handles the dragging of blocks
 *
 * While dragging we create 2 dropPointers, "anchor" and "other".
 * Droppointers are Dom elements that overlay the block that takes the drop. (anchor) If we
 * drop between 2 blocks, we also overlay the other block (other)
 * We show arrows in the overlay to indicate the direction the block will move.
 *
 */

base.plugin("blocks.core.DragDrop", ["blocks.core.Broadcaster", "blocks.core.Layouter", "base.core.Constants", "blocks.core.Constants", "blocks.core.Overlay", function (Broadcaster, Layouter, BaseConstants, BlocksConstants, Overlay)
{
    var DragDrop = this;
    var draggingEnabled = false;
    var dragging = false;
    var draggedOverlay = null;
    var dropPointerElements = null;
    var lastDropLocation = null;
    var currentDraggedBlock = null;
    var old_direction = BaseConstants.DIRECTION.NONE;
    /*
    * METHODS CALLED WHILE DRAGGING
    **/

    this.setActive = function(value) {
        draggingEnabled = value;
        currentDraggedBlock = null;
    }

    var hideAll = function(element) {
        if (element.prop("tagName") != "BODY") {
            //var siblings = element.siblings().addClass("not-visible");
            //hideAll(element.parent());
        }
    };

    var showAll = function() {
        $(".not-visible").removeClass("not-visible");
    };

    var insideWindow = function(x, y) {
        if (x < 0 || x > window.innerWidth || y < 0 || y > window.innerHeight) return false; else return true;
    };


    this.dragStarted = function (blockEvent) {
        Logger.debug("drag started");
//        Broadcaster.zoom();
        old_direction = BlocksConstants.DIRECTION.NONE;
        if (blockEvent != null) {
            currentDraggedBlock = Broadcaster.getHooveredBlockForPosition(blockEvent.custom.draggingStart.pageX, blockEvent.custom.draggingStart.pageY).current;
        } else {
            currentDraggedBlock = null;
        }

        createDraggedOverlay(currentDraggedBlock);
        if (draggingEnabled && currentDraggedBlock != null && currentDraggedBlock.canDrag && currentDraggedBlock.getTotalBlocks() > 1) {
            hideAll(Broadcaster.getContainer().element);
            //currentDraggedBlock.getContainer().createAllDropspots();
            Broadcaster.getContainer().createAllDropspots();
            createDropPointerElement();
            //createDropPointerElement("other");
            dragging = true;
            Overlay.removeResizehandles();
            // we have to set both
            // html for undefined area and baody to override default cursor of body.

            $("body").addClass(BlocksConstants.FORCE_DRAG_CURSOR);
        } else if (currentDraggedBlock == null) {
            dragging = true
            Broadcaster.getContainer().createAllDropspots();
            Overlay.removeResizehandles();
            createDropPointerElement();
            $("body").addClass(BlocksConstants.FORCE_DRAG_CURSOR);
        }
    };

    // TODO check necessity canLayout
    this.dragLeaveBlock = function (blockEvent) {
        if (dragging && (blockEvent.block.current == null || !blockEvent.block.current.canDrag)) {
            hideDropPointerElement();
            lastDropLocation = null;

        }
    };



    this.dragEnterBlock = function (blockEvent) {
        if (blockEvent.block.current != null) {
            Logger.debug("Recalculate triggers on enter");
            blockEvent.block.current.recalculateTriggers(old_direction, blockEvent.pageX, blockEvent.pageY, null);
        }
    };

    this.dragOverBlock = function(blockEvent) {
        var dropBlock = blockEvent.block.current;

            //while (dropBlock != null && dropBlock.getContainer() != null && dropBlock.getContainer() != currentDraggedBlock.getContainer()) {
            //    dropBlock = dropBlock.getContainer();
            //    if (dropBlock != null) dropBlock = dropBlock.parent;
            //}


        if (dragging) {
            // find the triggered dropspot
            // dropspot has an "anchor" block and sometimes "other" (when dropping between 2 columns, 2 rows, 2 templates)
            var dropSpot = null;

// && (lastDropLocation == null || lastDropLocation.block == dropBlock)
            if (dropBlock != null && dropBlock.canDrag) {
                var direction = Broadcaster.mouseDirectionForBlock(blockEvent.block.current);
                // when outside container block of last droplocation is null
                if (direction != old_direction && !(lastDropLocation != null && lastDropLocation.block == null)) {
                    Logger.debug("Direction changed " + direction + " from " + old_direction);
                    dropBlock.recalculateTriggers(direction, blockEvent.pageX, blockEvent.pageY, lastDropLocation);
                }
                old_direction = direction;

                dropSpot = dropBlock.getTriggeredDropspot(direction, blockEvent.pageX, blockEvent.pageY);
                if (dropBlock != null && dropSpot == null) {
                    dropBlock.recalculateTriggers(direction, blockEvent.pageX, blockEvent.pageY, null);
                    dropBlock.getTriggeredDropspot(direction, blockEvent.pageX, blockEvent.pageY);
                }

            } else  {
                var container = Broadcaster.getContainer().getLayoutContainer();
                //Logger.debug("dropspot is null of niet drag");
                //Logger.debug(container);
                if (blockEvent.pageY > container.top && blockEvent.pageY < container.bottom) {
                    if (blockEvent.pageX < container.left) {
                        dropSpot = new blocks.elements.Dropspot(BlocksConstants.SIDE.LEFT, container, 0);
                    } else if (blockEvent.pageX > container.right) {
                        dropSpot = new blocks.elements.Dropspot(BlocksConstants.SIDE.RIGHT, container, 0);
                    }
                } else if (blockEvent.pageX > container.left && blockEvent.pageX < container.right) {
                    if (blockEvent.pageY < container.top) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.TOP, container, 0);
                    } else if (blockEvent.pageY > container.bottom) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.BOTTOM, container, 0);
                    }
                }
                if (dropSpot != null) dropSpot.block = container;

            }

            if (lastDropLocation != dropSpot) {
                lastDropLocation = dropSpot;
                // We can not drop on ourselves so skip
                if (lastDropLocation != null)  {
                    //Logger.debug("no null dropspot")
                    if (!dropSpotIsDraggedBlock(lastDropLocation) && insideWindow(blockEvent.clientX, blockEvent.clientY)) {
                        Logger.debug("Drospot changed with min: " + lastDropLocation.min + " - " + lastDropLocation.max);
                        // show overlays for our droplocation(s)
                        drawDropPointerElement(lastDropLocation.anchor, lastDropLocation.side);
                    } else {
                        hideDropPointerElement();
                    }
                } else {
                    hideDropPointerElement();
                }
            } else {
                // Do nothing because nothing changed
                //Logger.debug("dropspot not changed");
                //Logger.debug(lastDropLocation);
            }

        }
    };

    this.dragEnded = function (blockEvent) {
        if (dragging) {

            // check for null (e.g during abort_drag)
            // If we did not drop on ourself, change location
            if (currentDraggedBlock != null && lastDropLocation != null && !dropSpotIsDraggedBlock(lastDropLocation) && insideWindow(blockEvent.clientX, blockEvent.clientY)) {
                Logger.debug("Drop block");
                    Overlay.removeOverlays();
                    resetDragDrop();
                    Layouter.changeBlockLocation(currentDraggedBlock, lastDropLocation.anchor, lastDropLocation.side);

            } else {
                Logger.debug("No drop for block");
                this.dragAborted();
            }

        }
    };

    this.getCurrentDropspot = function() {
        return lastDropLocation;
    };

    var resetDragDrop = function(){
        $("body").removeClass(BlocksConstants.FORCE_DRAG_CURSOR);
        removeDropPointerElement();
        removeDraggedOverlay();
        draggingEnabled = false;
        dragging = false;
        showAll();
    };

    this.dragAborted = function() {
        resetDragDrop();
        Overlay.showResizehandles();
    };

    /*
    * checks if the droplocation (or dropspot) equals the block we are dragging
    *
    * */
    var dropSpotIsDraggedBlock = function(dropSpot) {
        var retVal = false;
        // This is an error. Return true to prevent drop
        // TODO: How can this happen
        if (dropSpot == null) return true;
        if (currentDraggedBlock == null) return false;
        // dragged block equals anchor or other
        if ((currentDraggedBlock === dropSpot.anchor || currentDraggedBlock === dropSpot.other)) {
            retVal = true;
        } else if (dropSpot.anchor != null && dropSpot.anchor.children.length == 1 && dropSpot.anchor.children[0] === currentDraggedBlock) {
            // We drop a
            retVal = true;
        } else if (dropSpot.other != null && dropSpot.other.children.length == 1 && dropSpot.other.children[0] === currentDraggedBlock) {
            retVal = true;
        }
        return retVal;
    };



    // create drop pointer as element in DOM
    var createDropPointerElement = function () {
        Logger.debug("create droppointer ");
        var zindex = BlocksConstants.maxIndex + 3;
        if (dropPointerElements == null) {
            dropPointerElements = $("<div class='templates-dropspot' />");
            dropPointerElements.css("z-index", zindex);
            // TODO position close to blue lin
            // TODO make drop line thicker
            dropPointerElements.append(
                $("<div class='droppointer-arrow-container' style='position:absolute;'/>")
                    .append($("<div class='droppointer-arrow' style='position:relative;'></div>"))
            ); // element for arrow
            $("body").append(dropPointerElements);
            dropPointerElements.css("position", "absolute");
        }
        hideDropPointerElement();
    };

    // remove droppointer as element in dom
    var removeDropPointerElement = function () {
        if (dropPointerElements != null) {
            dropPointerElements.remove();
            dropPointerElements = null;
        }
    };

    // update droppointer in dom
    // name = anchor or other
    var drawDropPointerElement = function (surface, side) {
        if (surface != null) {
            Logger.debug("draw droppointer element")
            dropPointerElements.css("top", surface.top + "px");
            dropPointerElements.css("left", surface.left + "px");
            dropPointerElements.css("width", surface.right - surface.left + "px");
            dropPointerElements.css("height", surface.bottom - surface.top + "px");
            dropPointerElements.css("border", "");
            dropPointerElements.show();
            if (side != null) {
                dropPointerElements.css(cssSide[side], "5px solid rgba(0, 0, 119, 1)");
            }
            //
            //// Scroll element xx pixels into view
            //var SCROLL_THRESHOLD = 100;
            //var currentScrollPosition = $(window).scrollTop();
            //var clientHeight = window.innerHeight;
            //Logger.debug("top: " + currentScrollPosition + " - height client: " + clientHeight);
            //if (side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) {
            //    // element is not completely visible on screen
            //    if (!(surface.top > currentScrollPosition && surface.bottom < currentScrollPosition + clientHeight)) {
            //        var scrollDiff = clientHeight - (surface.bottom - surface.top);
            //        // element fits inside screen
            //        if (scrollDiff > 0) {
            //            // make top visible
            //            if (surface.top < currentScrollPosition) {
            //                scrollDiff = surface.top - SCROLL_THRESHOLD;
            //            } else {
            //                scrollDiff = surface.bottom + SCROLL_THRESHOLD;
            //            }
            //        } else {
            //            scrollDiff = surface.top + Math.floor(scrollDiff / 2);
            //        }
            //        Logger.debug("Scroll: " + scrollDiff);
            //        $("html, body").animate({scrollTop: scrollDiff + "px"}, 300);
            //    }
            //} else if (side == Constants.SIDE.TOP && (surface.top < currentScrollPosition || surface.top > currentScrollPosition + clientHeight)) {
            //    // Top is not visible
            //    Logger.debug("Scroll Top: " + scrollDiff);
            //    $("html, body").animate({scrollTop: (surface.top - SCROLL_THRESHOLD) + "px"}, 300);
            //}  else if (side == Constants.SIDE.BOTTOM && (surface.bottom < currentScrollPosition || surface.bottom > currentScrollPosition + clientHeight)) {
            //    // Top is not visible
            //    Logger.debug("Scroll Bottom: " + scrollDiff);
            //    $("html, body").animate({scrollTop: (surface.bottom + SCROLL_THRESHOLD) + "px"}, 300);
            //}
            //showArrowInDroppointerElement(dropPointerElements[name], side);
        } else {
            hideDropPointerElement();
        }
    };

    // show arrow in droppointer overlay
    var showArrowInDroppointerElement= function(droppointer, side) {
        //var arrowContainer = $(droppointer.children()[0]);
        //var arrow = $(arrowContainer.children()[0]).removeClass();
        //var maxSize = 48;
        //var minSize = 5;
        //var height = droppointer.height() - 5;
        //if (height > maxSize) {
        //    height = maxSize;
        //} else if (height < minSize) {
        //    height = minSize;
        //}
        //var width = droppointer.width() - 5;
        //if (width > maxSize) {
        //    width = maxSize;
        //} else if (width < minSize) {
        //    width = minSize;
        //}
        //
        //arrowContainer.css("left", "");
        //arrowContainer.css("right", "");
        //arrowContainer.css("top", "");
        //arrowContainer.css("bottom", "");
        //arrow.css("top", "");
        //arrow.css("left", "");
        //arrow.css("font-size", height + "px");
        //
        //Logger.debug("CW: " + arrow.width() + " CH:" + arrow.height());
        //if (side == Constants.SIDE.TOP) {
        //    arrowContainer.css("left", "50%");
        //    arrowContainer.css("top", "0px");
        //
        //    arrow.css("left", -(width/2) + "px");
        //} else if (side == Constants.SIDE.BOTTOM) {
        //    arrowContainer.css("left", "50%");
        //    arrowContainer.css("bottom", "0px");
        //    arrow.css("left", -(width/2) + "px");
        //} else if (side == Constants.SIDE.LEFT) {
        //    arrowContainer.css("top", "50%");
        //    arrowContainer.css("left", "0px");
        //    arrow.css("top", -(height/2) + "px");
        //} else if (side == Constants.SIDE.RIGHT) {
        //    arrowContainer.css("top", "50%");
        //    arrowContainer.css("right", "0px");
        //    arrow.css("top", -(height/2) + "px");
        //}
        //
        //Logger.debug(" arrow height: " + arrow.height() + "  arrow width: " + arrow.width());
        //arrow.addClass("glyphicon");
        //arrow.addClass(cssArrowClass[side]);
    };

    function isElementInViewport (el) {

        //special bonus for those using jQuery
        if (typeof jQuery === "function" && el instanceof jQuery) {
            el = el[0];
        }

        var rect = el.getBoundingClientRect();

        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && /*or $(window).height() */
            rect.right <= (window.innerWidth || document.documentElement.clientWidth) /*or $(window).width() */
            );
    }


    var hideDropPointerElement = function() {
        Logger.debug("hide droppointer elemnent")
        if (dropPointerElements != null) dropPointerElements.hide();
    }

    // Creates an overlay for the block we are dragging to show that we can not drop there
    var createDraggedOverlay = function(surface) {
        if (draggedOverlay == null && surface != null) {
            draggedOverlay = $("<div class='" + BlocksConstants.DRAGGED_BLOCK_OVERLAY_CLASS + "' style=\"position: absolute; left:" + surface.left + "px; top:" + surface.top + "px; height: " + (surface.bottom - surface.top) + "px; width:" + (surface.right - surface.left) + "px; \" />");
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
    cssSide[BaseConstants.SIDE.TOP] = "border-top";
    cssSide[BaseConstants.SIDE.BOTTOM] = "border-bottom";
    cssSide[BaseConstants.SIDE.LEFT] = "border-left";
    cssSide[BaseConstants.SIDE.RIGHT] = "border-right";

    // Simple object to translate SIDE in correct glyphicon class for arrow
    // TODO put this in config? but how?
    var cssArrowClass = {};
    cssArrowClass[BaseConstants.SIDE.TOP] = "glyphicon-arrow-up";
    cssArrowClass[BaseConstants.SIDE.BOTTOM] = "glyphicon-arrow-down";
    cssArrowClass[BaseConstants.SIDE.LEFT] = "glyphicon-arrow-left";
    cssArrowClass[BaseConstants.SIDE.RIGHT] = "glyphicon-arrow-right";




}]);




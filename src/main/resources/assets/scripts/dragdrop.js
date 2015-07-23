/**
 *
 * Plugin that handles the dragging of blocks
 *
 * While dragging we create 2 dropPointers, "anchor" and "other".
 * Droppointers are Dom elements that overlay the block that takes the drop. (anchor) If we
 * drop between 2 blocks, we also overlay the other block (other)
 * We show arrows in the overlay to indicate the direction the block will move.
 *
 */

base.plugin("blocks.core.DragDrop", ["blocks.core.Broadcaster", "blocks.core.Layouter", "base.core.Constants", "constants.blocks.common", "blocks.core.Overlay", function (Broadcaster, Layouter, BaseConstants, BlocksConstants, Overlay)
{
    var DragDrop = this;
    var draggingEnabled = false;
    var dragging = false;
    var draggedOverlay = null;
    var dropPointerElements = null;
    var lastDropLocation = null;
    var currentDraggedBlock = null;
    var old_direction = BaseConstants.DIRECTION.NONE;
    var sidebar = null;
    /*
     * METHODS CALLED WHILE DRAGGING
     **/

    this.setActive = function (value)
    {
        draggingEnabled = value;
        currentDraggedBlock = null;
    }

    var hideAll = function (element)
    {
        if (element.prop("tagName") != "BODY") {
            //var siblings = element.siblings().addClass("not-visible");
            //hideAll(element.parent());
        }
    };

    var showAll = function ()
    {
        $(".not-visible").removeClass("not-visible");
    };

    var insideWindow = function (x, y)
    {
        if (x < 0 || x > window.innerWidth || y < 0 || y > window.innerHeight) return false; else return true;
    };


    this.dragStarted = function (blockEvent)
    {
        Logger.debug("drag started");
        sidebar = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS).offset().left;

//        Broadcaster.zoom();
        old_direction = BaseConstants.DIRECTION.NONE;
        if (blockEvent != null) {
            currentDraggedBlock = Broadcaster.getHoveredBlockForPosition(blockEvent.custom.draggingStart.pageX, blockEvent.custom.draggingStart.pageY).current;
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

            $("body").addClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
        } else if (currentDraggedBlock == null) {
            dragging = true
            Broadcaster.getContainer().createAllDropspots();
            Overlay.removeResizehandles();
            createDropPointerElement();
            $("body").addClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
        }
    };

    // TODO check necessity canLayout
    this.dragLeaveBlock = function (blockEvent)
    {
        if (dragging && (blockEvent.block.current == null || !blockEvent.block.current.canDrag)) {
            hideDropPointerElement();
            lastDropLocation = null;

        }
    };


    this.dragEnterBlock = function (blockEvent)
    {
        if (blockEvent.block.current != null) {
            Logger.debug("Recalculate triggers on enter");
            blockEvent.block.current.recalculateTriggers(old_direction, blockEvent.pageX, blockEvent.pageY, null);
        }
    };

    this.dragOverBlock = function (blockEvent)
    {
        var dropBlock = blockEvent.block.current;


        if (dragging) {
            // find the triggered dropspot
            // dropspot has an "anchor" block and sometimes "other" (when dropping between 2 columns, 2 rows, 2 templates)
            var dropSpot = null;

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

            } else if (sidebar == null || sidebar > blockEvent.pageX) {
                var container = Broadcaster.getContainer().getLayoutContainer();

                if (blockEvent.pageY > container.top && blockEvent.pageY < container.bottom) {
                    if (blockEvent.pageX < container.left) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.LEFT, container, 0);
                    } else if (blockEvent.pageX > container.right) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.RIGHT, container, 0);
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
                if (lastDropLocation != null) {
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

    this.dragEnded = function (blockEvent)
    {
        sidebar = null;
        if (dragging) {

            // check for null (e.g during abort_drag)
            // If we did not drop on ourself, change location
            if (currentDraggedBlock != null && lastDropLocation != null && !dropSpotIsDraggedBlock(lastDropLocation) && insideWindow(blockEvent.clientX, blockEvent.clientY)) {
                Logger.debug("Drop block");
                Overlay.removeOverlays();
                resetDragDrop();
                Layouter.changeBlockLocation(currentDraggedBlock, lastDropLocation.anchor, lastDropLocation.side);
            } else if (currentDraggedBlock == null && lastDropLocation != null  && insideWindow(blockEvent.clientX, blockEvent.clientY)) {
                // We added a new block
                Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
                Overlay.removeOverlays();
                // show select box with all blocks
                var box = $("<div />");

                box.load("/blocks/admin/page/blocks", function() {
                    var select = box.find("select");
                    var description = $("<div />");
                    select.change(function() {
                        description.html(select.find(":selected").attr("description"));
                    });
                    box.append(description);
                    var cancelled = true;
                    BootstrapDialog.show({
                        message: function() {return box},
                        buttons: [{
                            label: 'Cancel',
                            action: function(dialogRef) {
                                dialogRef.close();

                            }
                        }, {
                            label: 'OK',
                            cssClass: 'btn-primary',
                            action: function(dialogRef){
                                var name = select.val();
                                var waitingDialog = new BootstrapDialog({
                                    message: "Please wait"
                                });
                                waitingDialog.open();
                                $.getJSON("/blocks/admin/page/block/" + name, function(data) {
                                    // TODO: add waiting dialog
                                    waitingDialog.close();
                                    var block = $(data.html);
                                    Overlay.removeOverlays();
                                    resetDragDrop();
                                    cancelled = false;
                                    Layouter.addNewBlockAtLocation(block, lastDropLocation.anchor, lastDropLocation.side);
                                    dialogRef.close();
                                }, function() {
                                    BootstrapDialog.show({
                                        type: BootstrapDialog.TYPE_DANGER,
                                        message: "An error Occured. Sorry.",
                                        buttons: [{
                                            label: 'Ok',
                                            action: function(dialogRef) {
                                                dialogRef.close();

                                            }
                                        }]
                                    });
                                });
                            }
                        }],
                        onhidden: function() {
                            if (cancelled) {
                                DragDrop.dragAborted();
                                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                            }
                        }
                    });
                });

            } else {
                Logger.debug("No drop for block");
                this.dragAborted();
            }

        }
    };

    this.getCurrentDropspot = function ()
    {
        return lastDropLocation;
    };

    var resetDragDrop = function ()
    {
        $("body").removeClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
        removeDropPointerElement();
        removeDraggedOverlay();
        draggingEnabled = false;
        dragging = false;
        showAll();
    };

    this.dragAborted = function ()
    {
        resetDragDrop();
    };

    /*
     * checks if the droplocation (or dropspot) equals the block we are dragging
     *
     * */
    var dropSpotIsDraggedBlock = function (dropSpot)
    {
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
    var createDropPointerElement = function ()
    {
        Logger.debug("create droppointer ");
        var zindex = base.utils.maxIndex + 3;
        if (dropPointerElements == null) {
            dropPointerElements = $("<div class='blocks-dropspot' />");
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
    var removeDropPointerElement = function ()
    {
        if (dropPointerElements != null) {
            dropPointerElements.remove();
            dropPointerElements = null;
        }
    };

    // update droppointer in dom
    // name = anchor or other
    var drawDropPointerElement = function (surface, side)
    {
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

        } else {
            hideDropPointerElement();
        }
    };

    function isElementInViewport(el)
    {

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


    var hideDropPointerElement = function ()
    {
        Logger.debug("hide droppointer elemnent")
        if (dropPointerElements != null) dropPointerElements.hide();
    }

    // Creates an overlay for the block we are dragging to show that we can not drop there
    var createDraggedOverlay = function (surface)
    {
        if (draggedOverlay == null && surface != null) {
            draggedOverlay = $("<div class='" + BlocksConstants.DRAGGED_BLOCK_OVERLAY_CLASS + "' style=\"position: absolute; left:" + surface.left + "px; top:" + surface.top + "px; height: " + (surface.bottom - surface.top) + "px; width:" + (surface.right - surface.left) + "px; \" />");
            $("body").append(draggedOverlay);
        }
    };

    var removeDraggedOverlay = function ()
    {
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




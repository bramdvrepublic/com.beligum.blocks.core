/**
 *
 * Plugin that handles the dragging of blocks
 *
 * While dragging we create 2 dropPointers, "anchor" and "other".
 * Droppointers are Dom elements that overlay the block that takes the drop. (anchor) If we
 * drop between 2 blocks, we also overlay the other block (other)
 * We show arrows in the overlay to indicate the direction the block will move.
 */
base.plugin("blocks.core.DragDrop", ["blocks.core.Broadcaster", "blocks.core.Layouter", "base.core.Constants", "constants.blocks.core", "blocks.core.Overlay", "messages.blocks.core", "blocks.core.Notification", "blocks.core.Mouse", function (Broadcaster, Layouter, BaseConstants, BlocksConstants, Overlay, BlocksMessages, Notification, Mouse)
{
    var DragDrop = this;
    var draggingEnabled = false;
    var dragging = false;
    var draggedOverlay = null;
    var dropPointerElements = null;
    var lastDropLocation = null;
    var currentDraggedBlock = null;
    var sidebar = null;
    var oldDirection = BaseConstants.DIRECTION.NONE;

    // Simple object to translate SIDE in css border side
    var cssSide = {};
    cssSide[BaseConstants.SIDE.TOP] = "top";
    cssSide[BaseConstants.SIDE.BOTTOM] = "bottom";
    cssSide[BaseConstants.SIDE.LEFT] = "left";
    cssSide[BaseConstants.SIDE.RIGHT] = "right";

    var cssBorderSide = {};
    cssBorderSide[BaseConstants.SIDE.TOP] = "border-" + cssSide[BaseConstants.SIDE.TOP];
    cssBorderSide[BaseConstants.SIDE.BOTTOM] = "border-" + cssSide[BaseConstants.SIDE.BOTTOM];
    cssBorderSide[BaseConstants.SIDE.LEFT] = "border-" + cssSide[BaseConstants.SIDE.LEFT];
    cssBorderSide[BaseConstants.SIDE.RIGHT] = "border-" + cssSide[BaseConstants.SIDE.RIGHT];

    // Simple object to translate SIDE in correct glyphicon class for arrow
    // TODO put this in config? but how?
    var cssArrowClass = {};
    cssArrowClass[BaseConstants.SIDE.TOP] = "glyphicon-arrow-up";
    cssArrowClass[BaseConstants.SIDE.BOTTOM] = "glyphicon-arrow-down";
    cssArrowClass[BaseConstants.SIDE.LEFT] = "glyphicon-arrow-left";
    cssArrowClass[BaseConstants.SIDE.RIGHT] = "glyphicon-arrow-right";

    /*
     * METHODS CALLED WHILE DRAGGING
     **/

    this.setActive = function (value)
    {
        draggingEnabled = value;
        currentDraggedBlock = null;
    };

    this.isDragging = function()
    {
        return draggingEnabled && dragging;
    };

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
        if (x < 0 || x > window.innerWidth || y < 0 || y > window.innerHeight) {
            return false;
        } else {
            return true;
        }
    };

    this.dragStarted = function (blockEvent, data)
    {
        sidebar = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS).offset().left;

//        Broadcaster.zoom();
        oldDirection = BaseConstants.DIRECTION.NONE;
        if (blockEvent != null) {
            //currentDraggedBlock = Broadcaster.getHoveredBlockForPosition(data.draggingStart.pageX, data.draggingStart.pageY).current;
            currentDraggedBlock = Broadcaster.getHoveredBlock().current;
        } else {
            currentDraggedBlock = null;
        }

        createDraggedOverlay(currentDraggedBlock);
        if (draggingEnabled && currentDraggedBlock != null && currentDraggedBlock.canDrag && currentDraggedBlock.getTotalBlocks() > 1) {
            hideAll(Broadcaster.getContainer().element);
            //currentDraggedBlock.getContainer().createAllDropspots();
            Broadcaster.getContainer().createAllDropspots();
            createDropPointerElement();
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

    this.dragEnterBlock = function (blockEvent)
    {
        if (blockEvent.block.current != null) {
            Logger.debug("Recalculate triggers on enter");
            blockEvent.block.current.recalculateTriggers(oldDirection, blockEvent.originalEvent.pageX, blockEvent.originalEvent.pageY, null);
        }
    };

    // TODO check necessity canLayout
    this.dragLeaveBlock = function (blockEvent)
    {
        if (blockEvent.block.current == null || !blockEvent.block.current.canDrag) {
            hideDropPointerElement();
            lastDropLocation = null;
        }
    };

    this.dragOverBlock = function (blockEvent)
    {
        var pageX = blockEvent.originalEvent.pageX;
        var pageY = blockEvent.originalEvent.pageY;
        var dropBlock = blockEvent.block.current;

        if (dragging) {

            // find the triggered dropspot
            // dropspot has an "anchor" block and sometimes "other" (when dropping between 2 columns, 2 rows, 2 templates)
            var dropSpot = null;

            if (dropBlock != null && dropBlock.canDrag) {
                var direction = Mouse.directionForBlock(blockEvent.block.current);

                // when outside container block of last droplocation is null
                if (direction != oldDirection && !(lastDropLocation != null && lastDropLocation.block == null)) {
                    Logger.debug("Direction changed " + direction + " from " + oldDirection);
                    dropBlock.recalculateTriggers(direction, pageX, pageY, lastDropLocation);
                }
                oldDirection = direction;

                dropSpot = dropBlock.getTriggeredDropspot(direction, pageX, pageY);
                if (dropSpot == null && dropBlock != null) {
                    dropBlock.recalculateTriggers(direction, pageX, pageY, null);
                    dropSpot = dropBlock.getTriggeredDropspot(direction, pageX, pageY);
                }
            }
            else if (sidebar == null || sidebar > pageX) {
                var container = Broadcaster.getContainer().getLayoutContainer();

                if (blockEvent.pageY > container.top && blockEvent.pageY < container.bottom) {
                    if (pageX < container.left) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.LEFT, container, 0);
                    } else if (pageX > container.right) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.RIGHT, container, 0);
                    }
                } else if (pageX > container.left && pageX < container.right) {
                    if (blockEvent.pageY < container.top) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.TOP, container, 0);
                    } else if (blockEvent.pageY > container.bottom) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstants.SIDE.BOTTOM, container, 0);
                    }
                }
                if (dropSpot != null) {
                    dropSpot.block = container;
                }
            }
            else {
                Logger.debug("Nope, block is not draggable");
            }

            if (lastDropLocation != dropSpot) {
                lastDropLocation = dropSpot;
                // We can not drop on ourselves so skip
                if (lastDropLocation != null) {
                    if (!dropSpotIsDraggedBlock(lastDropLocation) && insideWindow(blockEvent.clientX, blockEvent.clientY)) {
                        Logger.debug("Dropspot changed with min: " + lastDropLocation.min + " - " + lastDropLocation.max);
                        // show overlays for our droplocation(s)
                        drawDropPointerElement(lastDropLocation.anchor, lastDropLocation.side);
                    }
                    else {
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
            } else if (currentDraggedBlock == null && lastDropLocation != null && insideWindow(blockEvent.clientX, blockEvent.clientY)) {
                // We added a new block
                Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, blockEvent);
                Overlay.removeOverlays();
                // show normal cursor during dialog
                $("body").removeClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
                // Remove all pointer elements
                removeDropPointerElement();
                removeDraggedOverlay();
                // show select box with all blocks
                var box = $("<div />");

                var boxDialog;
                box.load("/blocks/admin/page/blocks", function ()
                {
                    box.find("a").click(function (event)
                    {
                        var name = $(this).attr("data-value");

                        //seems fast enough...
                        var waitingDialog;
                        //var waitingDialog = new BootstrapDialog({
                        //    message: "Please wait"
                        //});

                        boxDialog.close();
                        if (waitingDialog) {
                            waitingDialog.open();
                        }
                        $.getJSON("/blocks/admin/page/block/" + name)
                            .done(function (data)
                            {
                                addHeadResource(data.inlineStyles, name+"-in-style");
                                addHeadResource(data.externalStyles, name+"-ex-style");
                                addHeadResource(data.inlineScripts, name+"-in-script", true);
                                addHeadResource(data.externalScripts, name+"-ex-script", true);

                                var block = $(data.html);
                                Overlay.removeOverlays();
                                resetDragDrop();
                                cancelled = false;
                                Layouter.addNewBlockAtLocation(block, lastDropLocation.anchor, lastDropLocation.side);
                            })
                            .fail(function (xhr, textStatus, exception)
                            {
                                Notification.error(BlocksMessages.savePageError + (exception ? "; " + exception : ""), xhr);
                            })
                            .always(function ()
                            {
                                if (waitingDialog) {
                                    waitingDialog.close();
                                }
                            });
                    });
                });

                var cancelled = true;
                boxDialog = BootstrapDialog.show({
                    cssClass: BlocksConstants.NEW_BLOCK_MODAL_CLASS,
                    message: function ()
                    {
                        return box
                    },
                    buttons: [],
                    onhidden: function ()
                    {
                        if (cancelled) {
                            DragDrop.dragAborted();
                            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, blockEvent);
                        }
                    }
                });

            } else {
                Logger.debug("No drop for block");
                this.dragAborted();
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, blockEvent);
            }

        }
    };

    var addHeadResource = function(resourceArray, className, isScript)
    {
        if (resourceArray != null && resourceArray.length>0) {
            //remove existing ones
            $("head ."+className).remove();
            for (var i=0;i<resourceArray.length;i++) {
                var newEl = $(resourceArray[i]);
                newEl.addClass(className);

                var srcAttr = newEl.attr("src");
                $("head").append(newEl);

                if (isScript && srcAttr) {
                    $.getScript(srcAttr)
                        .done(function(script, textStatus) {
                            //this is needed to auto-wire the plugins (was a quick fix, hope it's ok)
                            base.run();
                        })
                        .fail(function (xhr, textStatus, exception) {
                            Notification.error(BlocksMessages.savePageError + (exception ? "; " + exception : ""), xhr);
                        });
                }
            }
        }
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
        var zindex = base.utils.maxZIndex + 3;
        if (dropPointerElements == null) {
            dropPointerElements = $("<div class='" + BlocksConstants.BLOCKS_DROPSPOT_CLASS + "' />");
            dropPointerElements.css("z-index", zindex);
            dropPointerElements.css("position", "absolute");
            // TODO position close to blue lin
            // TODO make drop line thicker
            //dropPointerElements.append(
            //    $("<div class='droppointer-arrow-container' style='position:absolute;'/>")
            //        .append($("<div class='droppointer-arrow' style='position:relative;'></div>"))
            //); // element for arrow
            $("body").append(dropPointerElements);
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
            var top = surface.top;
            var left = surface.left;
            var width = surface.right - surface.left;
            var height = surface.bottom - surface.top;

            // Instead of showing the drop surface with the border of a bounding box, we'll just draw the border
            // as a box itself, so we can use the background css to style it
            var offset = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH / 2.0;
            if (side == BaseConstants.SIDE.TOP) {
                top = surface.top - offset;
                height = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }
            else if (side == BaseConstants.SIDE.RIGHT) {
                left = surface.right - offset;
                width = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }
            else if (side == BaseConstants.SIDE.BOTTOM) {
                top = surface.bottom - offset;
                height = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }
            else if (side == BaseConstants.SIDE.LEFT) {
                left = surface.left - offset;
                width = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }

            dropPointerElements.css("top", top + "px");
            dropPointerElements.css("left", left + "px");
            dropPointerElements.css("width", width + "px");
            dropPointerElements.css("height", height + "px");
            for (var i = 1; i < 5; i++) {
                dropPointerElements.removeClass(BlocksConstants.BLOCKS_DROPSPOT_CLASS + "-" + cssSide[i]);
            }
            dropPointerElements.addClass(BlocksConstants.BLOCKS_DROPSPOT_CLASS + "-" + cssSide[side]);

            dropPointerElements.show();

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
        if (dropPointerElements != null) {
            dropPointerElements.hide();
        }
    };

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

}]);




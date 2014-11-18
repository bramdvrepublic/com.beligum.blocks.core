/*
* Resize handles are the handles to resize columns
* A row contains all the possible resize handles
*
* this plugin shows the resizehandle when hoovering over it and allows for resizing columns
* by dragging the handles.
*
* initDrag: here we initialize the triggers to resize the columns while dragging a handle
* checkDrag: checks to resize a column while dragging
*
* this plugin does not send events of it's own
*
* */

blocks.plugin("blocks.core.Resizer", ["blocks.core.Elements", "blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.DomManipulation", function (Elements, Broadcaster, Constants, DOM) {
    var active = false;
    var draggingEnabled = false;
    var dragging = false;
    var dragColumns;
    var currentDragColumn;
    var resizeHandleElement;
    var activeResizeHandle;
    var minColumn;
    var maxColumn;


    var activate = function() {
        active = true;
        dragColumns = [];
        currentDragColumn = 0;
        activeResizeHandle = null;
        minColumn = null;
        maxColumn = null;
        // check if a handle exists in the dom and remove it before setting to null
        removeHandleElement();
    };


    var deactivate = function() {
        active = false;
    };


    // Check if a handle exists in the dom and if not, add 1
    var checkIfHandleElementExists = function () {
        if (resizeHandleElement == null) {
            // TODO set handle class in config
            $(".column-resize-handle").remove();
            resizeHandleElement = $("<div class='.column-resize-handle' />")
            $("body").append(resizeHandleElement);
        }
    };

    // show the handle at the correct position
    var showHandleElement = function (surface) {
        checkIfHandleElementExists();
        resizeHandleElement.css('position', 'absolute');
        resizeHandleElement.css('background-color', 'black');
        resizeHandleElement.css("left", surface.left + "px");
        resizeHandleElement.css("top", surface.top + "px");
        resizeHandleElement.css("width", surface.right - surface.left + "px");
        resizeHandleElement.css("height", surface.bottom - surface.top + "px");
        $("body").css("cursor", "col-resize");
    };

    // update the position of the handle element in the dom
    var moveHandleElement = function () {
        checkIfHandleElementExists();
        activeResizeHandle.calculateSurface();
        resizeHandleElement.css('left', activeResizeHandle.drawSurface.left);
    };

    // remove the handle from the dom
    var removeHandleElement = function () {
        // TODO set drag priority in config
        if (resizeHandleElement != null) {
            resizeHandleElement.remove();
            resizeHandleElement = null;
            $("body").css("cursor", 'auto');
        }
    };

    /*
    * return the current resizehandle that we are hoovering over
    * */
    var findActiveResizeHandle = function(blockEvent) {
        var retVal = null;
        if (activeResizeHandle != null) {
            if (activeResizeHandle.top <=  blockEvent.event.pageY && activeResizeHandle.bottom >= blockEvent.event.pageY &&
                activeResizeHandle.left <= blockEvent.event.pageX && activeResizeHandle.right >= blockEvent.event.pageX) {
                retVal =  activeResizeHandle;
            }
        } else if (blockEvent.block.current != null) {
            // find the first parent that is a row (but not a block)
            var activeRow = null;
            if (blockEvent.block.current.parent != null) {
                activeRow = blockEvent.block.current.parent.parent
            }

            if (activeRow != null && activeRow instanceof Elements.Row) {
                retVal = activeRow.findTriggeredResizeHandle(blockEvent.event.pageX, blockEvent.event.pageY, Elements.ResizeHandle);
            }
        }
        return retVal;
    };

    var activeResizehandleChanged = function (blocksEvent) {
        var retVal = false;
        var newResizeHandle = findActiveResizeHandle(blocksEvent);
        if (activeResizeHandle != newResizeHandle) {
            if (activeResizeHandle != null) {
                removeHandleElement();
            }
            activeResizeHandle = newResizeHandle;
            retVal = true;
        }
        return retVal;
    }
    /*
    * Checks if the mouse hoovers over a handle and allows or disallows dragging
    * */
    var manageActiveResizeHandle = function (blocksEvent) {
        if (activeResizehandleChanged(blocksEvent)) {
            if (activeResizeHandle != null && !draggingEnabled) {
                Broadcaster.send(new Broadcaster.EVENTS.ENABLE_DRAG(1000, "blocks.core.Resizer", dragEnabled));
            }  else if (activeResizeHandle == null && draggingEnabled) {
                Broadcaster.send(new Broadcaster.EVENTS.DISABLE_DRAG(1000, "blocks.core.Resizer"));
                draggingEnabled = false;
            }
        }
    };

    var dragEnabled = function() {
        if (!draggingEnabled) {
            draggingEnabled = true;
            showHandleElement(activeResizeHandle.drawSurface);
        }
    }


    /*
    * bind to jQuery mousemove event
    * show dragHandle
    * init triggerpoints to resize our column (initdrag)
    * */
    var startDrag = function (blocksEvent) {
        if (active && draggingEnabled) {
            $(document).on("mousemove.resizehandledrag", function (event) {
                doDrag(event)
            });
            showHandleElement(activeResizeHandle.drawSurface);
            initDrag(activeResizeHandle);
            dragging = true;
        }
    };

    /*
    * unbind from jQuery event
    * remove handle
    * send DOM_DID_CHANGE EVENT
    * */
    var endDrag = function (blocksEvent) {
        if (active && dragging) {
            dragging = false;
            $(document).off("mousemove.resizehandledrag");
            removeHandleElement();
            $('body').css("cursor", 'auto')
            Broadcaster.send(new Broadcaster.EVENTS.DOM_DID_CHANGE());
        }
    };

    var doDrag = function (event) {
        if (dragging) {
            checkDrag(event);
        }
    };

    /*
    * Before dragging calculate all the possible columns
    *
    * */
    var initDrag = function (resizeHandle) {
        activeResizeHandle = resizeHandle;
        if (resizeHandle.leftColumn.parent == resizeHandle.rightColumn.parent) {
            // for the parent row of the calculate offset left and offset right
            // offset = nr of columns
            var row = resizeHandle.leftColumn.parent;
            var columns = row.children;
            var offsetLeft = 0;
            for (var i = 0; i < columns.length; i++) {
                if (columns[i] !== resizeHandle.leftColumn) {
                    offsetLeft += DOM.getColumnWidth(columns[i].element);
                } else {
                    break;
                }
            }
            var offsetRight = offsetLeft + DOM.getColumnWidth(resizeHandle.leftColumn.element) + DOM.getColumnWidth(resizeHandle.rightColumn.element);
            // width of currentColumn
            var currentColumn = offsetLeft + DOM.getColumnWidth(resizeHandle.leftColumn.element);

            // min and max column that we can drag to
            // we can drag until the outer (left or right) column is 1 unit wide
            var min = offsetLeft + 1;
            var max = offsetRight - 1;
            // column width in pixels
            var colWidth = (row.getFullWidth()) / 12;
            // dragColumns are the trigger zones when we jump to the next column
            // trigger zone is half a column left and right from start of a column
            if (max > min) {
                for (var i = min; i <= max; i++) {
                    dragColumns[i] = {
                        start: row.left + (i * colWidth) - (colWidth / 2),
                        end: row.left + (i * colWidth) + (colWidth / 2),
                        middle: row.left + (i * colWidth),
                        index: i
                    };
                }
            }
            currentDragColumn = currentColumn;
            minColumn = min;
            maxColumn = max;
        } else {  // left and right column of resizehandle have different parents !!!
            // strange things happening, should never happen
            endDrag();
        }
    };

    /*
    * While dragging check if we are in a new dragColumn
    * if so resize our column
    *
    * */
    var checkDrag = function (event) {
        var curCol = dragColumns[currentDragColumn];
        if (event.pageX > curCol.start && event.pageX < curCol.end) {
            Logger.debug("No column change");
            return;
        }

        for (var i=minColumn; i <= maxColumn; i++ ) {
            var diff = i - currentDragColumn;
            if (event.pageX > dragColumns[currentDragColumn + diff].start &&
                event.pageX < dragColumns[currentDragColumn + diff].end) {
                currentDragColumn = currentDragColumn + diff;
                DOM.setColumnWidth(activeResizeHandle.leftColumn.element, DOM.getColumnWidth(activeResizeHandle.leftColumn.element) + diff);
                DOM.setColumnWidth(activeResizeHandle.rightColumn.element, DOM.getColumnWidth(activeResizeHandle.rightColumn.element) - diff);
                // move resizehandle
                moveHandleElement();
                break;
            }
        }
    };


    Broadcaster.on(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK, "blocks.core.Resizer", function (event) {
        manageActiveResizeHandle(event.blockEvent)
    });
    Broadcaster.on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, "blocks.core.Resizer", function (event) {
        manageActiveResizeHandle(event.blockEvent)
    });
    Broadcaster.on(Broadcaster.EVENTS.HOOVER_OVER_BLOCK, "blocks.core.Resizer", function (event) {
        manageActiveResizeHandle(event.blockEvent)
    });

    Broadcaster.on(Broadcaster.EVENTS.DO_ALLOW_DRAG, "blocks.core.Resizer", function () {
        activate();
    });
    Broadcaster.on(Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG, "blocks.core.Resizer", function () {
        deactivate();
    });

    // effective dragging
    Broadcaster.on(Broadcaster.EVENTS.START_DRAG, "blocks.core.Resizer", function (event) {
        startDrag(event.blockEvent)
    });
    Broadcaster.on(Broadcaster.EVENTS.END_DRAG, "blocks.core.Resizer", function (event) {
        endDrag(event.blockEvent)
    });
    Broadcaster.on(Broadcaster.EVENTS.ABORT_DRAG, "blocks.core.Resizer", function (event) {
        endDrag(event.blockEvent)
    });

    Broadcaster.on(Broadcaster.EVENTS.DRAG_DISABLED, "blocks.core.Resizer", function (event) {
            draggingEnabled = false;
    });

    // On boot
    activate();

}]);


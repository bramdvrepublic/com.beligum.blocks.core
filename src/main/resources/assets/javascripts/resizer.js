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

blocks.plugin("blocks.core.Resizer", ["blocks.core.Elements", "blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.DomManipulation", "blocks.core.Overlay", function (Elements, Broadcaster, Constants, DOM, Overlay) {
    var Resizer = this;
    var active = false;
    var draggingEnabled = false;
    var dragging = false;
    var dragColumns;
    var currentDragColumn;
    var resizeHandleElement;
    var activeResizeHandle;
    var minColumn;
    var maxColumn;


    this.activate = function(value) {
        active = value;
        dragColumns = [];
        currentDragColumn = 0;
        activeResizeHandle = null;
        minColumn = null;
        maxColumn = null;

    };



    // Check if a handle exists in the dom and if not, add 1
    var checkIfHandleElementExists = function () {
        if (resizeHandleElement == null) {
            $("." + Constants.COLUMN_RESIZER_CLASS).remove();
            resizeHandleElement = $("<div/>").addClass(Constants.COLUMN_RESIZER_CLASS);
            resizeHandleElement.css("z-index", (Overlay.maxIndex() + 1));
            $("html").append(resizeHandleElement);
        }
    };

    // show the handle at the correct position
    var showHandleElement = function (surface) {

    };

    // update the position of the handle element in the dom
    var moveHandleElement = function () {
//        checkIfHandleElementExists();
//        activeResizeHandle.updateSurface();
//        resizeHandleElement.css('left', activeResizeHandle.drawSurface.left);
    };

    // remove the handle from the dom
    var removeHandleElement = function () {
        if (resizeHandleElement != null) {
            $("." + Constants.COLUMN_RESIZER_CLASS).remove();
            resizeHandleElement = null;
        }
    };

    /*
     * return the current resizehandle that we are hoovering over
     * */
    var findActiveResizeHandle = function(blockEvent) {
        var retVal = null;
        if (activeResizeHandle != null) {
            if (activeResizeHandle.top <=  blockEvent.pageY && activeResizeHandle.bottom >= blockEvent.pageY &&
                activeResizeHandle.left <= blockEvent.pageX && activeResizeHandle.right >= blockEvent.pageX) {
                retVal =  activeResizeHandle;
            }
        } else if (blockEvent.block.current != null) {
            // find the first parent that is a row (but not a block)
            var activeRow = blockEvent.block.current.parent;
            while (!(activeRow instanceof blocks.elements.Row || activeRow instanceof blocks.elements.Container ||activeRow == null)) {
                activeRow = activeRow.parent
            }

            if (activeRow != null && activeRow instanceof blocks.elements.Row) {
                retVal = activeRow.findTriggeredResizeHandle(blockEvent.pageX, blockEvent.pageY, blocks.elements.ResizeHandle);
            }
        }
        return retVal;
    };


    var activeRowElement = null;

    var setCursor = function(value) {
        if (value && activeRowElement != null) {
            activeRowElement.addClass(Constants.RESIZING_CLASS);
        } else if (!value && activeRowElement != null) {
            activeRowElement.removeClass(Constants.RESIZING_CLASS);
            activeRowElement = null;
        }
    };

    var activeResizehandleChanged = function (blocksEvent) {
        var retVal = false;
        var newResizeHandle = findActiveResizeHandle(blocksEvent);
        if (activeResizeHandle != newResizeHandle) {
            if (activeResizeHandle != null) {
                removeHandleElement();
                setCursor(false);
            }

            activeResizeHandle = newResizeHandle;

            retVal = true;
        }
        return retVal;
    }
    /*
     * Checks if the mouse hoovers over a handle and allows or disallows dragging
     * */
    this.manageActiveResizeHandle = function (blocksEvent) {
        if (activeResizehandleChanged(blocksEvent)) {
            if (activeResizeHandle != null && !draggingEnabled) {
                Broadcaster.send(Broadcaster.EVENTS.DISABLE_BLOCK_DRAG);
                draggingEnabled = true;
                var col = activeResizeHandle.leftColumn || activeResizeHandle.rightColumn;

                setCursor(false);
                activeRowElement = col.parent.element;
                setCursor(true);
                showHandleElement(activeResizeHandle.drawSurface);
            }  else if (activeResizeHandle == null && draggingEnabled) {
                Broadcaster.send(Broadcaster.EVENTS.ENABLE_BLOCK_DRAG);
                draggingEnabled = false;
            }
        }
    };


    /*
     * bind to jQuery mousemove event
     * show dragHandle
     * init triggerpoints to resize our column (initdrag)
     * */
    this.startDrag = function (blocksEvent) {
        if (active && draggingEnabled) {
            $(document).on("mousemove.resizehandledrag", function (event) {
                doDrag(event)
            });
            showHandleElement(activeResizeHandle.drawSurface);
            initDrag(activeResizeHandle);
            activeRowElement.find("*").addClass(Constants.RESIZING_CLASS);
            dragging = true;
        }
    };

    /*
     * unbind from jQuery event
     * remove handle
     * send DOM_DID_CHANGE EVENT
     * */
    this.endDrag = function (blocksEvent) {
        if (active && dragging) {
            dragging = false;
            $(document).off("mousemove.resizehandledrag");
            removeHandleElement();
            activeRowElement.find("*").removeClass(Constants.RESIZING_CLASS);
            Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
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
        var row;
        if (resizeHandle.leftColumn != null) {
            row = resizeHandle.leftColumn.parent;
        } else if (resizeHandle.rightColumn != null) {
            row = resizeHandle.rightColumn.parent;
        } else {
            // left and right column of resizehandle have different parents !!!
            // strange things happening, should never happen
            endDrag();
            return;
        }

        // for the parent row of the column, calculate offset left and offset right
        // offset = nr of columns
        var columns = row.children;
        var offsetLeft = 0;
        var widthLeftColumn = 0;
        var widthRightColumn = 0;

        for (var i=0; i < columns.length; i++) {
            if (columns[i] == resizeHandle.leftColumn) {
                widthLeftColumn = DOM.getColumnWidth(columns[i].element);
                if (resizeHandle.rightColumn != null) {
                    widthRightColumn = DOM.getColumnWidth(columns[i + 1].element);
                }
                break;
            } else if (resizeHandle.leftColumn == null && columns[i] != resizeHandle.rightColumn) {
                widthRightColumn = DOM.getColumnWidth(columns[i].element);
                break;
            } else if (resizeHandle.leftColumn != null) {
                offsetLeft += DOM.getColumnWidth(columns[i].element);
            }
        }

        // current location of the resizehandle
        var currentPosition = offsetLeft + widthLeftColumn;
        var offsetRight = currentPosition + widthRightColumn;




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
        currentDragColumn = currentPosition;
        minColumn = min;
        maxColumn = max;

    };

    /*
     * While dragging check if we are in a new dragColumn
     * if so resize our column
     *
     * */
    var checkDrag = function (event) {
        var curCol = dragColumns[currentDragColumn];
        if (curCol != null && (event.pageX > curCol.start && event.pageX < curCol.end) ) {
            Logger.debug("No column change");
            return;
        }

        for (var i=minColumn; i <= maxColumn; i++ ) {
            var diff = i - currentDragColumn;
            if (event.pageX > dragColumns[currentDragColumn + diff].start &&
                event.pageX < dragColumns[currentDragColumn + diff].end) {
                currentDragColumn = currentDragColumn + diff;
                if (activeResizeHandle.leftColumn == null) {
                    var element = $('<div class="col-md-1"><div></div></div>');
                    activeResizeHandle.rightColumn.element.before(element);
                    activeResizeHandle.leftColumn = new blocks.elements.Column(0, 0, 0, 0, element, null, 0);
                    DOM.setColumnWidth(activeResizeHandle.rightColumn.element, DOM.getColumnWidth(activeResizeHandle.rightColumn.element) - diff);

                } else if (activeResizeHandle.rightColumn == null) {

                    activeResizeHandle.rightColumn = {};
                    activeResizeHandle.rightColumn.element = $('<div class="col-md-1"><div></div></div>');
                    DOM.setColumnWidth(activeResizeHandle.leftColumn.element, DOM.getColumnWidth(activeResizeHandle.leftColumn.element) + diff);
                    activeResizeHandle.leftColumn.element.after(activeResizeHandle.rightColumn.element);
                } else {
                    DOM.setColumnWidth(activeResizeHandle.leftColumn.element, DOM.getColumnWidth(activeResizeHandle.leftColumn.element) + diff);
                    DOM.setColumnWidth(activeResizeHandle.rightColumn.element, DOM.getColumnWidth(activeResizeHandle.rightColumn.element) - diff);

                }
                // move resizehandle
                moveHandleElement();
                break;
            }
        }
    };



    // On boot

}]);


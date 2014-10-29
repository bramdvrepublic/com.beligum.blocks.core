blocks.plugin("blocks.core.Resizer", ["blocks.core.Elements", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DomManipulation", function (Elements, Broadcaster, Mouse, DOM) {


    var checkIfHandleExists = function () {
        if (resizeHandleElement == null) {
            $(".column-resize-handle").remove();
            resizeHandleElement = $("<div class='.column-resize-handle' />")
            $("body").append(resizeHandleElement);
        }
    };

    var showHandle = function (surface) {
        checkIfHandleExists();
        resizeHandleElement.css('position', 'absolute');
        resizeHandleElement.css('background-color', 'black');
        resizeHandleElement.css("left", surface.left + "px");
        resizeHandleElement.css("top", surface.top + "px");
        resizeHandleElement.css("width", surface.right - surface.left + "px");
        resizeHandleElement.css("height", surface.bottom - surface.top + "px");
        $("body").css("cursor", "col-resize");
    };

    var moveHandle = function () {
        checkIfHandleExists();
        activeResizeHandle.calculateSurface();
        resizeHandleElement.css('left', activeResizeHandle.drawSurface.left);
    };

    var removeHandle = function () {
        Broadcaster.send(Mouse.config.EVENT.CAN_NOT_START_DRAG, {surface: activeResizeHandle, priority: 100});
        activeResizeHandle = null;
        if (resizeHandleElement != null) {
            resizeHandleElement.remove();
            resizeHandleElement = null;
            $("body").css("cursor", 'auto');
        }

    };

    var manageResizeHandle = function (blocksEvent) {
        if (blocksEvent.block.current != null) {
            var newResizeHandle = getActiveResizeHandle(blocksEvent);
            if (activeResizeHandle != newResizeHandle && newResizeHandle != null) {
                if (activeResizeHandle != null) {
                    removeHandle();
                }
                activeResizeHandle = newResizeHandle;
                showHandle(activeResizeHandle.drawSurface);
                Broadcaster.send(Mouse.config.EVENT.CAN_START_DRAG, {surface: activeResizeHandle, priority: 100});
            } else if (activeResizeHandle != newResizeHandle && newResizeHandle == null) {
                removeHandle();
            }
        } else {
            if (activeResizeHandle != null) {
                removeHandle();
            }
        }
    };


    var startDrag = function (blocksEvent) {
        if (blocksEvent.drag.surface instanceof Elements.ResizeHandle) {
            $(document).on("mousemove.resizehandledrag", function (event) {
                doDrag(event)
            });
            showHandle(blocksEvent.drag.surface.drawSurface);
            initDrag(blocksEvent.drag.surface);
        }
    };

    var endDrag = function (blocksEvent) {
        if (blocksEvent.drag.surface === activeResizeHandle) {
            $(document).off("mousemove.resizehandledrag");
            removeHandle();
            $('body').css("cursor", 'auto')
            Broadcaster.send(Mouse.config.EVENT.REFRESH_LAYOUT);
        }
    };

    var doDrag = function (event) {
        checkDrag(event);
    };

    var initDrag = function (resizeHandle) {
        activeResizeHandle = resizeHandle;
        if (resizeHandle.leftColumn.parent == resizeHandle.rightColumn.parent) {

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
            var currentColumn = offsetLeft + DOM.getColumnWidth(resizeHandle.leftColumn.element);
            var min = offsetLeft + 1;
            var max = offsetRight - 1;
            var colWidth = (row.getFullWidth()) / 12;
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
        }
    };

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
                moveHandle();
                break;
            }
        }
    };

    var getActiveResizeHandle = function(blockEvent) {
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
                retVal = activeRow.findTriggeredResizeHandle(blockEvent.event, Elements.ResizeHandle);
            }
        }
        return retVal;
    };



    /*
     *  When hoovering a block check if active resizehandle -> CAN_START_DRAG
     *  Onstart drag check active resizehandle -> if ok then we are dragging resizehandle
     *
     *  */

    var dragColumns = [];
    var currentDragColumn = 0;
    var resizeHandleElement = null;
    var activeResizeHandle = null;
    var minColumn = null;
    var maxColumn = null;

    // Hoover enter or hoover over
    // find active handle -> if active changed, send event can-drag
    Broadcaster.on(Mouse.config.EVENT.HOOVER_ENTER_BLOCK, function (event) {
        manageResizeHandle(event)
    });
    Broadcaster.on(Mouse.config.EVENT.HOOVER_LEAVE_BLOCK, function (event) {
        // check if not active block
        manageResizeHandle(event)
    });
    Broadcaster.on(Mouse.config.EVENT.HOOVER_OVER_BLOCK, function (event) {
        manageResizeHandle(event)
    });

    // effective dragging
    Broadcaster.on(Mouse.config.EVENT.START_DRAG, function (event) {
        startDrag(event)
    });
    Broadcaster.on(Mouse.config.EVENT.END_DRAG, function (event) {
        endDrag(event)
    });


}]);


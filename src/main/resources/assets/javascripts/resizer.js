blocks.plugin("blocks.core.Resizer", ["blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DomManipulation", function (Broadcaster, Mouse, DOM) {


    var checkIfHandleExists = function () {
        if (handle == null) {
            $(".column-resize-handle").remove();
            handle = $("<div class='.column-resize-handle' />")
            $("body").append(handle);
        }
    };

    var showHandle = function (surface) {
        checkIfHandleExists();
        handle.css('position', 'absolute');
        handle.css('background-color', 'black');
        handle.css("left", surface.left + "px");
        handle.css("top", surface.top + "px");
        handle.css("width", surface.right - surface.left + "px");
        handle.css("height", surface.bottom - surface.top + "px");
    }

    var moveHandle = function () {
        checkIfHandleExists();
        draggedHandle.calculateSurface();
        handle.css('left', draggedHandle.drawSurface.left);
    };

    var removeHandle = function () {
        if (handle != null) {
            handle.remove();
            handle = null;
        }
    };

    var showResizeHandleOnHoover = function (blocksEvent) {
        if (blocksEvent.dragging == false) {
            Logger.debug("Show resizehandler");
            showHandle(blocksEvent.resizeHandle.current.drawSurface);
            $("body").css("cursor", "col-resize");
        }
    };

    var hideResizeHandleOnHoover = function (blocksEvent) {
        if (blocksEvent.dragging == false) {
            removeHandle();
            $("body").css("cursor", 'auto')
        }
    };

    var startDrag = function (blocksEvent) {
        var _this = this;
        $(document).on("mousemove.resizehandledrag", function (event) {
            doDrag(event)
        });
        showHandle(blocksEvent.drag.surface.drawSurface);
        initDrag(blocksEvent.drag.surface);
    };

    var endDrag = function (blocksEvent) {
        $(document).off("mousemove.resizehandledrag");
        removeHandle();
        $('body').css("cursor", 'auto')
        Broadcaster.send(Mouse.config.EVENT.REFRESH_LAYOUT);
    };

    var doDrag = function (event) {
        checkDrag(event);
    };

    var initDrag = function (resizeHandle) {
        draggedHandle = resizeHandle;
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
                    var col = {
                        start: row.left + (i * colWidth) - (colWidth / 2),
                        end: row.left + (i * colWidth) + (colWidth / 2),
                        middle: row.left + (i * colWidth),
                        index: i
                    }
                    dragColumns[i] = col;
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
            Logger.debug("No column change")
            return;
        }

        for (var i=minColumn; i <= maxColumn; i++ ) {
            var diff = i - currentDragColumn;
            if (event.pageX > dragColumns[currentDragColumn + diff].start &&
                event.pageX < dragColumns[currentDragColumn + diff].end) {
                currentDragColumn = currentDragColumn + diff;
                DOM.setColumnWidth(draggedHandle.leftColumn.element, DOM.getColumnWidth(draggedHandle.leftColumn.element) + diff);
                DOM.setColumnWidth(draggedHandle.rightColumn.element, DOM.getColumnWidth(draggedHandle.rightColumn.element) - diff);
                // move resizehandle
                moveHandle();
                break;
            }
        }
    };

    var dragColumns = [];
    var currentColumn = 0;
    var currentDragColumn = 0;
    var handle = null;
    var draggedHandle = null;
    var minColumn = null;
    var maxColumn = null;


    var _this = this;
    Broadcaster.on(Mouse.config.EVENT.HOOVER_ENTER_RESIZE_HANLDE, function (event) {
        showResizeHandleOnHoover(event)
    });
    Broadcaster.on(Mouse.config.EVENT.HOOVER_LEAVE_RESIZE_HANDLE, function (event) {
        hideResizeHandleOnHoover(event)
    });
    Broadcaster.on(Mouse.config.EVENT.START_DRAG_RESIZE_HANDLE, function (event) {
        startDrag(event)
    });
    Broadcaster.on(Mouse.config.EVENT.END_DRAG_RESIZE_HANDLE, function (event) {
        endDrag(event)
    });


}]);


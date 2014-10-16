
blocks.module("blocks.resizer", ["blocks.broadcaster", "blocks.mouseEvent"])
    .service("Resizer", ["Broadcaster", "BlockMouseEvents", function(Broadcaster, BlockMouseEvents) {



        var checkIfHandleExists = function() {
            if (handle == null) {
                $(".column-resize-handle").remove();
                handle = $("<div class='.column-resize-handle' />")
                $("body").append(handle);
            }
        };

        var showHandle = function(surface) {
            checkIfHandleExists();
            handle.css('position', 'absolute');
            handle.css('background-color','black')
            handle.css("left", surface.left + "px");
            handle.css("top", surface.top + "px");
            handle.css("width", surface.right - surface.left + "px");
            handle.css("height", surface.bottom - surface.top + "px");
        }

        var moveHandle = function() {
            checkIfHandleExists();
            draggedHandle.calculateSurface();
            handle.css('left', draggedHandle.drawSurface.left);
        };

        var removeHandle = function() {
            if (handle != null) {
                handle.remove();
                handle = null;
            }
        };

        this.showResizeHandleOnHoover = function(blocksEvent) {
            if (blocksEvent.dragging == false) {
                Logger.debug("Show resizehandler");
                showHandle(blocksEvent.resizeHandle.current.drawSurface);
                $("body").css("cursor", "col-resize");
            }
        };

        this.hideResizeHandleOnHoover = function(blocksEvent) {
            if (blocksEvent.dragging == false) {
                removeHandle();
                $("body").css("cursor", 'auto')
            }
        };

        this.startDrag = function(blocksEvent) {
            var _this = this;
            $(document).on("mousemove.resizehandledrag", function(event) {
                doDrag(event)
            });
            showHandle(blocksEvent.drag.surface);
            initDrag(blocksEvent.drag.surface);
        };

        this.endDrag = function(blocksEvent) {
            $(document).off("mousemove.resizehandledrag");
            removeHandle();
            $('body').css("cursor", 'auto')
            Broadcaster.send(BlockMouseEvents.REFRESH_LAYOUT);
        };

        var doDrag = function(event) {
            checkDrag(event);
        };

        var initDrag = function(resizeHandle) {
            draggedHandle = resizeHandle;
            if (resizeHandle.leftColumn.parent == resizeHandle.rightColumn.parent) {

                var row = resizeHandle.leftColumn.parent;
                var columns = row.children;
                var offsetLeft = 0;
                for (var i=0; i < columns.length; i++) {
                    if (columns[i] !== resizeHandle.leftColumn) {
                        offsetLeft += columns[i].getColWidth();
                    } else {
                        break;
                    }
                }
                var offsetRight = offsetLeft + resizeHandle.leftColumn.getColWidth() + resizeHandle.rightColumn.getColWidth();
                var currentColumn = offsetLeft + resizeHandle.leftColumn.getColWidth();
                var min = offsetLeft + 1;
                var max = offsetRight - 1;
                var colWidth = (row.getFullWidth()) / 12;
                if (max > min) {
                    for (var i=min; i <= max; i++) {
                        var col = {
                            start: row.left + (i*colWidth) - (colWidth/2),
                            end: row.left +  (i*colWidth) + (colWidth/2),
                            middle: row.left +  (i*colWidth),
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

        var checkDrag = function(event) {
            var curCol = dragColumns[currentDragColumn];
            if (event.pageX > curCol.start && event.pageX < curCol.end) {
                Logger.debug("No column change")
                return;
            } else if (minColumn < currentDragColumn &&
                event.pageX > dragColumns[currentDragColumn - 1].start &&
                event.pageX < dragColumns[currentDragColumn - 1].end) {
                Logger.debug("Column change left")
                currentDragColumn = currentDragColumn - 1;
                draggedHandle.leftColumn.setColumnWidth(draggedHandle.leftColumn.getColWidth() - 1);
                draggedHandle.rightColumn.setColumnWidth(draggedHandle.rightColumn.getColWidth() + 1);
                // move resizehandle
                moveHandle();
            } else if (maxColumn > currentDragColumn && event.pageX > dragColumns[currentDragColumn + 1].start && event.pageX < dragColumns[currentDragColumn + 1].end) {
                Logger.debug("Column change right")
                // 1 column to the left
                currentDragColumn = currentDragColumn + 1;
                draggedHandle.leftColumn.setColumnWidth(draggedHandle.leftColumn.getColWidth() + 1);
                draggedHandle.rightColumn.setColumnWidth(draggedHandle.rightColumn.getColWidth() - 1);
                // move resizehandle
                moveHandle();
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
        Broadcaster.on(BlockMouseEvents.HOOVER_ENTER_RESIZE_HANLDE, function(event) {
            _this.showResizeHandleOnHoover(event)});
        Broadcaster.on(BlockMouseEvents.HOOVER_LEAVE_RESIZE_HANDLE, function(event) {_this.hideResizeHandleOnHoover(event)});
        Broadcaster.on(BlockMouseEvents.START_DRAG_RESIZE_HANDLE, function(event) {_this.startDrag(event)});
        Broadcaster.on(BlockMouseEvents.END_DRAG_RESIZE_HANDLE, function(event) {_this.endDrag(event)});


    }]);


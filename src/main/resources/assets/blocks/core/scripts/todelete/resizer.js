/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Resize handles are the handles to resize columns
 * A row contains all the possible resize handles
 *
 * this plugin shows the resizer when hovering over it and allows for resizing columns
 * by dragging the handles.
 *
 * initDrag: here we initialize the triggers to resize the columns while dragging a handle
 * checkDrag: checks to resize a column while dragging
 *
 * this plugin does not send events of it's own
 *
 */
base.plugin("blocks.core.Resizer", ["blocks.core.Broadcaster", "constants.blocks.core", "blocks.core.DOM", function (Broadcaster, Constants, DOM)
{
    var Resizer = this;

    var active = false;
    var draggingEnabled = false;
    var dragging = false;
    var dragColumns = [];
    var currentDragColumn;
    var activeResizer;
    var minColumn;
    var maxColumn;
    var blockResizing = false;
    var activeRowElement = null;

    this.activate = function (value)
    {
        active = value;
        dragColumns = [];
        currentDragColumn = 0;
        activeResizer = null;
        minColumn = null;
        maxColumn = null;
    };

    this.allowResize = function ()
    {
        blockResizing = false;
    };
    this.disallowResize = function ()
    {
        blockResizing = true;
    };


    /*
     * bind to jQuery mousemove event
     * show dragHandle
     * init triggerpoints to resize our column (initdrag)
     * */
    this.startDrag = function (handle)
    {
        if (!blockResizing) {
            Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS);
            activeResizer = handle;
            DOM.enableNativeDnd(false);
            DOM.enableContextMenu(false);
            $("body").addClass(Constants.FORCE_RESIZE_CURSOR_CLASS);

            // Hide all resizers except the ones in the current row
            activeRowElement = handle.leftColumn.parent.element;
            var handles = handle.leftColumn.parent.resizers || [];
            for (var i = 0; i < handles.length; i++) {
                //handles[i].overlay.show();
                handles[i].showOverlay();
            }

            draggingEnabled = true;
            setCursor(false);

            setCursor(true);
            $(document).on("mousemove.resizerdrag", function (event)
            {
                doDrag(event)
            });

            initDrag(activeResizer);
            dragging = true;
        }
    };

    /*
     * unbind from jQuery event
     * remove handle
     * send DOM_CHANGED EVENT
     * */
    this.endDrag = function (handle)
    {
        if (dragging) {
            dragging = false;
            $("body").removeClass(Constants.FORCE_RESIZE_CURSOR_CLASS);
            DOM.enableContextMenu(true);
            Broadcaster.send(Broadcaster.EVENTS.DOM_CHANGED);
            Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS);
        }
    };

    var doDrag = function (event)
    {
        if (dragging) {
            checkDrag(event);
        }
    };

    var setCursor = function (value)
    {
        if (value && activeRowElement != null) {
            activeRowElement.addClass(Constants.RESIZING_CLASS);
        } else if (!value && activeRowElement != null) {
            activeRowElement.removeClass(Constants.RESIZING_CLASS);
            activeRowElement = null;
        }
    };

    /*
     * Before dragging calculate all the possible columns
     *
     * */
    var initDrag = function (resizer)
    {
        activeResizer = resizer;
        var row;
        if (resizer.leftColumn != null) {
            row = resizer.leftColumn.parent;
        } else if (resizer.rightColumn != null) {
            row = resizer.rightColumn.parent;
        } else {
            // left and right column of resizer have different parents !!!
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

        for (var i = 0; i < columns.length; i++) {
            if (columns[i] == resizer.leftColumn) {
                widthLeftColumn = DOM.getColumnWidth(columns[i].element);
                if (resizer.rightColumn != null) {
                    widthRightColumn = DOM.getColumnWidth(columns[i + 1].element);
                }
                break;
            } else if (resizer.leftColumn == null && columns[i] != resizer.rightColumn) {
                widthRightColumn = DOM.getColumnWidth(columns[i].element);
                break;
            } else if (resizer.leftColumn != null) {
                offsetLeft += DOM.getColumnWidth(columns[i].element);
            }
        }

        // current location of the resizer
        var currentPosition = offsetLeft + widthLeftColumn;
        var offsetRight = currentPosition + widthRightColumn;

        // min and max column that we can drag to
        // we can drag until the outer (left or right) column is 1 unit wide
        var min = offsetLeft + 1;
        var max = offsetRight - 1;
        // column width in pixels
        var colWidth = row.getFullWidth() / DOM.MAX_COLUMNS;
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
     */
    var checkDrag = function (event)
    {
        if (dragColumns.length > 0) {
            var curCol = dragColumns[currentDragColumn];
            if (curCol != null && (event.pageX > curCol.start && event.pageX < curCol.end)) {
                //Logger.debug("No column change");
                return;
            }

            for (var i = minColumn; i <= maxColumn; i++) {
                var diff = i - currentDragColumn;
                if (event.pageX > dragColumns[currentDragColumn + diff].start &&
                    event.pageX < dragColumns[currentDragColumn + diff].end) {
                    currentDragColumn = currentDragColumn + diff;
                    if (activeResizer.leftColumn == null) {
                        var element = $('<div class="col-md-1"><div></div></div>');
                        activeResizer.rightColumn.element.before(element);
                        activeResizer.leftColumn = new blocks.elements.Column(0, 0, 0, 0, element, null, 0);
                        DOM.setColumnWidth(activeResizer.rightColumn.element, DOM.getColumnWidth(activeResizer.rightColumn.element) - diff);
                        activeResizer.updateHeight();

                    } else if (activeResizer.rightColumn == null) {
                        activeResizer.rightColumn = {};
                        activeResizer.rightColumn.element = $('<div class="col-md-1"><div></div></div>');
                        DOM.setColumnWidth(activeResizer.leftColumn.element, DOM.getColumnWidth(activeResizer.leftColumn.element) + diff);
                        activeResizer.leftColumn.element.after(activeResizer.rightColumn.element);
                        activeResizer.updateHeight();
                    } else {
                        DOM.setColumnWidth(activeResizer.leftColumn.element, DOM.getColumnWidth(activeResizer.leftColumn.element) + diff);
                        DOM.setColumnWidth(activeResizer.rightColumn.element, DOM.getColumnWidth(activeResizer.rightColumn.element) - diff);
                        activeResizer.updateHeight();
                    }

                    // move resizer and update all handles in the current row
                    activeResizer.update();

                    break;
                }
            }
        }
    };

}]);


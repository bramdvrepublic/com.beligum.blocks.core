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

/*
 * Simple plugin that handles functions for the DOM. All elements should be jQuery Objects.
 * To allow for animations, most functions that manipulate the dom directly (remove an elment, add an element)
 * take callbacks to run when their animation ends. This way you can chain your animations.
 *
 * EXTERNAL FUNCTIONS
 * appendElement
 * removeBlock
 *
 * EXTERNAL HELPER FUNCTIONS
 * ... see bottom page
 *
 *
 * */


base.plugin("blocks.core.DomManipulation", ["constants.base.core.internal", "constants.blocks.core", function (Constants, BlocksConstants)
{
    var DOM = this;

    // LOCAL CONSTANTS
    this.COLUMN_WIDTH_CLASS = [
        {name: "col-xs-", min: 0, max: 767},
        {name: "col-sm-", min: 768, max: 991},
        {name: "col-md-", min: 992, max: 1199},
        {name: "col-lg-", min: 120, max: 10000}
    ];
    this.MAX_COLUMNS = 12;

    var maxZIndex = 0;

    $.fn.hasAttribute = function (name)
    {
        return this.attr(name) !== undefined;
    };

    this.isRow = function (element)
    {
        return element.hasClass(BlocksConstants.ROW_CLASS);
    };

    this.isColumn = function (element)
    {
        var el = $(element);
        var retVal = false;
        var classList = el[0].className.split(/\s+/);
        for (var i = 0; i < classList.length; i++) {
            for (var j = 0; j < DOM.COLUMN_WIDTH_CLASS.length; j++) {
                if (classList[i].indexOf(DOM.COLUMN_WIDTH_CLASS[j].name) == 0) {
                    return true;
                }
            }
        }
        return retVal;
    };

    this.isContainer = function (element)
    {
        return element.hasClass(BlocksConstants.CONTAINER_CLASS);
    };

    //this.isBlock = function (element)
    //{
    //    return element.hasAttribute(Constants.IS_PROPERTY) || !(DOM.isColumn(element) && DOM.isRow(element));
    //};

    // TODO check tagname for '-'
    this.isTemplate = function (element)
    {
        return element.hasAttribute(BlocksConstants.IS_ENTITY);
    };

    //this.isProperty = function (element)
    //{
    //    return element.hasAttribute(Constants.IS_PROPERTY);
    //};

    this.getColumnWidth = function (element)
    {
        //extracts the class width number from the element,
        //searching for a supplied clazz prefix eg. 'col-md-', 'col-xs-', etc
        var getColWidth = function (clazz)
        {
            //g: global
            //i: ignore case
            var regex = new RegExp('\\b' + clazz + '\\d+', 'gi');
            var widths = element[0].className.match(regex, '');
            if (widths != null && widths.length > 0) {
                var nr = widths[0].substring(7, widths[0].length);
                return parseInt(nr);
            } else {
                return null;
            }
        };

        var currentWidth = null;
        var docWidth = $(document).width();
        for (var i = 0; i < DOM.COLUMN_WIDTH_CLASS.length; i++) {
            var colWidth = DOM.COLUMN_WIDTH_CLASS[i];
            var newWidth = getColWidth(colWidth.name);
            if (docWidth > colWidth.max && newWidth != null) {
                currentWidth = newWidth;
            } else if (docWidth >= colWidth.min && docWidth < colWidth.max) {
                if (newWidth != null) {
                    currentWidth = newWidth;
                } else if (currentWidth == null) {
                    currentWidth = 12;
                }
            }
        }

        if (currentWidth == 0) {
            Logger.error("Encountered zero column width for an element, this shouldn't happen", element);
        }

        return currentWidth;
    };

    this.getColumnClass = function ()
    {
        // TODO Wouter: This should change with screen width
        return "col-md-";

        // var colClass = null;
        // var docWidth = $(document).width();
        // for (var i = 0; i < DOM.COLUMN_WIDTH_CLASS.length; i++) {
        //     if ((docWidth >= DOM.COLUMN_WIDTH_CLASS[i].min && docWidth < DOM.COLUMN_WIDTH_CLASS[i].max)) {
        //         colClass = DOM.COLUMN_WIDTH_CLASS[i].name;
        //         break;
        //     } else if (docWidth > DOM.COLUMN_WIDTH_CLASS[i].max) {
        //         colClass = DOM.COLUMN_WIDTH_CLASS[i].name;
        //     }
        // }
        // return colClass;
    };

    // Sets the column width in grid-units, not pixels
    this.setColumnWidth = function (element, newWidth, animationTime, callback)
    {
        var colClass = this.getColumnClass();
        var currentClass = colClass + this.getColumnWidth(element);
        var newClass = colClass + newWidth;
        if (callback == null) {
            element.removeClass(currentClass);
            element.addClass(newClass);
        } else {
            var newWidth = (element.parent().innerWidth() / 12) * newWidth;
            if (currentClass != newClass) {
                element.animate({width: newWidth}, animationTime, function ()
                {
                    element.removeClass(currentClass);
                    element.addClass(newClass);
                    element.css("width", "");
                    callback();
                });
            } else {
                callback();
            }
        }
    };

    // distributes the width of the columns in a row so they take the max nr of grid-units
    this.distributeColumnsInRow = function (element, callback)
    {
        var tcolumns = element.children();
        var columns = [];
        var oldColumnWidths = [];
        var newColIdx = -1;

        // Check if current distribution of columns is incorrect
        // Total width of all columns must be 12
        var totalWidth = 0;
        for (var i = 0; i < tcolumns.length; i++) {
            var col = $(tcolumns[i]);
            if (DOM.isColumn(col)) {
                var colWidth = DOM.getColumnWidth(col);
                totalWidth += colWidth;
                columns.push(col);
                oldColumnWidths.push(colWidth);
                //this is added in layout.js on drop
                if (col.hasClass(BlocksConstants.NEW_BLOCK_CLASS)) {
                    //save the current array index
                    newColIdx = columns.length - 1;
                }
            }
        }

        if (totalWidth == DOM.MAX_COLUMNS) {
            callback();
        }
        else {

            //if we end up here, it means we dropped a new column in a row and the row became too wide
            //instead of solving it by re-distributing all columns in that row (affecting the overall layout way too much),
            //we choose to 'find' the column that's too wide, scale the column to it's left down by one (except when the column if the leftmost)
            //and rescale the new column to size 1

            //old algo spreads all columns across the row, but it's too intrusive
            var USE_OLD_ALGO = false;

            var columnCount = columns.length;
            var columnsWidth = {};
            var newTotalWidth = 0;
            if (USE_OLD_ALGO) {
                var ratio = DOM.MAX_COLUMNS / totalWidth;
                for (var i = 0; i < columnCount; i++) {
                    columnsWidth[i] = Math.round(DOM.getColumnWidth($(columns[i])) * ratio);
                    if (columnsWidth[i] < 1) {
                        columnsWidth[i] = 1;
                    }
                    newTotalWidth += columnsWidth[i];
                }
            }
            else {
                //a column was removed from a row: since we don't really know where, we'll let the right-most column (because we generally assume the leftmost columns to be the most important in western regions) grow to it's full size
                if (totalWidth < DOM.MAX_COLUMNS) {
                    var allButRight = 0;
                    for (var i = 0; i < columnCount - 1; i++) {
                        columnsWidth[i] = oldColumnWidths[i];
                        allButRight += columnsWidth[i];
                    }
                    columnsWidth[columnCount - 1] = DOM.MAX_COLUMNS - allButRight;
                }
                //a new col was added to a row: detect which one, make some room by resizing it's neightbour and fit it in
                else {
                    var allButNew = 0;
                    for (var i = 0; i < columnCount; i++) {
                        columnsWidth[i] = oldColumnWidths[i];

                        if (i != newColIdx) {
                            allButNew += columnsWidth[i];
                        }
                    }

                    //will only trigger if we inserted a new column in a row (not if we moved the order of the cols in the same row)
                    if (newColIdx != -1) {
                        //if there's room left, squeeze in the new col
                        if (allButNew < DOM.MAX_COLUMNS) {
                            columnsWidth[newColIdx] = DOM.MAX_COLUMNS - allButNew;
                        }
                        //if not, make some room by resizing a neighbour
                        else {
                            columnsWidth[newColIdx] = 1;

                            //now, we want to find the closest neighbour we can steal one width from (width >= 2)
                            //(to give to our new col). First, we go to the left, then we go to the right,
                            //then we compare distances and choose the closest.
                            var lNeigh = -1;
                            for (var i = newColIdx; i >= 0 && lNeigh < 0; i--) {
                                if (columnsWidth[i] > 1) {
                                    lNeigh = i;
                                }
                            }
                            var rNeigh = -1;
                            for (var i = newColIdx; i < columnCount && rNeigh < 0; i++) {
                                if (columnsWidth[i] > 1) {
                                    rNeigh = i;
                                }
                            }

                            var bestNeigh;
                            if (lNeigh >= 0 && rNeigh >= 0) {
                                //note: the <= means: if both are equal, we prefer to alter the left one
                                bestNeigh = Math.abs(newColIdx - lNeigh) <= Math.abs(newColIdx - rNeigh) ? lNeigh : rNeigh;
                            }
                            else if (lNeigh >= 0) {
                                bestNeigh = lNeigh;
                            }
                            else if (rNeigh >= 0) {
                                bestNeigh = rNeigh;
                            }
                            else {
                                Logger.error("Both left- and right-side neightbours of the new block don't have room; this should be impossible", element);
                            }

                            columnsWidth[bestNeigh] = columnsWidth[bestNeigh] - 1;
                        }
                    }
                }

                //should always be 12, right?
                newTotalWidth = 0;
                for (var i = 0; i < columnCount; i++) {
                    newTotalWidth += columnsWidth[i];
                }
                if (newTotalWidth != DOM.MAX_COLUMNS) {
                    Logger.error("New total width of the columns in this row does not equal " + DOM.MAX_COLUMNS + '; this shouldn\'t happen', element);
                }
            }

            var diff = DOM.MAX_COLUMNS - newTotalWidth;
            var index = 0;
            var doSetColumnWidth = function ()
            {
                if (index < columnCount) {
                    if (diff < 0 && columnsWidth[index] > 1) {
                        diff += 1;
                        columnsWidth[index] -= 1;
                    } else if (diff > 0) {
                        diff -= 1;
                        columnsWidth[index] += 1;
                    }
                    DOM.setColumnWidth($(columns[index]), columnsWidth[index], 50, function ()
                    {
                        index += 1;
                        doSetColumnWidth();
                    });

                } else {
                    callback();
                }
            };
            doSetColumnWidth();
        }
    };

    /**
     * Finds the maximum z-index in the DOM tree of elements that are relative or absolutely positioned.
     * Returns 1 when no such elements were found.
     */
    this.calculateMaxIndex = function ()
    {
        maxZIndex = Math.max.apply(null, $.map($('body  *'), function (e, n)
            {
                if ($(e).css('position') == 'absolute' || $(e).css('position') == 'relative') {
                    return parseInt($(e).css('z-index')) || 1;
                }
            })
        );
    };
    this.getMaxZIndex = function ()
    {
        return maxZIndex;
    };

    /*
     * METHODS TO CLEAN THE DOM (REMOVE COLUMNS WITHOUT BLOCKS, ROWS WITHOUT COLUMNS, ...)
     * usefull after manipulating the layout
     * */

    // if Column or row is empty then delete
    // when not deleting, try simplifying
    //this.deleteEmptyElement = function (element, callback)
    //{
    //    var isLayout = (DOM.isColumn(element) || DOM.isRow(element));
    //    if (isLayout && element.children().length == 0) {
    //        var parent = element.parent();
    //        element.toggle(150, function() {
    //            element.remove();
    //            DOM.deleteEmptyElement(parent, callback);
    //        });
    //
    //    } else {
    //        simplifyElement(element, callback);
    //    }
    //};
    //
    //// generic method to simplify columns and rows.
    //var simplifyElement = function (element, callback)
    //{
    //    if (DOM.isRow(element)) {
    //        simplifyColumnInColumn(element, callback);
    //    } else if (DOM.isColumn(element)) {
    //        simplifyRowInRow(element, callback);
    //    }
    //}
    //
    //// if: 1 column(A) in 1 row(B) in 1 column(C),
    //// then we can put template of column A in Column C and delete A & B
    //var simplifyColumnInColumn = function (element, callback)
    //{
    //    var parentColumn = element.parent();
    //
    //    if (element.children().length == 1) {
    //        var childColumn = DOM.isColumn($(element.children()[0]));
    //        if (DOM.isRow(element) &&
    //            DOM.isColumn(parentColumn) &&
    //            DOM.isColumn(childColumn) &&
    //            parentColumn.children().length == 1)
    //        {
    //            parentColumn.children.remove();
    //            parentColumn.append(childColumn.children());
    //        } else {
    //            DOM.changedRows.push(element);
    //            callback();
    //        }
    //    } else {
    //        if (DOM.isRow(element)) DOM.changedRows.push(element);
    //        callback();
    //    }
    //};
    //
    //// if: 1 Row(A) in 1 Column(B) in 1 Row(C),
    //// then we can put template of Row A in Row C and delete A & B
    //var simplifyRowInRow = function (element, callback)
    //{
    //    var parentRow = element.parent();
    //    // We have a column with 1 child that is a row and this column
    //    //is the single child of the parent row
    //    if (DOM.isColumn(element)) {
    //        var childRows = element.children(".row");
    //        if (childRows.length > 0 && DOM.isRow(childRow) && parentRow.children().length == 1) {
    //            parentRow.replaceWith(childRows);
    //            DOM.changedRows.push(parentRow);
    //        } else {
    //            callback();
    //        }
    //    } else {
    //        callback();
    //    }
    //
    //};

    this.cleanDown = function (element, callback)
    {
        // clean
        var findNext = function (element, callback)
        {
            if (element.next().length > 0) {
                DOM.cleanup(element.next(), callback);
            } else if (!DOM.isContainer(element)) {
                DOM.cleanDown(element.parent(), callback);
            } else {
                callback();
            }
        };

        if (element.children().length == 0) {
            element.toggle(50, function ()
            {
                var parent = element.parent();
                element.remove();
                DOM.cleanup(parent, callback);
            });
        } else if (DOM.isColumn(element)) {
            var childRows = element.children(".row");
            if (childRows.length > 0 && DOM.isRow(element.parent()) && element.parent().children().length == 1) {
                element.parent().replaceWith(childRows);
                DOM.cleanup(childRows.first(), callback);
            } else {
                findNext(element, callback);
            }
        } else if (DOM.isRow(element)) {
            var childColumns = element.children(".column");
            if (childColumns.length > 0 && element.parent().children().length == 1) {
                element.parent().replaceWith(childColumns);
                DOM.cleanup(childColumns.first(), callback);
            } else {
                DOM.distributeColumnsInRow(element, function ()
                {
                    findNext(element, callback);
                });
            }
        } else {
            findNext(element, callback);
        }

    };

    this.cleanup = function (element, callback)
    {
        var children;
        if (DOM.isColumn(element) || DOM.isContainer(element)) {
            children = element.children(".row");
        } else if (DOM.isRow(element)) {
            children = element.children();
        }

        if (children.first().length > 0) {
            DOM.cleanup(children.first(), callback);
        } else {
            DOM.cleanDown(element, callback);
        }
    };


    // function to insert a block and clean up
    this.appendElement = function (blockElement, dropLocationElement, side, callback)
    {
        if (side == Constants.SIDE.RIGHT || side == Constants.SIDE.BOTTOM) {
            dropLocationElement.after(blockElement)
        } else if (side == Constants.SIDE.TOP || side == Constants.SIDE.LEFT) {
            dropLocationElement.before(blockElement)
        }
        callback();
    };

    // Function to remove a block and clean up
    this.removeBlock = function (element, animationTime, callback)
    {
        element.toggle(animationTime, function ()
        {
            var blockElement = element.remove();
            blockElement.toggle();
            callback();

        })
    };
    /*
     *  HELPER FUNCTIONS TO WRAP BLOCKS AND EASILY MANIPULATE LAYOUT
     */
    this.createRow = function ()
    {
        return $('<div class="' + BlocksConstants.ROW_CLASS + '"></div>');
    };

    this.createColumn = function (columnWidth)
    {
        return $('<div class="' + this.getColumnClass() + columnWidth + '"></div>');
    };

    this.wrapBlockInColumn = function (blockElement, columnWidth)
    {
        columnWidth = columnWidth == null ? 12 : columnWidth;
        var col = DOM.createColumn(columnWidth)
        blockElement = blockElement.replaceWith(col);
        col.append(blockElement);
        return col;
    };

    this.wrapBlockInRow = function (blockElement)
    {
        var row = DOM.createRow();
        blockElement = blockElement.replaceWith(row);
        row.append(DOM.createColumn(12).append(blockElement));
        return row;
    };

    this.wrapColumnInRow = function (blockElement)
    {
        DOM.setColumnWidth(blockElement, 12);
        var row = DOM.createRow();
        blockElement = blockElement.replaceWith(row);
        row.append(blockElement);
        return row;
    };

    this.wrapColumnInColumn = function (blockElement)
    {
        var width = DOM.getColumnWidth(blockElement);
        DOM.setColumnWidth(blockElement, 12);
        var col = DOM.createColumn(width);
        blockElement = blockElement.replaceWith(col);
        col.append(DOM.createRow().append(blockElement));
        return col;
    };

    this.wrapRowInColumn = function (blockElement)
    {
        return DOM.wrapBlockInColumn(blockElement, 12);
    };

    this.wrapRowInRow = function (blockElement)
    {
        return DOM.wrapColumnInRow(DOM.wrapRowInColumn(blockElement, 12));
    };

    /*
     * When 1 column contains e.g. 6 templates and we drop a block
     * to the right of the 3th block in that column,
     * then we need to wrap all templates in rows
     *
     * We do this efficiently and in this example this method would wrap:
     *  - block 1 & 2 in a row (with 1 column)
     *  - block 3 in a row (with 1 column)
     *  - 4,5,6 in 1 row (with 1 column)
     *
     * this method takes a block and wraps this block in a row
     * and also wraps the other siblings in rows
     *
     * returns the blockelement(!) inside a new row and column
     *
     * */
    this.wrapSiblingBlocksInRows = function (blockElement)
    {
        var parentColumnElement = blockElement.parent();
        if (DOM.isColumn(parentColumnElement)) {
            var before = [];
            var current = null;
            var after = [];
            var children = parentColumnElement.children().remove();
            var childCount = children.length;
            var flag = false;
            // grab all templates in the parent column
            // put all siblings before our block in the before array
            // put all siblings after our block in the after array
            for (var i = 0; i < childCount; i++) {
                var child = children[i];
                if (child === blockElement[0]) {
                    current = child;
                    flag = true;
                } else if (!flag) {
                    before.push(child);
                } else {
                    after.push(child)
                }
            }
            // wrap before, current and after in a row and re-append to the parent
            if (before.length > 0) {
                parentColumnElement.append(DOM.wrapBlockInRow($(before)));
            }
            parentColumnElement.append(DOM.wrapBlockInRow($(current)));
            if (after.length > 0) {
                parentColumnElement.append(DOM.wrapBlockInRow($(after)));
            }

            return blockElement;
        }

        return blockElement;
    };

    // http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
    this.disableTextSelection = function ()
    {
        var sel = window.getSelection();
        sel.removeAllRanges();
        var html = $("html");
        html.addClass(BlocksConstants.PREVENT_SELECTION_CLASS);
        window.ondragstart = function ()
        {
            return false;
        };
    };

    this.enableTextSelection = function ()
    {
        //http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
        var html = $("html");
        html.removeClass(BlocksConstants.PREVENT_SELECTION_CLASS);
        window.ondragstart = function ()
        {
            return true;
        };
    };

    this.disableContextMenu = function ()
    {
        $("html").attr("oncontextmenu", "return false;");
        // IE < 10
        $("html").attr("onselectstart", "return false;");
    };

    this.enableContextMenu = function ()
    {
        $("html").removeAttr("oncontextmenu", "");
        // IE < 10
        $("html").removeAttr("onselectstart");
    };

    // debouncing function from John Hann
    // http://unscriptable.com/index.php/2009/03/20/debouncing-javascript-methods/

    var debounce = function (func, threshold, execAsap)
    {
        var timeout;

        return function debounced()
        {
            var obj = this, args = arguments;

            function delayed()
            {
                if (!execAsap)
                    func.apply(obj, args);
                timeout = null;
            };

            if (timeout)
                clearTimeout(timeout);
            else if (execAsap)
                func.apply(obj, args);

            timeout = setTimeout(delayed, threshold || 50);
        };
    };

    // smartresize
    $.fn["smartresize"] = function (fn)
    {
        return fn ?
            this.bind('resize', debounce(fn)) :
            this.trigger("smartresize");
    };

    // smartresize
    $.fn["smartmousemove"] = function (fn)
    {
        return fn ? this.bind('resize', debounce(fn)) : this.trigger("smartmousemove");
    };
}]);
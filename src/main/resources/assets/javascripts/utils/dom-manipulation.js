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

blocks.plugin("blocks.core.DomManipulation", ["blocks.core.Broadcaster", "blocks.core.Constants", function (Broadcaster, Constants) {
    var _thisService = this;
    var DOM = this;
    // Get column width (in grid units 1-12, not pixels)

    this.canEdit = function(element) {
        return element.hasClass(Constants.CAN_EDIT_BLOCK_CLASS);
    };

    this.canLayout = function(element) {
        return element.hasClass(Constants.CAN_LAYOUT_ROW_CLASS);
    };

    this.isRow = function(element) {
        return element.hasClass(Constants.ROW_CLASS);
    }

    this.isColumn = function(element) {
        return element.hasClass(Constants.COLUMN_CLASS);
    }

    this.isContainer = function(element) {
        return element.hasClass(Constants.CONTAINER_CLASS);
    }

    this.isBlock = function(element) {
        return element.hasAttribute(Constants.IS_TYPE) || element.hasAttribute(Constants.IS_PROPERTY) || !(_thisService.isColumn(element) && _thisService.isRow(element));
    }


    this.getColumnWidth = function (element) {
        var widths = element[0].className.match(/\bcol-md-\d+/g, '');
        if (widths != null && widths.length > 0) {
            var nr = widths[0].substring(7, widths[0].length);
            return parseInt(nr);
        } else {
            Logger.error("Column. Could not get width of column");
            return 0;
        }
    };

    // Sets the column width in grid-units, not pixels
    this.setColumnWidth = function (element, newWidth, animationTime, callback) {
        var currentClass = Constants.COLUMN_WIDTH_CLASS + this.getColumnWidth(element);
        var newClass = Constants.COLUMN_WIDTH_CLASS + newWidth;
        if (callback == null) {
            element.removeClass(currentClass);
            element.addClass(newClass);
        } else {
            var newWidth = (element.parent().innerWidth() / 12) * newWidth;
            if (currentClass != newClass) {
                element.animate({width: newWidth}, animationTime, function () {
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
    var distributeColumnsInRow = function (element, callback) {
        var columns = element.children("." + Constants.COLUMN_CLASS);
        // Check if current distribution of columns is incorrect
        // Total width of all columns must be 12
        var totalWidth = 0;
        for (var i=0; i < columns.length; i++) {
            totalWidth += _thisService.getColumnWidth($(columns[i]));
        }
        if (totalWidth == Constants.MAX_COLUMNS) return;

        var columnCount = columns.length;
        var columnsWidth = {};
        var ratio = Constants.MAX_COLUMNS / totalWidth;
        var newTotalWidth = 0;
        for (var i=0; i < columnCount; i++) {
            columnsWidth[i] = Math.round(_thisService.getColumnWidth($(columns[i])) * ratio);
            if (columnsWidth[i] < 1) {
                columnsWidth[i] = 1;
            }
            newTotalWidth += columnsWidth[i];
        }
        var diff = Constants.MAX_COLUMNS - newTotalWidth;

        var index = 0;
        var doSetColumnWidth = function() {
            if (index < columnCount) {
                if (diff < 0 && columnsWidth[index] > 1) {
                    diff += 1;
                    columnsWidth[index] -= 1;
                } else if (diff > 0) {
                    diff -= 1;
                    columnsWidth[index] += 1;
                }
                _thisService.setColumnWidth($(columns[index]), columnsWidth[index], 200, function() {
                    index += 1;
                    doSetColumnWidth();
                });

            } else {
                callback();
            }
        }
        doSetColumnWidth();
        var x= 0;

    };

    /*
    * METHODS TO CLEAN THE DOM (REMOVE COLUMNS WITHOUT BLOCKS, ROWS WITHOUT COLUMNS, ...)
    * usefull after manipulating the layout
    * */

    // generic method that starts the chain of cleaning.
    // first delete empty elements
    var elementChanged = function (element, callback) {
        Logger.debug("Element Changed")
        if (element == null) {
            // What to do???
        } else {
            deleteEmptyElement(element, callback);
        }
    };

    // if Column or row is empty then delete
    // when not deleting, try simplifying
    var deleteEmptyElement = function (element, callback) {
        if (((element.hasClass(Constants.COLUMN_CLASS) || element.hasClass(Constants.ROW_CLASS))) &&
            element.children().length == 0) {
            var parent = element.parent();
            element.remove();

            if (parent == null) {
                // do nothing, this should not happen
            } else {
                elementChanged(parent, callback);
            }
        } else {
            simplifyElement(element, callback);
        }
    };

    // generic method to simplify columns and rows.
    var simplifyElement = function(element, callback) {
        if (element.hasClass(Constants.COLUMN_CLASS)) {
            simplifyColumnInColumn(element, callback);
        } else if (element.hasClass(Constants.ROW_CLASS)) {
            simplifyRowInRow(element, callback);
        } else {
            callback();
        }
    }

    // if: 1 column(A) in 1 row(B) in 1 column(C),
    // then we can put template of column A in Column C and delete A & B
    var simplifyColumnInColumn = function (element, callback) {
        if (element.parent().children().length == 1 && // 1 column in row
            element.parent().parent().children().length == 1 &&  // 1 row in column
            !element.parent().hasClass(Constants.CAN_LAYOUT_ROW_CLASS)) { // do not delete can_layout elements
            var parentColumn = element.parent().parent();
            var children = element.children().remove();
            parentColumn.children().remove();
            parentColumn.append(children); // column with new template
            elementChanged(parentColumn, callback)
        } else {
            callback();
        }
    };

    // if: 1 Row(A) in 1 Column(B) in 1 Row(C),
    // then we can put template of Row A in Row C and delete A & B
    var simplifyRowInRow = function (element, callback) {
        if (element.parent().children().length == 1 && // 1 row (A) in column (B)
            element.parent().parent().children().length == 1 &&  // 1 column(B) in row (C)
            !(element.hasClass(Constants.CAN_LAYOUT_ROW_CLASS))) { // do not delete can_layout elements
            var parentRow = element.parent().parent(); // Row C
            var children = element.children().remove();
            parentRow.children().remove();
            parentRow.append(children); // column with new template
            elementChanged(parentRow, callback);
        } else {
            distributeColumnsInRow(element, callback);
        }
    };


    // function to insert a block and clean up
    this.appendElement = function (blockElement, dropLocationElement, side, callback) {
        blockElement.toggle(false);
        if (side == Constants.SIDE.RIGHT || side == Constants.SIDE.BOTTOM) {
            dropLocationElement.after(blockElement)
        } else if (side == Constants.SIDE.TOP || side == Constants.SIDE.LEFT) {
            dropLocationElement.before(blockElement)
        }

        elementChanged(dropLocationElement.parent(), function() {
            blockElement.toggle(300, function() {
                callback();
            });

        })
    };


    // Function to remove a block and clean up
    this.removeBlock = function (block, animationTime, callback) {
        block.element.toggle(animationTime, function() {
            var blockElement = block.element.remove();
            blockElement.toggle();
            if (block.parent != null) {
                elementChanged(block.parent.element, callback);
            } else {
                callback();
            }
        })
    };
    /*
     *  HELPER FUNCTIONS TO WRAP BLOCKS AND EASILY MANIPULATE LAYOUT
     */
    this.createRow = function () {
        return $("<div class='" + Constants.ROW_CLASS + "'></div>")
    };

    this.createColumn = function (columnWidth) {
        return $("<div class='" + Constants.COLUMN_CLASS + " " + Constants.COLUMN_WIDTH_CLASS + columnWidth +"'></div>");
    };

    this.wrapBlockInColumn = function (blockElement, columnWidth) {
        columnWidth = columnWidth == null ? 12 : columnWidth;
        var col = DOM.createColumn(columnWidth)
        blockElement = blockElement.replaceWith(col);
        col.append(blockElement);
        return col;
    };

    this.wrapBlockInRow = function (blockElement) {
        var row = DOM.createRow();
        blockElement = blockElement.replaceWith(row);
        row.append(DOM.createColumn(12).append(blockElement));
        return row;
    };

    this.wrapColumnInRow = function (blockElement) {
        DOM.setColumnWidth(blockElement, 12);
        var row = DOM.createRow();
        blockElement = blockElement.replaceWith(row);
        row.append(blockElement);
        return row;
    };

    this.wrapColumnInColumn = function (blockElement) {
        var width = DOM.getColumnWidth(blockElement);
        DOM.setColumnWidth(blockElement, 12);
        var col = DOM.createColumn(width);
        blockElement = blockElement.replaceWith(col);
        col.append(DOM.createRow().append(blockElement));
        return col;
    };

    this.wrapRowInColumn = function (blockElement) {
        return DOM.wrapBlockInColumn(blockElement, 12);
    }

    this.wrapRowInRow = function (blockElement) {
        return DOM.wrapColumnInRow(DOM.wrapRowInColumn(blockElement, 12));
    }

    /*
    * When 1 column contains e.g. 6 blocks and we drop a block
    * to the right of the 3th block in that column,
    * then we need to wrap all blocks in rows
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
    this.wrapSiblingBlocksInRows = function (blockElement) {
        var parentColumnElement = blockElement.parent();
        if (parentColumnElement.hasClass(Constants.COLUMN_CLASS)) {
            var before = [];
            var current = null;
            var after = [];
            var children = parentColumnElement.children().remove();
            var childCount = children.length;
            var flag = false;
            // grab all blocks in the parent column
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

    var DOM = this;


}]);
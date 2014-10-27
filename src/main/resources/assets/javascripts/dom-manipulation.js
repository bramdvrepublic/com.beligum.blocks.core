blocks.plugin("blocks.core.DomManipulation", ["blocks.core.Elements", "blocks.core.Broadcaster", "blocks.core.Constants", function (Elements, Broadcaster, Constants) {
    var _thisService = this;
    this.getColumnWidth = function (element, callback) {
        var widths = element[0].className.match(/\bcol-md-\d+/g, '');
        if (widths != null && widths.length > 0) {
            var nr = widths[0].substring(7, widths[0].length);
            return parseInt(nr);
        } else {
            Logger.error("Column. Could not get width of column");
            return 0;
        }
    };

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


    // if Column or row is empty then delete
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

    var simplifyElement = function(element, callback) {
        if (element.hasClass(Constants.COLUMN_CLASS)) {
            simplifyColumnInColumn(element, callback);
        } else if (element.hasClass(Constants.ROW_CLASS)) {
            simplifyRowInRow(element, callback);
        } else {
            callback();
        }
    }

    var elementChanged = function (element, callback) {
        Logger.debug("Element Changed")
        if (element == null) {
            // What to do???
        } else {
            deleteEmptyElement(element, callback);
        }
    };


    // if: 1 column(A) in 1 row(B) in 1 column(C),
    // then we can put content of column A in Column C and delete A & B
    var simplifyColumnInColumn = function (element, callback) {
        if (element.parent().children().length == 1 && // 1 column in row
            element.parent().parent().children().length == 1 &&  // 1 row in column
            !element.parent().hasClass(Constants.CAN_LAYOUT_CLASS)) { // do not delete can_layout elements
            var parentColumn = element.parent().parent();
            var children = element.children().remove();
            parentColumn.children().remove();
            parentColumn.append(children); // column with new content
            elementChanged(parentColumn, callback)
        } else {
            callback();
        }
    };

    // if: 1 Row(A) in 1 Column(B) in 1 Row(C),
    // then we can put content of Row A in Row C and delete A & B
    var simplifyRowInRow = function (element, callback) {
        if (element.parent().children().length == 1 && // 1 row (A) in column (B)
            element.parent().parent().children().length == 1 &&  // 1 column(B) in row (C)
            !(element.hasClass(Constants.CAN_LAYOUT_CLASS))) { // do not delete can_layout elements
            var parentRow = element.parent().parent(); // Row C
            var children = element.children().remove();
            parentRow.children().remove();
            parentRow.append(children); // column with new content
            elementChanged(parentRow, callback);
        } else {
            distributeColumnsInRow(element, callback);
        }
    };


    this.appendElement = function (blockElement, dropLocationElement, side) {
        blockElement.toggle(false);
        if (side == Constants.SIDE.RIGHT || side == Constants.SIDE.BOTTOM) {
            dropLocationElement.after(blockElement)
        } else if (side == Constants.SIDE.TOP || side == Constants.SIDE.LEFT) {
            dropLocationElement.before(blockElement)
        }

        elementChanged(dropLocationElement.parent(), function() {
            blockElement.toggle(300, function() {
                Broadcaster.sendNoTimeout("refreshLayout");
            });

        })
    };


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



}]);
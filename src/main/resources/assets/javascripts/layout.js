/**
 * Created by wouter on 17/09/14.
 */



blocks.plugin("blocks.core.Layouter", ["blocks.core.Elements", "blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.DomManipulation", function (Elements, Broadcaster, Constants, DOM) {
        var _thisService = this;
        var distributeColumnsInRow2 = function (element) {
            var columns = element.children("." + Constants.COLUMN_CLASS);
            // Check if current distribution of columns is incorrect
            // Total width of all columns must be 12
            var totalWidth = 0;
            for (var i=0; i < columns.length; i++) {
                totalWidth += DOM.getColumnWidth($(columns[i]));
            }
            if (totalWidth == 12) return;

            var columnCount = columns.length;
            var firstColumn = Constants.MAX_COLUMNS % columnCount;
            var newWidth = Constants.MAX_COLUMNS;
            if (firstColumn == 0) {
                newWidth = Constants.MAX_COLUMNS / columnCount;
                firstColumn = newWidth;
            } else {
                newWidth = (Constants.MAX_COLUMNS - firstColumn) / columnCount;
                firstColumn += newWidth;
            }

            for (var i = 0; i < columnCount; i++) {
                if (i == 0) {
                    Elements.setColumnWidth($(columns[i]), firstColumn);
                } else {
                    Elements.setColumnWidth($(columns[i]), newWidth);
                }
            }
        };

        var createRow = function () {
            return $("<div class='" + Constants.ROW_CLASS + "'></div>")
        };

        var createColumn = function (columnWidth) {
            return $("<div class='" + Constants.COLUMN_CLASS + " " + Constants.COLUMN_WIDTH_CLASS + columnWidth +"'></div>");
        };

        var wrapBlockInColumn = function (blockElement, columnWidth) {
            return createColumn(columnWidth).append(blockElement);
        };

        var wrapBlockInRow = function (blockElement) {
            return createRow().append(createColumn(12).append(blockElement));
        };



        var wrapSiblingBlocksInRows = function (blockElement) {
            var parentColumnElement = blockElement.parent();
            if (parentColumnElement.hasClass(Constants.COLUMN_CLASS)) {
                var before = [];
                var current = null;
                var after = [];
                var children = parentColumnElement.children().remove();
                var childCount = children.length;
                var flag = false;
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
                if (before.length > 0) parentColumnElement.append(wrapBlockInRow(before));
                parentColumnElement.append(wrapBlockInRow(current));
                if (after.length > 0)parentColumnElement.append(wrapBlockInRow(after));
                return blockElement.parent();
            } else {
                return blockElement;
            }
        };

        var getDropLocationElement = function (dropLocation, side) {
            var dropLocationElement = dropLocation.element;
            var retVal = dropLocationElement;
            if (dropLocation instanceof Elements.Block) {
                if (side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) {
                    wrapSiblingBlocksInRows(dropLocationElement);
                    retVal = dropLocationElement.parent();
                } else {
                    // Do nothing
                }
            } else if (dropLocationElement.hasClass(Constants.CAN_LAYOUT_CLASS)) {
                var childrenColumns = dropLocationElement.children();
                if (childrenColumns.length > 1) {
                    retVal = createRow().append(childrenColumns.remove());
                    dropLocationElement.append(wrapBlockInColumn(retVal, 12));
                } else if (childrenColumns.length == 1) {
                    if (side == Constants.SIDE.TOP) {
                        retVal = $(childrenColumns[0].children[0]);
                    } else {
                        retVal = $(childrenColumns[0].children[childrenColumns[0].children.length - 1]);
                    }
                } else {
                    Logger.debug("This should never happen!")
                }
            }
            return retVal;
        };

        this.changeBlockLocation = function (block, dropLocation, side) {
            // remove dropped block
            var columnWidth = DOM.getColumnWidth(block.parent.element);
            DOM.removeBlock(block, 200, function() {
                prepareDropLocation(block.element, dropLocation, side)
            });

            var prepareDropLocation =  function(droppedElement, dropLocation, side) {
                var dropLocationElement = getDropLocationElement(dropLocation, side);
                if (dropLocationElement.hasClass(Constants.COLUMN_CLASS)) {
                    droppedElement = wrapBlockInColumn(droppedElement, columnWidth);
                } else if (dropLocationElement.hasClass(Constants.ROW_CLASS)) {
                    droppedElement = wrapBlockInRow(droppedElement);
                } else if (dropLocationElement.hasClass(Constants.BLOCK_CLASS) && (side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT)) {
                    //dropLocationElement = wrapChildBlocksInRows(dropLocationElement);
                    droppedElement = wrapBlockInColumn(droppedElement, columnWidth);
                } else {
                    // this should not be possible
                }
                DOM.appendElement(droppedElement, dropLocationElement, side);

            };

        };

        this.addNewBlockAtBlockSide = function(blockElement, dropLocation, side) {

        };


    }]);

    blocks.config("blocks.core.Layouter", {
        EVENTS: {
            LAYOUT_CHANGED: "LayoutChangedEvent",
            ROW_CHANGED: "RowChangedEvent",
            COLUMN_CHANGED: "ColumnChangedEvent",
            FADE_TIME: 200
        }
    })

    blocks.config("blocks.core.Elements", {

        SHOW_ONLY_FUNCTIONAL_DROPSPOTS: true
    });

/**
 * Created by wouter on 17/09/14.
 */


blocks.module("blocks.elements", ["blocks.core", "blocks.broadcaster"])
    .service("Elements", ["Class", function(Class) {

        this.COLUMN_CLASS = "column";
        this.ROW_CLASS = "row";
        this.BLOCK_CLASS = "block";
        this.COLUMN_WIDTH_CLASS = "col-md-";
        this.CAN_LAYOUT_CLASS = "can-layout";
        this.MAX_COLUMNS = 12;

        var _thisService = this;

        var surface = Class({
            constructor: function(top, bottom, left, right) {
                if (top <= bottom) {
                    this.top = top;
                    this.bottom = bottom;
                } else {
                    this.top = bottom;
                    this.bottom = top;
                }
                if (left <= right) {
                    this.left = left;
                    this.right = right;
                } else {
                    this.left = right;
                    this.right = left;
                }
            },

            isEventInSurface: function(event) {
                var retVal = false;
                if (this.top <= event.pageY && event.pageY <= this.bottom && this.left <= event.pageX && event.pageX <= this.right) {
                    retVal = true;
                }
                return retVal;
            }
        })
        var dropspot = Class(surface, {
            STATIC: {
                SIDE: {
                    TOP: 1,
                    BOTTOM: 2,
                    LEFT: 3,
                    RIGHT: 4,
                    NONE: 0
                },
                WIDTH: 20 //  defines the hotspot width on the side of the blocks
            },

            constructor: function(top, bottom, left, right, side, parent) {
                dropspot.Super.call(this, top, bottom, left, right);
                this.parent = parent;
                this.drawSurface = new surface(top, bottom, left, right);
                this.side = side;
                this.calculateDrawSurface();
            },

            calculateDrawSurface: function() {
                var parent = this.parent;
                if (this.side == dropspot.SIDE.TOP || this.side == dropspot.SIDE.LEFT) {
                    // search element that is not first or until parent == null

                } else {
                    while (parent.element != null && parent.element.next().length == 0 && parent.parent != null) {
                        parent = parent.parent;
                    }
                }
                if (parent != this.parent) {

                }
                if (this.side == dropspot.SIDE.LEFT) {
                    while (parent.parent != null && parent.isFirstLeft()) {
                        parent = parent.parent;
                    }
                    this.drawSurface.left = parent.left;
                    this.drawSurface.right = parent.left + dropspot.WIDTH;
                } else if (this.side == dropspot.SIDE.TOP) {
                    while (parent.parent != null && parent.isFirstTop()) {
                        parent = parent.parent;
                    }
                    this.drawSurface.top = parent.top;
                    this.drawSurface.bottom = parent.top + dropspot.WIDTH;
                } else if (this.side == dropspot.SIDE.BOTTOM) {
                    while (parent.parent != null && parent.isLastBottom()) {
                        parent = parent.parent;
                    }
                    this.drawSurface.top = parent.bottom - dropspot.WIDTH;
                    this.drawSurface.bottom = parent.bottom;
                } else if (this.side == dropspot.SIDE.RIGHT) {
                    while (parent.parent != null && parent.isLastRight()) {
                        parent = parent.parent;
                    }
                    this.drawSurface.left = parent.right - dropspot.WIDTH;
                    this.drawSurface.right = parent.right;
                }

            },

            isTriggered: function(event) {
                var retVal = false;
                if (this.isEventInSurface(event)) {
                    retVal = true;
                }
                return retVal;
            }

        });
        var resizeHandle =  Class(surface, {
            STATIC: {
                DRAW_WIDTH: 5,
                TRIGGER_WIDTH: 20
            },

            constructor: function(leftColumn, rightColumn) {
                this.leftColumn = leftColumn;
                this.rightColumn = rightColumn;
                this.calculateSurface();
            },

            calculateSurface: function() {
                var middle = (this.leftColumn.calculateRight(this.leftColumn.element) + this.leftColumn.calculateLeft(this.rightColumn.element)) / 2;
                this.leftColumn.right = middle;
                this.rightColumn.left = middle;
                var t = Math.min(this.leftColumn.top, this.rightColumn.top);
                var b = Math.max(this.leftColumn.bottom, this.rightColumn.bottom);
                var l = ((this.leftColumn.right + this.rightColumn.left) / 2) - (resizeHandle.TRIGGER_WIDTH/2);
                var r = ((this.leftColumn.right + this.rightColumn.left) / 2) + (resizeHandle.TRIGGER_WIDTH/2);
                resizeHandle.Super.call(this, t, b, l, r);

                var l = ((this.leftColumn.right + this.rightColumn.left) / 2) - (resizeHandle.DRAW_WIDTH/2);
                var r = ((this.leftColumn.right + this.rightColumn.left) / 2) + (resizeHandle.DRAW_WIDTH/2);
                this.drawSurface = new surface(t, b, l, r);
            },

            isTriggered: function(event) {
                var retVal = false;
                if (this.isEventInSurface(event)) {
                    retVal = true;
                }
                return retVal;
            }

        })
        var layoutElement = Class(surface, {
            calculateTop: function(element) {
                return  element.offset().top
            },

            calculateBottom: function(element) {
                return element.offset().top + element.outerHeight()
            },

            calculateLeft: function(element) {
                return element.offset().left
            },

            calculateRight: function(element) {
                return element.offset().left + element.outerWidth()
            },

            top: 0,
            bottom: 0,
            left: 0,
            right: 0,
            element: null,
            parent: null,

            constructor: function(top, bottom, left, right, element, parent) {
                layoutElement.Super.call(this, top, bottom, left, right);
                this.parent = parent;
                this.element = element;
                this.children = [];
                this.triggers = [];
                this.resizeHandles = [];
            },

            findActiveElement: function(event) {
                var retVal = null;
                if (this.isEventInSurface(event)) {
                    var i = 0;
                    while (retVal == null && i < this.children.length) {
                        retVal = this.children[i].findActiveElement(event);
                        i++;
                    }
                    if (retVal == null) {
                        retVal = this;
                    }
                }
                return retVal;
            },

            findActiveTrigger: function(event, triggerType) {
                var retVal = null;
                if (this.isEventInSurface(event)) {
                    // find triggers
                    var triggerCount = this.triggers.length;
                    for (var i=0; i < triggerCount; i++) {
                        if (this.triggers[i].isTriggered(event) && this.triggers[i] instanceof triggerType) {
                            return this.triggers[i];
                        }
                    }
                }
                return retVal;
            },
            isFirstLeft: function() {return true},
            isLastRight: function() {return true},
            isFirstTop: function() {return true},
            isLastBottom: function() {return true},

            getFullWidth: function() {
                var retVal = 0;
                if (this.parent != null) {
                    var outerleft = this;
                    var outerright = this;
                    while (!this.isFirstLeft() && this.parent != null) {
                        outerleft = this.parent;
                    }
                    while (!this.isLastRight() && this.parent != null) {
                        outerright = this.parent;
                    }
                    retVal = outerright.right - outerleft.left;

                } else {
                    retVal = this.right - this.left;
                }
                return retVal;
            }
        })
        // A row inside a column or a container
// Can only contain columns
        var row = Class(layoutElement, {
            constructor: function(top, bottom, left, right, element, parent) {
                row.Super.call(this, top, bottom, left, right, element, parent);
                this.generateChildren();
                this.generateTriggers();
            },

            generateChildren: function() {
                // check only for columns
                var columns = this.element.children("." + _thisService.COLUMN_CLASS);
                var innerZone = new layoutElement(this.top + dropspot.WIDTH, this.bottom - dropspot.WIDTH, this.left, this.right, null);

                if (columns.length > 0) {
                    var columnCount = columns.length;
                    var oldColumn = null;
                    for (var i =0; i < columnCount; i++) {
                        // create zone for child
                        var currentColumn = $(columns[i]);
                        var zoneLeft = innerZone.left;
                        var zoneRight = innerZone.right;
                        if (i > 0) { // Not first column
                            // left side is between previous column and this column
                            var previousColumn = $(columns[i - 1]);
                            zoneLeft = (this.calculateRight(previousColumn) + this.calculateLeft(currentColumn)) / 2;
                        }
                        if (i < columnCount - 1) { // not last column
                            // right side is between next column and this column
                            var nextColumn = $(columns[i + 1]);
                            zoneRight = (this.calculateRight(currentColumn) + this.calculateLeft(nextColumn)) / 2;
                        }
                        var newColumn = new column(innerZone.top, innerZone.bottom, zoneLeft, zoneRight, currentColumn, this);
                        if (oldColumn != null) {
                            // add resizeHandle
                            this.triggers.push(new resizeHandle(oldColumn, newColumn));
                        }

                        this.children.push(newColumn);
                        oldColumn = newColumn;
                    }
                }
            },

            generateTriggers: function() {
                var triggerTop = new dropspot(this.top, this.top + dropspot.WIDTH, this.left, this.right, dropspot.SIDE.TOP, this);
                var triggerBottom = new dropspot(this.bottom - dropspot.WIDTH, this.bottom, this.left, this.right, dropspot.SIDE.BOTTOM, this);
                this.triggers.push(triggerTop);
                this.triggers.push(triggerBottom);
            },

            isFirstTop: function() {return this.element.prev().length == 0},
            isLastBottom: function() {return this.element.next().length == 0}


        })
        // special kind of row that defines the region where blocks can be dragged
        var container = Class(row, {
            constructor: function(element) {
                container.Super.call(this, this.calculateTop(element), this.calculateBottom(element), this.calculateLeft(element), this.calculateRight(element), element, null);
            }
        });
        // Special kind of row that can contain content
        var block = Class(row, {
            constructor: function(top, bottom, left, right, element, parent) {
                block.Super.call(this, top, bottom, left, right, element, parent);
                this.generateTriggers();
            },

            generateTriggers: function() {
                var triggerTop = new dropspot(this.top, this.top + dropspot.WIDTH, this.left, this.right, dropspot.SIDE.TOP, this);
                var triggerBottom = new dropspot(this.bottom - dropspot.WIDTH, this.bottom, this.left, this.right, dropspot.SIDE.BOTTOM, this);
                this.triggers.push(triggerTop);
                this.triggers.push(triggerBottom);

                if (this.element.siblings(".block").length > 0) {
                    var triggerLeft = new dropspot(this.top, this.bottom, this.left, this.left + dropspot.WIDTH, dropspot.SIDE.LEFT, this);
                    var triggerRight = new dropspot(this.top, this.bottom, this.right - dropspot.WIDTH, this.right, dropspot.SIDE.RIGHT, this);
                    this.triggers.push(triggerLeft);
                    this.triggers.push(triggerRight);
                }
            },

            isFirstTop: function() {return this.element.prev().length == 0},
            isLastBottom: function() {return this.element.next().length == 0}

        });

        // A column (inside a row) -> Can contain rows or blocks
        var column = Class(layoutElement, {
            constructor: function(top, bottom, left, right, element, parent) {
                column.Super.call(this, top, bottom, left, right, element, parent);
                this.generateChildren();
                this.generateTriggers();
            },

            generateChildren: function() {
                var childType = _thisService.ROW_CLASS;
                var rows = this.element.children("." + _thisService.ROW_CLASS);
                if (rows.length == 0) {
                    var rows = this.element.children("." + _thisService.BLOCK_CLASS);
                    var childType = _thisService.BLOCK_CLASS;
                }


                var innerZone = new layoutElement(this.top, this.bottom, this.left + dropspot.WIDTH, this.right - dropspot.WIDTH, null);
                if (this.element.siblings("." + _thisService.COLUMN_CLASS) > 0 ) {
                    innerZone.top = this.top + dropspot.WIDTH;
                    innerZone.bottom = this.bottom - dropspot.WIDTH;
                }

                if (rows.length > 0) {
                    var rowCount = rows.length;
                    for (var i =0; i < rowCount ; i++) {
                        // create zone for child
                        var currentRow = $(rows[i]);
                        var zoneTop = innerZone.top;
                        var zoneBottom = innerZone.bottom;
                        if (i > 0) {  // Not first row
                            var previousRow = $(rows[i - 1]);
                            zoneTop = (this.calculateTop(currentRow) + this.calculateBottom(previousRow)) / 2;
                        }
                        if (i < rowCount - 1) { // Not last row
                            var nextRow = $(rows[i + 1]);
                            zoneBottom = (this.calculateBottom(currentRow) + this.calculateTop(nextRow)) / 2;
                        }

                        if (childType == _thisService.ROW_CLASS) {
                            this.children.push(new row(zoneTop, zoneBottom, innerZone.left, innerZone.right, currentRow, this));
                        } else if (childType == _thisService.BLOCK_CLASS) {
                            this.children.push(new block(zoneTop, zoneBottom, innerZone.left, innerZone.right, currentRow, this));
                        }

                    }
                }
            },

            generateTriggers: function() {
                var triggerLeft = new dropspot(this.top, this.bottom, this.left, this.left + dropspot.WIDTH, dropspot.SIDE.LEFT, this);
                var triggerRight = new dropspot(this.top, this.bottom, this.right - dropspot.WIDTH, this.right, dropspot.SIDE.RIGHT, this);
                this.triggers.push(triggerLeft);
                this.triggers.push(triggerRight);

            },

            getColWidth: function() {
                var widths = this.element[0].className.match(/\bcol-md-\d+/g, '');
                if (widths != null && widths.length > 0) {
                    var nr = widths[0].substring(7, widths[0].length);
                    return parseInt(nr);
                } else {
                    Logger.error("Column. Could not get width of column");
                    return 0;
                }
            },

            setColumnWidth: function(newWidth) {
                var currentClass = _thisService.COLUMN_WIDTH_CLASS + this.getColWidth();
                var newClass = _thisService.COLUMN_WIDTH_CLASS + newWidth;
                this.element.removeClass(currentClass);
                this.element.addClass(newClass);

            },

            isFirstLeft: function() {return this.element.prev().length == 0},
            isLastRight: function() {return this.element.next().length == 0}
        })

        this.Surface = surface;
        this.DropSpot = dropspot;
        this.ResizeHandle = resizeHandle;
        this.LayoutElement = layoutElement;
        this.Row = row;
        this.Container = container;
        this.Block = block;
        this.Column = column;
    } ])

    .service("LayoutAnalyzer", ["Elements", function(Elements) {
        var _this = this;
        this.trees = [];

        this.generateHotspots = function() {
            // We create some sort of a heat map. We define boxes for all draggable blocks
            // we can add left and rigth from each column
            // and left and right from container if container has more than 1 row
            // select each row and add bottom
            // if row has +1 colunms, we can add also to bottom of columns
            // except if column has +1 rows

            Logger.debug("Calculate hotspots");
            var _this = this;
            _this.trees = [];
            //_this.cleanLayout();
            $("." + Elements.CAN_LAYOUT_CLASS).each(function() {
                var containerElement = $(this);
                // get size of container including border & padding but not margins
                var container = new Elements.Container(containerElement);
                Logger.debug(container);
                _this.trees.push(container);
            });
        };

        this.distributeColumns = function(element) {
            var columns = element.children("." + Elements.COLUMN_CLASS);
            var columnCount = columns.length;
            var firstColumn = Elements.MAX_COLUMNS % columnCount;
            var newWidth = Elements.MAX_COLUMNS;
            if (firstColumn == 0) {
                newWidth = Elements.MAX_COLUMNS / columnCount;
                firstColumn = newWidth;
            } else {
                newWidth = (Elements.MAX_COLUMNS - firstColumn) / columnCount
            }

            var newWidthClass= Elements.COLUMN_WIDTH_CLASS + (Elements.MAX_COLUMNS / columnCount)
            for (var i = 0; i < columnCount; i++) {
                var currWidth = columns[i].className.match(/\bcol-md-\d+/g, '');
                var currentColumn = $(columns[i]);
                if (currWidth != null && currWidth.length > 0) {
                    currWidth = currWidth[0].substring(7, currWidth[0].length);
                    currentColumn.removeClass(Elements.COLUMN_WIDTH_CLASS + currWidth);
                }
                if (i == 0) {
                    currentColumn.addClass(Elements.COLUMN_WIDTH_CLASS + firstColumn);
                } else {
                    currentColumn.addClass(Elements.COLUMN_WIDTH_CLASS + newWidth);
                }
            }
        }

        this.cleanLayout = function() {
            // find all columns
            // if column empty then remove -> parent row empty -> remove parent column empty -> remove ...
            // if column empty, remove, parent row not empty = redistribute columns
            // if column not empty and parent row has only 1 child - check if row has siblings
            // parent row no siblings then remove column and row and put column content in parent column
            // parent row has siblings -> check if any sibling has more then 1 column
            $("." + Elements.CAN_LAYOUT_CLASS).each(function() {
                var containerElement = $(this);
                var columns = containerElement.find("." + Elements.COLUMN_CLASS);
                var columnCount = columns.length;
                for (var i=0; i < columns.length; i++) {
                    var column = $(columns[i]);
                    column = deleteEmptyElements(column);
                    deleteOneInOne(column);
                }

            });

        };

        var deleteOneInOne = function(element) {
            var retVal = element;
            var tryDelete = true;
            while (tryDelete) {
                if (element.hasClass(Elements.COLUMN_CLASS)) {
                    if (element.parent().children().length == 1 && element.parent().parent().children().length == 1 && !element.parent().hasClass(Elements.CAN_LAYOUT_CLASS)) {
                        var parentColumn = element.parent().parent();
                        var children = element.children().remove();
                        parentColumn.children().remove();
                        parentColumn.append(children)
                        retVal = parentColumn;
                    }
                }
                if (retVal === element) tryDelete = false;
            }
            return retVal;
        }

        var deleteEmptyElements = function(element) {
            while (element.children().length == 0) {
                var parent = element.parent();
                element.remove();
                element = parent;
            }
            if (element.hasClass(Elements.ROW_CLASS)) {
                _this.distributeColumns(element);
            }
            return element;
        };

    }])

    .service("Layouter", ["Elements", "Broadcaster", "LayoutAnalyzer", function(Elements, Broadcaster, LayoutAnalyzer) {



        var appendElement = function(blockElement, dropLocationElement, side) {
            if (side == Elements.DropSpot.SIDE.RIGHT || side == Elements.DropSpot.SIDE.BOTTOM) {
                dropLocationElement.after(blockElement)
            } else if (side == Elements.DropSpot.SIDE.TOP || side == Elements.DropSpot.SIDE.LEFT) {
                dropLocationElement.before(blockElement)
            }

            if (dropLocationElement.hasClass(Elements.COLUMN_CLASS)) {
                LayoutAnalyzer.distributeColumns(dropLocationElement.parent());
            }

            if (blockElement.hasClass(Elements.BLOCK_CLASS)) {
                blockElement.fadeIn(200);
            } else {
                blockElement.find("." + Elements.BLOCK_CLASS).fadeIn(200);
            }
            Broadcaster.send("refreshLayout");
        };



        var removeBlock = function(block, side) {
            var blockElement = block.element;
            var blockParentColumn = block.parent;
            var blockParentRow = blockParentColumn.parent;

            blockElement = blockElement.remove();
            if (blockParentColumn.element.children().length == 0) {
                blockParentColumn.element.remove();
            }

            if (blockParentRow.element.children().length == 0 && !blockParentRow.element.hasClass("can-layout")) {
                blockParentRow.element.remove();
            } else {
                LayoutAnalyzer.distributeColumns(blockParentRow.element);
            }

            return blockElement;
        }

        var createRow = function() {
            return $("<div class='"+ Elements.ROW_CLASS +"'></div>")
        }

        var createColumn = function() {
            return $("<div class='" + Elements.COLUMN_CLASS + " " + Elements.COLUMN_WIDTH_CLASS + "12'></div>");
        }

        var wrapBlockInColumn = function(blockElement) {
            return createColumn().append(blockElement);
        }

        var wrapBlockInRow = function(blockElement) {
            return createRow().append(createColumn().append(blockElement));
        }

        var wrapChildBlocksInRows = function(blockElement) {
            parentColumnElement = blockElement.parent();
            if (parentColumnElement.hasClass(Elements.COLUMN_CLASS)) {
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

        var getDropLocationElement = function(dropLocation, side) {
           var dropLocationElement = dropLocation.element;
           if (dropLocation instanceof Elements.Block) {
               if (side == Elements.DropSpot.SIDE.LEFT || side == Elements.DropSpot.SIDE.RIGHT) {
                   wrapChildBlocksInRows(dropLocationElement.parent());
                   dropLocationElement = dropLocationElement.parent();
               }
           }
            return dropLocationElement;
        }

        this.changeBlockLocation = function(block, dropLocation, side) {
            block.element.fadeOut(200, function() {
                var blockElement = removeBlock(block);
                var dropLocationElement = getDropLocationElement(dropLocation);
                if (dropLocationElement.hasClass(Elements.COLUMN_CLASS)) {
                    blockElement = wrapBlockInColumn(blockElement);
                } else if (dropLocationElement.hasClass(Elements.ROW_CLASS)) {
                    blockElement = wrapBlockInRow(blockElement);
                } else if (dropLocationElement.hasClass(Elements.BLOCK_CLASS) && (side == Elements.DropSpot.SIDE.LEFT || side == Elements.DropSpot.SIDE.RIGHT)) {
                    dropLocationElement = wrapChildBlocksInRows(dropLocationElement);
                    blockElement = wrapBlockInColumn(blockElement);
                }
                appendElement(blockElement, dropLocationElement, side);
            })
        };


    }]);



blocks
    .plugin("blocks.core.Elements", ["blocks.core.Class", "blocks.core.Constants", function (Class, Constants) {

        var _thisService = this;

        var surface = Class.create({
            constructor: function (top, bottom, left, right) {
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

            isEventInSurface: function (event) {
                var retVal = false;
                if (this.top <= event.pageY && event.pageY <= this.bottom && this.left <= event.pageX && event.pageX <= this.right) {
                    retVal = true;
                }
                return retVal;
            }
        });

        var dropspot = Class.create({
            STATIC: {
                WIDTH: 20 //  defines the hotspot width on the side of the blocks
            },

            constructor: function (side, anchor) {
                this.anchor = anchor;
                this.side = side;
                this.min = 0;
                this.max = 0;
            },

            other: function() {
                var retVal = null;
                if (this.anchor != null) {
                    retVal = this.anchor.getElementAtSide(this.side);
                }
                return retVal;
            },

            setTrigger: function(min, max) {
                if (min < max) {
                    this.min = min;
                    this.max = max;
                } else {
                    this.max = min;
                    this.min = max;
                }
            },

            isTriggered: function (co) {
                var retVal = false;
                if (co > this.min && co < this.max) {
                    retVal = true;
                }
                return retVal;
            }

        });

        var resizeHandle = Class.create(surface, {
            STATIC: {
                DRAW_WIDTH: 5,
                TRIGGER_WIDTH: 20
            },

            constructor: function (leftColumn, rightColumn) {
                this.leftColumn = leftColumn;
                this.rightColumn = rightColumn;
                this.calculateSurface();
            },

            calculateSurface: function () {
                var middle = (this.leftColumn.calculateRight(this.leftColumn.element) + this.leftColumn.calculateLeft(this.rightColumn.element)) / 2;
                this.leftColumn.right = middle;
                this.rightColumn.left = middle;
                var t = Math.min(this.leftColumn.top, this.rightColumn.top);
                var b = Math.max(this.leftColumn.bottom, this.rightColumn.bottom);
                var l = ((this.leftColumn.right + this.rightColumn.left) / 2) - (resizeHandle.TRIGGER_WIDTH / 2);
                var r = ((this.leftColumn.right + this.rightColumn.left) / 2) + (resizeHandle.TRIGGER_WIDTH / 2);
                resizeHandle.Super.call(this, t, b, l, r);

                var l = ((this.leftColumn.right + this.rightColumn.left) / 2) - (resizeHandle.DRAW_WIDTH / 2);
                var r = ((this.leftColumn.right + this.rightColumn.left) / 2) + (resizeHandle.DRAW_WIDTH / 2);
                this.drawSurface = new surface(t, b, l, r);
            },

            isTriggered: function (event) {
                var retVal = false;
                if (this.isEventInSurface(event)) {
                    retVal = true;
                }
                return retVal;
            }

        });

        var layoutElement = Class.create(surface, {
            calculateTop: function (element) {
                return  element.offset().top
            },

            calculateBottom: function (element) {
                return element.offset().top + element.outerHeight()
            },

            calculateLeft: function (element) {
                return element.offset().left
            },

            calculateRight: function (element) {
                return element.offset().left + element.outerWidth()
            },

            top: 0,
            bottom: 0,
            left: 0,
            right: 0,
            element: null,
            parent: null,

            constructor: function (top, bottom, left, right, element, parent, index) {
                layoutElement.Super.call(this, top, bottom, left, right);
                this.parent = parent;
                this.index = index;
                this.element = element;
                this.children = [];
                this.resizeHandles = [];
            },

            findActiveElement: function (event) {
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

            findTriggeredResizeHandle: function (event, triggerType) {
                var retVal = null;
                if (this.isEventInSurface(event)) {
                    // find resizeHandles
                    var triggerCount = this.resizeHandles.length;
                    for (var i = 0; i < triggerCount; i++) {
                        if (this.resizeHandles[i].isTriggered(event) && this.resizeHandles[i] instanceof triggerType) {
                            return this.resizeHandles[i];
                        }
                    }
                }
                return retVal;
            },
            isOuterLeft: function () {return true},
            isOuterRight: function () { return true },
            isOuterTop: function () { return true },
            isOuterBottom: function () { return true },
            isOuter: function(side) {
                if (side == Constants.SIDE.TOP) {
                    return this.isOuterTop();
                } else if (side == Constants.SIDE.BOTTOM) {
                    return this.isOuterBottom();
                } else if (side == Constants.SIDE.LEFT) {
                    return this.isOuterLeft();
                } else if (side == Constants.SIDE.RIGHT) {
                    return this.isOuterRight();
                }
            },
            getElementAtSide: function(side) {
                if (side == Constants.SIDE.TOP || side == Constants.SIDE.LEFT) {
                    return this.getPrevious();
                } else {
                    return this.getNext();
                }
            },

            getNext: function() {
                if (this.parent == null) return null;
                if (this.index + 1 < this.parent.children.length) {
                    return this.parent.children[this.index + 1];
                } else {
                    return null;
                }
            },
            getPrevious: function() {
                if (this.parent == null) return null;
                if (this.index -1 >= 0) {
                    return this.parent.children[this.index - 1];
                } else {
                    return null;
                }
            },
            getFullWidth: function () {
                var retVal = 0;
                if (this.parent != null) {
                    var outerleft = this;
                    var outerright = this;
                    while (!this.isOuterLeft() && this.parent != null) {
                        outerleft = this.parent;
                    }
                    while (!this.isOuterRight() && this.parent != null) {
                        outerright = this.parent;
                    }
                    retVal = outerright.right - outerleft.left;

                } else {
                    retVal = this.right - this.left;
                }
                return retVal;
            },

            getDropspots: function(side, dropspots) {
                if (side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) {
                    if (this instanceof column ||
                        (this instanceof block &&
                            (this.element.next().length > 0 || this.element.prev().length > 0))) {
                        dropspots.push(new dropspot(side, this));
                    }
                } else {
                    if (this instanceof row || this instanceof block) {
                        dropspots.push(new dropspot(side, this));
                    }
                }
                if (this.isOuter(side) && this.parent != null) {
                    dropspots = this.parent.getDropspots(side, dropspots);
                }
                return dropspots;
            }
        });

        // A row inside a column or a container
        // Can only contain columns
        var row = Class.create(layoutElement, {
            constructor: function (top, bottom, left, right, element, parent, index) {
                row.Super.call(this, top, bottom, left, right, element, parent, index);
                this.generateChildren();
            },

            generateChildren: function () {
                // check only for columns
                var columns = this.element.children("." + Constants.COLUMN_CLASS);
                var innerZone = new layoutElement(this.top, this.bottom, this.left, this.right, null);

                if (columns.length > 0) {
                    var columnCount = columns.length;
                    var oldColumn = null;
                    for (var i = 0; i < columnCount; i++) {
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
                        var newColumn = new column(innerZone.top, innerZone.bottom, zoneLeft, zoneRight, currentColumn, this, i);
                        if (oldColumn != null) {
                            // add resizeHandle
                            this.resizeHandles.push(new resizeHandle(oldColumn, newColumn));
                        }

                        this.children.push(newColumn);
                        oldColumn = newColumn;
                    }
                }
            },

            isOuterTop: function () { return this.element.prev().length == 0 },
            isOuterBottom: function () { return this.element.next().length == 0 },

            getElementAtSide: function(side) {
                if (side == Constants.SIDE.TOP) {
                    return this.getPrevious();
                } else if (side == Constants.SIDE.BOTTOM) {
                    return this.getNext();
                } else {
                    return null;
                }
            }

        });

        // A column (inside a row) -> Can contain rows or blocks
        var column = Class.create(layoutElement, {
            constructor: function (top, bottom, left, right, element, parent, index) {
                column.Super.call(this, top, bottom, left, right, element, parent, index);
                this.generateChildren();
            },

            generateChildren: function () {
                var childType = Constants.ROW_CLASS;
                var rows = this.element.children("." + Constants.ROW_CLASS);
                if (rows.length == 0) {
                    rows = this.element.children("." + Constants.BLOCK_CLASS);
                    childType = Constants.BLOCK_CLASS;
                }

                var innerZone = new layoutElement(this.top, this.bottom, this.left, this.right, null);
//                if (this.element.siblings("." + Constants.COLUMN_CLASS) > 0) {
//                    innerZone.top = this.top + dropspot.WIDTH;
//                    innerZone.bottom = this.bottom - dropspot.WIDTH;
//                }

                if (rows.length > 0) {
                    var rowCount = rows.length;
                    for (var i = 0; i < rowCount; i++) {
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

                        if (childType == Constants.ROW_CLASS) {
                            this.children.push(new row(zoneTop, zoneBottom, innerZone.left, innerZone.right, currentRow, this, i));
                        } else if (childType == Constants.BLOCK_CLASS) {
                            this.children.push(new block(zoneTop, zoneBottom, innerZone.left, innerZone.right, currentRow, this, i));
                        }

                    }
                }
            },

            isOuterLeft: function () { return this.element.prev().length == 0 },
            isOuterRight: function () { return this.element.next().length == 0 },

            getElementAtSide: function(side) {
                if (side == Constants.SIDE.LEFT) {
                    return this.getPrevious();
                } else if (side == Constants.SIDE.RIGHT) {
                    return this.getNext();
                } else {
                    return null;
                }
            }
        });

        // special kind of row that defines the region where blocks can be dragged
        var container = Class.create(row, {
            constructor: function (element) {
                container.Super.call(this, this.calculateTop(element), this.calculateBottom(element), this.calculateLeft(element), this.calculateRight(element), element, null, 0);
            }
        });

        // Special kind of row that can contain content
        var block = Class.create(row, {
            constructor: function (top, bottom, left, right, element, parent, index) {
                block.Super.call(this, top, bottom, left, right, element, parent, index);
                this.dropspots = {};
                this.verticalMiddle = this.left + ((this.right - this.left) / 2);
                this.horizontalMiddle = this.top + ((this.bottom - this.top) / 2);
                this.generateDropspots();
            },

            generateDropspots: function () {
                this.dropspots = {};
                this.dropspots[Constants.SIDE.TOP] = this.getDropspots(Constants.SIDE.TOP, []);
                this.dropspots[Constants.SIDE.BOTTOM] = this.getDropspots(Constants.SIDE.BOTTOM, []);
                this.dropspots[Constants.SIDE.LEFT] = this.getDropspots(Constants.SIDE.LEFT, []);
                this.dropspots[Constants.SIDE.RIGHT] = this.getDropspots(Constants.SIDE.RIGHT, []);
                this.generateTriggers();
            },

            isOuterTop: function () {
                return this.element.prev().length == 0
            },
            isOuterBottom: function () {
                return this.element.next().length == 0
            },
            getElementAtSide: function(side) {
                if (side == Constants.SIDE.TOP) {
                    return this.getPrevious();
                } else if (side == Constants.SIDE.BOTTOM) {
                    return this.getNext();
                } else {
                    return null;
                }
            },

            generateTriggers: function() {
                this.generateTriggersForSide(Constants.SIDE.TOP);
                this.generateTriggersForSide(Constants.SIDE.BOTTOM);
                this.generateTriggersForSide(Constants.SIDE.LEFT);
                this.generateTriggersForSide(Constants.SIDE.RIGHT);
            },

            generateTriggersForSide: function(side) {
                var left, triggerWidth, triggers;
                triggers = [];
                if (side == Constants.SIDE.TOP) {
                    left = this.top;
                    triggerWidth = ((this.bottom - left) / 2) / this.dropspots[side].length;
                } else if (side == Constants.SIDE.BOTTOM) {
                    left = this.bottom;
                    triggerWidth = ((this.top - left) / 2) / this.dropspots[side].length;
                } else if (side == Constants.SIDE.LEFT) {
                    left = this.left;
                    triggerWidth = ((this.right - left) / 2) / this.dropspots[side].length;
                } else if (side == Constants.SIDE.RIGHT) {
                    left = this.right;
                    triggerWidth = ((this.left - left) / 2) / this.dropspots[side].length;
                }

                var t_right = left + (triggerWidth * this.dropspots[side].length);
                for (var i=0; i < this.dropspots[side].length ; i++) {
                    this.dropspots[side][i].setTrigger(t_right, t_right -= triggerWidth);
                }
            },

            getTriggeredHotspot: function(direction, event) {
                var side = null;
                var co = 0;
                if (direction == Constants.DIRECTION.UP || direction == Constants.DIRECTION.DOWN) {
                    co = event.pageY;
                    if (event.pageY < this.horizontalMiddle) {
                        side = Constants.SIDE.TOP;
                    } else {
                        side = Constants.SIDE.BOTTOM;
                    }
                } else if (direction == Constants.DIRECTION.LEFT || direction == Constants.DIRECTION.RIGHT) {
                    co = event.pageX;
                    if (event.pageX < this.verticalMiddle) {
                        side = Constants.SIDE.LEFT;
                    } else {
                        side = Constants.SIDE.RIGHT;
                    }
                }
                if (side != null) {
                    for (var i = 0; i < this.dropspots[side].length; i++) {
                        if (this.dropspots[side][i].isTriggered(co)) {
                            return this.dropspots[side][i];
                        }
                    }
                }
                return null;
            }



        });

        this.trees = [];


        this.generateHotspots = function () {
            // We create some sort of a heat map. We define boxes for all draggable blocks
            // we can add left and rigth from each column
            // and left and right from container if container has more than 1 row
            // select each row and add bottom
            // if row has +1 colunms, we can add also to bottom of columns
            // except if column has +1 rows

            Logger.debug("Calculate hotspots");
            _thisService.trees = [];
            //_this.cleanLayout();
            $("." + Constants.CAN_LAYOUT_CLASS).each(function () {
                var containerElement = $(this);
                // get size of container including border & padding but not margins
                var container = new _thisService.Container(containerElement);
                Logger.debug(container);
                _thisService.trees.push(container);
            });
        };

        this.Surface = surface;
        this.DropSpot = dropspot;
        this.ResizeHandle = resizeHandle;
        this.LayoutElement = layoutElement;
        this.Row = row;
        this.Container = container;
        this.Block = block;
        this.Column = column;
    } ]);

/*
* Classes for all layout types and surface
* Layouter builds a virtual tree from these objects for easy searching and triggering
* and to take some load of the dom while calculating
*
*
* */
blocks
    .plugin("blocks.core.Elements", ["blocks.core.Class", "blocks.core.Constants", "blocks.core.DomManipulation", function (Class, Constants, DOM) {

        // Define a has Attribute function for jquery
        $.fn.hasAttribute = function(name) {
            return this.attr(name) !== undefined;
        };


        // smallest elemet with 4 corner
        // and a function to check if x,y is inside the surface
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

            isTriggered: function (x, y) {
                var retVal = false;
                if (this.top <= y && y <= this.bottom && this.left <= x && x <= this.right) {
                    retVal = true;
                }
                return retVal;
            }
        });

        /*
        * Special element that indicates a trigger where a block could be dropped
        * on an other block. the dropspot is located on a SIDE of the block and the
        * MIN and MAX value that defines the area on that side
        * e.g. side = TOP and min =0 and max= 10 then this dropspot will be triggered with an
        * y coordinate of 6
        *
        * */
        var dropspot = Class.create({

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

            // co is an x or y value that must be between min and max
            isTriggered: function (co) {
                var retVal = false;
                if (co >= this.min && co <= this.max) {
                    retVal = true;
                }
                return retVal;
            }

        });

        /*
        * defines a resizehandle. The surface of the resizeHandle is the are that triggers when you hoover over it
        * the draw-surface is the surface that will be drawn in the dom (can be bigger or smaller).
        * left and rightcolumn are the columns that this handle will resize when dragged
        */
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

            // calculate location by location of left and right column
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
            }

        });

        /*
        * Is the abstract class for DOM elements (row, container, block)
        * contains some helperfunctions
        * */
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
            canLayout: false,
            canDrag: false,
            canEdit: false,

            constructor: function (top, bottom, left, right, element, parent, index) {
                layoutElement.Super.call(this, top, bottom, left, right);
                this.parent = parent;
                this.index = index;
                this.element = element;
                this.children = [];
                this.resizeHandles = [];
                this.canDrag = false;
                if (element != null) {
                    this.canEdit = DOM.canEdit(element);
                    if (this.parent != null) {
                        if (this.parent.canLayout || this.parent.canDrag) {
                            this.canDrag = true;
                        }
                    } else {
                        this.canLayout = DOM.canLayout(element);
                        if (this.canLayout) {
                            this.canDrag = true;
                        }
                    }
                }
            },

            // Easily walk the tree and find the block that contains the coordinates
            findActiveElement: function (x, y) {
                var retVal = null;
                if (this.isTriggered(x, y)) {
                    var i = 0;
                    while (retVal == null && i < this.children.length) {
                        retVal = this.children[i].findActiveElement(x, y);
                        i++;
                    }
                    if (retVal == null) {
                        retVal = this;
                    }
                }
                return retVal;
            },

            // find resizehandle that contains coordinates
            findTriggeredResizeHandle: function (x, y) {
                var retVal = null;
                if (this.isTriggered(x, y)) {
                    // find resizeHandles
                    var triggerCount = this.resizeHandles.length;
                    for (var i = 0; i < triggerCount; i++) {
                        if (this.resizeHandles[i].isTriggered(x, y)) {
                            return this.resizeHandles[i];
                        }
                    }
                }
                return retVal;
            },
            // returns true if this block has no sibling on his left/right/top/bottom
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

            // Get element at side, general function for getNext, getPrevious
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

            // TODO: check if deprecated
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

            // find all dropspots for an element
            // is called for a block and returns all dropspots for this block and his parents.
            calculateDropspots: function(side, dropspots) {
                if (this instanceof column) {
                    var x = 0;
                }

                if (side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) {
                    if (this instanceof column ||
                        (this instanceof block &&
                            (this.element.next().length > 0 || this.element.prev().length > 0))) {
                        dropspots.push(new dropspot(side, this));
                    }
                } else { // TOP & BOTTOM
                    if (this instanceof row || this instanceof block) {
                        dropspots.push(new dropspot(side, this));
                    }
                }
                if (this.isOuter(side) && this.parent != null) {
                    dropspots = this.parent.calculateDropspots(side, dropspots);
                }
                return dropspots;
            },


            generateChildrenForColumn: function () {
                var childType = Constants.ROW_CLASS;
                var rows = this.element.children("." + Constants.ROW_CLASS);
                if (rows.length == 0) {
                    var rows = this.element.children();
                    var blocks = true;
                    for (var x=0; i < rows.length; i++) {
                        if ($(rows[x].css('display') !== 'block')) {
                            blocks = fals;
                            rows = [];
                            break;
                        }
                    }
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

            generateChildrenForRow: function () {
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
            }

        });

        // A row inside a column or a container
        // Can only contain columns
        var row = Class.create(layoutElement, {

            constructor: function (top, bottom, left, right, element, parent, index) {
                row.Super.call(this, top, bottom, left, right, element, parent, index);

                if (!(this instanceof container) && (element.hasAttribute(Constants.IS_ENTITY) || element.hasAttribute(Constants.IS_PROPERTY)) && !element.hasAttribute(Constants.FAKE_BLOCK)) {
                    element.attr(Constants.FAKE_BLOCK, "");
                    var newBlock = new block(top, bottom, left, right, element, this.parent, 0);
                    this.children.push(newBlock);
                } else {
                    this.generateChildrenForRow();
                }
            },


            isOuterTop: function () { return this.element.prev().length == 0 },
            isOuterBottom: function () { return this.element.next().length == 0 },

            // Override
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
                // will be set to true if parent is true

                if (!(this instanceof container) && (element.hasAttribute(Constants.IS_ENTITY) || element.hasAttribute(Constants.IS_PROPERTY)) && !element.hasAttribute(Constants.FAKE_BLOCK)) {
                    element.attr(Constants.FAKE_BLOCK, "");
                    var newBlock = new block(top, bottom, left, right, element, this.parent, 0);
                    this.children.push(newBlock);
                } else {
                    this.generateChildrenForColumn();
                }
            },

            isOuterLeft: function () { return this.element.prev().length == 0 },
            isOuterRight: function () { return this.element.next().length == 0 },

            // Override
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
                if (DOM.isColumn(element) || DOM.isContainer(element)) {
                    this.generateChildrenForColumn();
                } else if (DOM.isRow(element)) {
                   this.generateChildrenForRow();
                }
            }
        });

        // Special kind of row that can contain template
        var block = Class.create(row, {
            constructor: function (top, bottom, left, right, element, parent, index) {
                block.Super.call(this, top, bottom, left, right, element, parent, index);
                element.removeAttr(Constants.FAKE_BLOCK);
                // if a block is editable does not depend on the parent

                this.children = [];
                // Only generate dropspots if one of parents is layoutable
                this.dropspots = {};
                if (this.canDrag) {
                    this.verticalMiddle = this.left + ((this.right - this.left) / 2);
                    this.horizontalMiddle = this.top + ((this.bottom - this.top) / 2);
                    this.generateDropspots();
                }
            },

            // gets all dropspots for this block and his parents, for each side
            // then generate the triggers (surfaces) for each dropspot
            generateDropspots: function () {
                this.dropspots = {};
                this.dropspots[Constants.SIDE.TOP] = this.calculateDropspots(Constants.SIDE.TOP, []);
                this.dropspots[Constants.SIDE.BOTTOM] = this.calculateDropspots(Constants.SIDE.BOTTOM, []);
                this.dropspots[Constants.SIDE.LEFT] = this.calculateDropspots(Constants.SIDE.LEFT, []);
                this.dropspots[Constants.SIDE.RIGHT] = this.calculateDropspots(Constants.SIDE.RIGHT, []);
                this.generateTriggers();
            },

            isOuterTop: function () {
                var retVal = true;
                if (!DOM.isColumn(this.element)) {
                    retVal = this.element.prev().length == 0
                }
                return retVal;
            },
            isOuterBottom: function () {
                var retVal = true;
                if (!DOM.isColumn(this.element)) {
                    retVal = this.element.next().length == 0
                }
                return retVal;
            },

            isOuterLeft: function () {
                var retVal = true;
                if (DOM.isColumn(this.element)) {
                    retVal = this.element.prev().length == 0
                }
                return retVal;
            },
            isOuterRight: function () {
                var retVal = true;
                if (DOM.isColumn(this.element)) {
                    retVal = this.element.next().length == 0
                }
                return retVal;
            },

            getElementAtSide: function(side) {
                // TODO if block is column, this is not correct
                if (DOM.isColumn(this.element)) {
                    if (side == Constants.SIDE.LEFT) {
                        return this.getPrevious();
                    } else if (side == Constants.SIDE.RIGHT) {
                        return this.getNext();
                    } else {
                        return null;
                    }
                } else if (side == Constants.SIDE.TOP) {
                    return this.getPrevious();
                } else if (side == Constants.SIDE.BOTTOM) {
                    return this.getNext();
                } else {
                    return null;
                }
            },

            // calculates the triggers (surface where mouse coordinates trigger a dropspot)
            generateTriggers: function() {
                this.generateTriggersForSide(Constants.SIDE.TOP);
                this.generateTriggersForSide(Constants.SIDE.BOTTOM);
                this.generateTriggersForSide(Constants.SIDE.LEFT);
                this.generateTriggersForSide(Constants.SIDE.RIGHT);
            },

            // generate the triggers for the dropspots
            // e.g. 3 dropspots left =  width of trigger = half width of block / 3
            // then set triggers for dropspot. the most left dropspot is dropspot for deepest parent
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
                    // set min and max for trigger = trigger is zone between min x and max x (left/right) OR min y and max y (top/bottom)
                    this.dropspots[side][i].setTrigger(t_right, t_right -= triggerWidth);
                }
            },

            // Find the dropspot that is triggered for x and y
            getTriggeredDropspot: function(direction, x, y) {
                var side = null;
                var co = 0;
                if (direction == Constants.DIRECTION.UP || direction == Constants.DIRECTION.DOWN) {
                    co = y;
                    if (y < this.horizontalMiddle) {
                        side = Constants.SIDE.TOP;
                    } else {
                        side = Constants.SIDE.BOTTOM;
                    }
                } else if (direction == Constants.DIRECTION.LEFT || direction == Constants.DIRECTION.RIGHT) {
                    co = x;
                    if (x < this.verticalMiddle) {
                        side = Constants.SIDE.LEFT;
                    } else {
                        side = Constants.SIDE.RIGHT;
                    }
                }
                if (side != null && this.dropspots[side] != null) {
                    for (var i = 0; i < this.dropspots[side].length; i++) {
                        if (this.dropspots[side][i].isTriggered(co)) {
                            return this.dropspots[side][i];
                        }
                    }
                }
                return null;
            }
        });



        this.Surface = surface;
        this.ResizeHandle = resizeHandle;
        this.Row = row;
        this.Container = container;
        this.Block = block;
        this.Column = column;
    } ]);
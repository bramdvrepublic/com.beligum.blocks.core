
/*
 * Classes for all layout types and surface
 * Layouter builds a virtual tree from these objects for easy searching and triggering
 * and to take some load of the dom while calculating
 *
 *
 * */
blocks
    .plugin("blocks.core.Elements", ["blocks.core.Class", "blocks.core.Constants", "blocks.core.DomManipulation", "blocks.core.Edit", function (Class, Constants, DOM, Edit) {

        // Define a has Attribute function for jquery
        $.fn.hasAttribute = function(name) {
            return this.attr(name) !== undefined;
        };

        blocks.elements = {};



        // smallest elemet with 4 corner
        // and a function to check if x,y is inside the surface
        var surface = Class.create({
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

            constructor: function (side, anchor, index) {
                this.block = null;
                this.anchor = anchor;
                this.index = index;
                this.side = side;
                this.other = this.setOther();
                this.min = 0;
                this.max = 0;
                if (this.anchor.element.hasClass("column") && this.other != null) {
                    Logger.debug(this);
                }
            },

            setOther: function() {
                var retVal = null;
                if (this.anchor != null) {
                    retVal = this.anchor.getElementAtSide(this.side);
                }
                return retVal;
            },

            makeTriggers: function(x, y, direction) {
                var BORDER_THRESHOLD = 15;
                if (this.side != direction && this.side != Constants.OPPOSITE_DIRECTION[direction]) {
                    Logger.debug("exit because droploc not on this side " + direction);
                    return false;
                }
                var left = 0;
                var right = 0;
                var current = 0;
                var prevLength = this.index - 1 < 0? 0 : this.index - 1;
                var nrDropspots = 0;
                var dropspots = []
                if (direction == Constants.SIDE.TOP || direction == Constants.SIDE.BOTTOM) {
                    left = this.block.top;
                    right = this.block.bottom;
                    current = y;
//                    var prev = this.anchor.verticalDropspots.slice(0, this.index);
//                    var next = this.anchor.verticalDropspots.slice(this.index + 1, this.anchor.verticalDropspots.length);
                    nrDropspots = this.block.verticalDropspots.length;
                    dropspots = this.block.verticalDropspots;

                } else {
                    left = this.block.left;
                    right = this.block.right;
                    current = x;
//                    var prev = this.anchor.horizontalDropspots.slice(0, this.index);
//                    var next = this.anchor.horizontalDropspots.slice(this.index + 1, this.anchor.horizontalDropspots.length);
                    nrDropspots = this.block.horizontalDropspots.length;
                    dropspots = this.block.horizontalDropspots;
                }

                var border_threshold = Math.round((right - left) / nrDropspots);
                if (border_threshold > BORDER_THRESHOLD) border_threshold = BORDER_THRESHOLD;
                var inner_threshold = (right - left) - (border_threshold * (nrDropspots - 1));

//                var min = (current - ((threshold * prevLength) + (threshold/2)));
//                if (min < left) min = left;
//                if (right < (min + (threshold * nrDropspots))) {
//                    min = right - (threshold * nrDropspots);
//                }

                var i = 0;
                var min = left;
                while ( i < nrDropspots) {
                    var currentDP = dropspots[i];
                    currentDP.setTrigger(min, min+border_threshold);

                    if (i + 1 < nrDropspots && (dropspots[i+1].side != currentDP.side)) {
                        if (currentDP.side == direction) {
                            currentDP.setTrigger(min, min + inner_threshold);
                        } else if (dropspots[i+1].side == direction) {
                            min += border_threshold;
                            dropspots[i+1].setTrigger(min, min+inner_threshold);
                            i++;
                        }
                        min += inner_threshold
                    } else {
                        min += border_threshold
                    }

                    i++;
                }
                dropspots[0].min = left;
                dropspots[dropspots.length -1].max = right;
                return true;
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
                DRAW_WIDTH: 30,
                TRIGGER_WIDTH: 40
            },

            constructor: function (leftColumn, rightColumn) {
                this.leftColumn = leftColumn;
                this.rightColumn = rightColumn;
                this.updateSurface();

            },

            // calculate location by location of left and right column
            calculateSurface: function (t, b, left, right) {
                var l = left - resizeHandle.TRIGGER_WIDTH;
                var r = right + resizeHandle.TRIGGER_WIDTH;
                resizeHandle.Super.call(this, t, b, l, r);
//                var l = (middle) - (resizeHandle.DRAW_WIDTH / 2);
//                var r = (middle) + (resizeHandle.DRAW_WIDTH / 2);

                this.drawSurface = new surface(t, b, l, r);
            },

            updateSurface: function() {
                if (this.leftColumn == null) {
                    this.calculateSurface(this.rightColumn.top, this.rightColumn.bottom, this.rightColumn.left, this.rightColumn.left);
                }
                else if (this.rightColumn == null) {
                    this.calculateSurface(this.leftColumn.top, this.leftColumn.bottom, this.leftColumn.right, this.leftColumn.right);
                }
                else {

                    this.calculateSurface(Math.min(this.leftColumn.top, this.rightColumn.top),  Math.max(this.leftColumn.bottom, this.rightColumn.bottom), this.leftColumn.calculateRight(this.leftColumn.element), this.leftColumn.calculateLeft(this.rightColumn.element));
                }
            }

        });

        /*
         * Is the abstract class for DOM elements (row, container, block)
         * contains some helperfunctions
         * */
        var layoutElement = Class.create(surface, {


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
                this.el = {top:0, bottom:0, left:0, right:0};
                if (element != null) {
                    this.el = {
                        top: element.offset().top,
                        bottom: element.offset().top + element.height(),
                        left: element.offset().left,
                        right: element.offset().left + element.width()
                    };
                }

                this.parent = parent;
                this.index = index;
                this.element = element;
                this.children = [];
                this.resizeHandles = [];
                this.totalBlocks = null;
                this.canLayout = false;
                this.canDrag = false;
                this.canEdit = false;

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
            createAllDropspots: function() {
                if (this instanceof block) {
                    this.generateDropspots();
                } else {
                    if (this.children.length > 0) {
                        for (var i=0; i < this.children.length; i++) {
                            this.children[i].createAllDropspots();
                        }
                    }
                }
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
//                var columns = this.element.children("." + Constants.COLUMN_CLASS);
                var tColumns = this.element.children();
                var columns = [];

                for (var i=0; i < tColumns.length; i++ ) {
                    if (DOM.isColumn($(tColumns[i]))) {
                        columns.push($(tColumns[i]));
                    }
                }
                var innerZone = new layoutElement(this.top, this.bottom, this.left, this.right, null);

                if (columns.length > 0) {
                    var rowWidth = 0;
                    for (var x=0; x < columns.length; x++) {
                        rowWidth += DOM.getColumnWidth($(columns[x]));
                    }

                    // Variables to keep track when columns stack vertical
                    var prevColsWidth = 0;
                    var prevColBottom = innerZone.top;
                    var prevColMaxBottom = 0;

                    var columnCount = columns.length;
                    var oldColumn = null;
                    for (var i = 0; i < columnCount; i++) {
                        // create zone for child
                        var currentColumn = $(columns[i]);
                        var newColumn = null;
                        if (rowWidth <= 12) {
                            var zoneLeft = innerZone.left;
                            var zoneRight = innerZone.right;
                            if (i > 0) { // Not first column
                                // left side is between previous column and this column
                                var previousColumn = $(columns[i - 1]);
//                                zoneLeft = (this.calculateRight(previousColumn) + this.calculateLeft(currentColumn)) / 2;
                                zoneLeft = this.calculateLeft(currentColumn);
                            }
                            if (i < columnCount - 1) { // not last column
                                // right side is between next column and this column
                                var nextColumn = $(columns[i + 1]);
//                                zoneRight = (this.calculateRight(currentColumn) + this.calculateLeft(nextColumn)) / 2;
                                zoneRight = this.calculateRight(currentColumn);
                            }


                            newColumn = new column(innerZone.top, innerZone.bottom, zoneLeft, zoneRight, currentColumn, this, i);

                            var outside = this.parent != null && this.parent.parent != null &&  this.parent.parent instanceof container

                            if (oldColumn != null) {
                                this.resizeHandles.push(new resizeHandle(oldColumn, newColumn));
                            } else if (outside) {
                                this.resizeHandles.push(new resizeHandle(oldColumn, newColumn));
                            }


                            if (outside && i == columnCount - 1) {
                                this.resizeHandles.push(new resizeHandle(newColumn, null));
                            }
//
                        } else {
                            var colWidth = DOM.getColumnWidth(currentColumn);
                            prevColsWidth += colWidth;
                            if (prevColsWidth > 12) {
                                prevColsWidth = colWidth;
                                prevColBottom += prevColMaxBottom;
                                prevColMaxBottom = 0;
                            }
                            var curBottom = this.calculateBottom(currentColumn);
                            prevColMaxBottom = prevColMaxBottom < curBottom ? curBottom : prevColMaxBottom;

                            newColumn = new column(this.calculateTop(currentColumn), this.calculateBottom(currentColumn), this.calculateLeft(currentColumn), this.calculateRight(currentColumn), currentColumn, this, i);

                        }



                        this.children.push(newColumn);
                        oldColumn = newColumn;
                    }
                }
            },

            getBlocks: function() {
                if (this.totalBlocks != null) return this.totalBlocks;
                this.totalBlocks = 0;
                for (var i=0; i < this.children.length; i++) {
                    if (this.children[i] instanceof  block) {
                        this.totalBlocks += 1;
                    } else {
                        this.totalBlocks += this.children[i].getBlocks();
                    }
                }
                return this.totalBlocks;
            }



        });

        // A row inside a column or a container
        // Can only contain columns
        var row = Class.create(layoutElement, {

            constructor: function (top, bottom, left, right, element, parent, index) {
                row.Super.call(this, top, bottom, left, right, element, parent, index);
                this.generateChildrenForRow();
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
            },

            // find all dropspots for an element
            // is called for a block and returns all dropspots for this block and his parents.
            calculateDropspots: function(side, dropspots) {
                var isOuter = this.isOuter(side);
                if ((side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) && this.children.length > 1) {
                    dropspots.push(new dropspot(side, this, dropspots.length));
                }

                if (this.isOuter(side) && this.parent != null) dropspots = this.parent.calculateDropspots(side, dropspots);
                return dropspots;
            }

        });

        // A column (inside a row) -> Can contain rows or blocks
        var column = Class.create(layoutElement, {
            constructor: function (top, bottom, left, right, element, parent, index) {
                column.Super.call(this, top, bottom, left, right, element, parent, index);

                this.generateChildrenForColumn();
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
            },

            // find all dropspots for an element
            // is called for a block and returns all dropspots for this block and his parents.
            calculateDropspots: function(side, dropspots) {
                if ((side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT)) {
                    dropspots.push(new dropspot(side, this, dropspots.length));
                }

                if (this.isOuter(side) && this.parent != null) {
                    dropspots = this.parent.calculateDropspots(side, dropspots);
                }
                return dropspots;
            }
        });

        var property = Class.create(layoutElement, {
            constructor: function (element, parent) {
                property.Super.call(this, this.calculateTop(element), this.calculateBottom(element), this.calculateLeft(element), this.calculateRight(element), element, parent, 0);
                this.container = null;
                if (DOM.canEdit(element)) {
                    this.canEdit = true;
                }
                this.container = new container(element, this);
            },

            findActiveElement: function (x, y) {
                var retVal = null;
                if (this.isTriggered(x, y)) {
                    retVal = this.container.findActiveElement(x, y);
                    if (retVal == null || retVal == this.container) {
                        retVal = this;
                    }
                }
                return retVal;
            }

        });

        // special kind of row that defines the region where blocks can be dragged
        var container = Class.create(layoutElement, {
            constructor: function (element, parent) {
                container.Super.call(this, this.calculateTop(element), this.calculateBottom(element), this.calculateLeft(element), this.calculateRight(element), element, parent, 0);
                this.blocks = [];
                if (DOM.canLayout(element)) {
                    this.canLayout = true;
                    this.generateChildrenForColumn();
                } else {
                    for (var i=0; i < this.element.children().length; i++) {
                        this.generateProperties($(this.element.children()[i]));
                    }
                }
            },

            getElementAtSide: function(side) {
                return null;
            },

            calculateDropspots: function(side, dropspots) {
                if ((side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) && this.children.length > 1) {
                    dropspots.push(new dropspot(side, this, dropspots.length));
                }

                return dropspots;
            },

//
//            getProperty: function(x, y) {
//                for(var i=0; i< this.properties.length; i++) {
//                    if (this.properties[i].isTriggered(x, y)) {
//                        return this.properties[i];
//                    }
//                }
//                return null;
//            },

            generateProperties: function(element) {
                var prop = null;
                var canChange = DOM.canEdit(element) || DOM.canLayout(element)

                if (DOM.canLayout(element)) {
                    prop = new property(element, this);
                } else if (DOM.canEdit(element)) {
                    prop = new property(element, this);
//                    Edit.makeEditable(element);
                }

//                if ((canLayout || prop == null) && element.children().length > 0) {
                if ((prop == null) && element.children().length > 0) {
                    for(var i=0; i < element.children().length; i++) {
                        this.generateProperties($(element.children()[i]));
                    }
                }

                if (prop != null) {
                    this.children.push(prop);
                }
            }

        });

        // Special kind of row that can contain template
        var block = Class.create(layoutElement, {
            constructor: function (top, bottom, left, right, element, parent, index) {
                block.Super.call(this, top, bottom, this.calculateLeft(element), this.calculateRight(element), element, parent, index);
                // if a block is editable does not depend on the parent

                this.dropspots = {};
                if (this.canDrag) {
                    this.verticalMiddle = this.left + ((this.right - this.left) / 2);
                    this.horizontalMiddle = this.top + ((this.bottom - this.top) / 2);
                }
                var ct = this.getContainer()
                ct.blocks.push(this);
                this.canDrag = ct.canLayout;
                this.container = new container(this.element, this);
            },

            findActiveElement: function (x, y) {
                var retVal = null;
                if (this.isTriggered(x, y)) {
                    retVal = this.container.findActiveElement(x, y);
                    if (retVal == null || retVal == this.container) {
                        retVal = this;
                    }
                }
                return retVal;
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
                this.horizontalDropspots = [];
                this.verticalDropspots = [];
//                this.generateTriggersForSide(Constants.SIDE.TOP);
//                this.generateTriggersForSide(Constants.SIDE.BOTTOM);
//                this.generateTriggersForSide(Constants.SIDE.LEFT);
//                this.generateTriggersForSide(Constants.SIDE.RIGHT);

                var i= 0;
                for (i=this.dropspots[Constants.SIDE.TOP].length - 1; i >=0; i--) {
                    this.verticalDropspots.push(this.dropspots[Constants.SIDE.TOP][i]);
                }
                for (i=0; i < this.dropspots[Constants.SIDE.BOTTOM].length; i++) {
                    this.verticalDropspots.push(this.dropspots[Constants.SIDE.BOTTOM][i]);
                }

                for (i=this.dropspots[Constants.SIDE.LEFT].length - 1; i >=0; i--) {
                    this.horizontalDropspots.push(this.dropspots[Constants.SIDE.LEFT][i]);
                }
                for (i=0; i < this.dropspots[Constants.SIDE.RIGHT].length; i++) {
                    this.horizontalDropspots.push(this.dropspots[Constants.SIDE.RIGHT][i]);
                }
                for (i=0; i < this.horizontalDropspots.length; i++) {
                    this.horizontalDropspots[i].index = i;
                    this.horizontalDropspots[i].block = this;
                }

                for (i=0; i < this.verticalDropspots.length; i++) {
                    this.verticalDropspots[i].index = i;
                    this.verticalDropspots[i].block = this;
                }
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
//                for (var i=this.dropspots[side].length - 1; i >= 0 ; i--) {
                    // set min and max for trigger = trigger is zone between min x and max x (left/right) OR min y and max y (top/bottom)
                    this.dropspots[side][i].setTrigger(t_right, t_right -= triggerWidth);

                }
            },

            // Find the dropspot that is triggered for x and y
            getTriggeredDropspot: function(direction, x, y) {
                var side = direction;
                var co = 0;
                var dp = [];
                if (direction == Constants.DIRECTION.UP || direction == Constants.DIRECTION.DOWN) {
                    co = y;
                    dp = this.verticalDropspots;
//                    if (y < this.horizontalMiddle) {
//                        side = Constants.SIDE.TOP;
//                    } else {
//                        side = Constants.SIDE.BOTTOM;
//                    }
                } else if (direction == Constants.DIRECTION.LEFT || direction == Constants.DIRECTION.RIGHT) {
                    co = x;
                    dp = this.horizontalDropspots;
//                    if (x < this.verticalMiddle) {
//                        side = Constants.SIDE.LEFT;
//                    } else {
//                        side = Constants.SIDE.RIGHT;
//                    }
                }
                if (dp != null) {
                    for (var i = 0; i < dp.length; i++) {
                        if (dp[i].isTriggered(co)) {
//                            Logger.debug("find triggered hotspot: " + dp[i]["min"] + " - " + dp[i]["max"]);
                            return dp[i];
                        }
                    }
                }
                return null;

            },

            recalculateTriggers: function(direction, x, y, currentDropspot) {
                Logger.debug("Recalculate triggers");
                if (currentDropspot == null || !currentDropspot.makeTriggers(x, y, direction)) {
                    var newDropspot = currentDropspot;
                    try {
                        if (direction == Constants.DIRECTION.UP) {
                            newDropspot = this.verticalDropspots[this.verticalDropspots.length - 1];
                        } else if (direction == Constants.DIRECTION.DOWN) {
                            newDropspot = this.verticalDropspots[0];
                        } else if (direction == Constants.DIRECTION.LEFT) {
                            newDropspot = this.horizontalDropspots[this.horizontalDropspots.length - 1];
                        } else if (direction == Constants.DIRECTION.RIGHT) {
                            newDropspot = this.horizontalDropspots[0];
                        }
                    } catch (e) {
                        Logger.error(this);
                    }
                    Logger.debug("Calculate at border");
                    if (newDropspot != null) {
                        newDropspot.makeTriggers(x, y, direction);
                    }
                }
            },

            calculateDropspots: function(side, dropspots) {
                if (side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) {
                    dropspots.push(new dropspot(side, this, dropspots.length));
                } else if (this.element.siblings().length > 0) {
                    dropspots.push(new dropspot(side, this, dropspots.length));
                }

                if (this.isOuter(side) && this.parent != null) dropspots = this.parent.calculateDropspots(side, dropspots);
                return dropspots;
            },

            getContainer: function() {
                var parent = this.parent;
                while (parent != null && !(parent instanceof blocks.elements.Container)) {
                    parent = parent.parent;
                }
                return parent;
            },

            getTotalBlocks: function() {
                var c = this.getContainer();
                return c.getBlocks();
            }

        });


        blocks.elements.ResizeHandle = resizeHandle;
        blocks.elements.Row = row;
        blocks.elements.Container = container;
        blocks.elements.Block = block;
        blocks.elements.Column = column;
        blocks.elements.Property = property;
    } ]);
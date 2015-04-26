/**
 * Created by wouter on 5/03/15.
 */

/*
 * Is the abstract class for DOM elements (row, container, block)
 * contains some helperfunctions
 * */


base.plugin("blocks.core.Elements.LayoutElement", ["base.core.Class", "blocks.core.Constants", "blocks.core.DomManipulation", function (Class, Constants, DOM)
{
    var body = $("body");

    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    blocks.elements.LayoutElement = Class.create(blocks.elements.Surface, {
        top: 0,
        bottom: 0,
        left: 0,
        right: 0,
        element: null,
        parent: null,
        canLayout: false,
        canDrag: false,
        canEdit: false,

        constructor: function (element, parent, index)
        {
            this.parent = parent;
            blocks.elements.LayoutElement.Super.call(this, this.calculateTop(element, true), this.calculateBottom(element, true), this.calculateLeft(element, true), this.calculateRight(element, true));

            this.el = {top: 0, bottom: 0, left: 0, right: 0};
            if (element != null) {
                this.el = {
                    top: element.offset().top,
                    bottom: element.offset().top + element.height(),
                    left: element.offset().left,
                    right: element.offset().left + element.width()
                };
            }

            //// If we are the only child in the parent then our dimensions are the same as the dimensions of the parent
            //if (parent != null && !(this instanceof templates.elements.Container)) {
            //    var prev = element.parent();
            //    while (prev.siblings().length == 0 && prev[0] != parent.element[0]) {
            //        prev = prev.parent();
            //    }
            //    if (prev[0] != parent.element[0]) {
            //        this.top = parent.top;
            //        this.left = parent.left;
            //        this.bottom = parent.bottom;
            //        this.right = parent.right;
            //    }
            //}

            this.index = index; // index in the parent
            this.element = element; // jquery element
            this.children = [];
            this.resizeHandles = [];
            this.totalBlocks = null;
            this.canLayout = false;
            this.canDrag = false;
            this.canEdit = false;
            this.overlay = null;
            this.isEntity = false;


        },

        // Easily walk the tree and find the block that contains the coordinates
        findActiveElement: function (x, y, maxSearchLevel)
        {
            maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
            var retVal = null;
            if (this.isTriggered(x, y)) {
                var i = 0;
                if (maxSearchLevel != 0) {
                    while (retVal == null && i < this.children.length) {
                        retVal = this.children[i].findActiveElement(x, y, maxSearchLevel - 1);
                        i++;
                    }

                }
                if (retVal == null) {
                    retVal = this;
                }
            }
            return retVal;
        },

        // Easily walk the tree and find the block that contains the coordinates
        findElements: function (minSearchLevel, maxSearchLevel)
        {
            minSearchLevel = minSearchLevel == null ? 0 : minSearchLevel;
            maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
            var retVal = [];
            for (var i = 0; i < this.children.length; i++) {
                var props = this.children[i].findElements(minSearchLevel, maxSearchLevel);
                for (var j = 0; j < props.length; j++) {
                    retVal.push(props[j]);
                }
            }


            return retVal;
        },


        // find resizehandle that contains coordinates
        findTriggeredResizeHandle: function (x, y)
        {
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
        // to be overridden by subclasse
        isOuterLeft: function ()
        {
            return true
        },
        isOuterRight: function ()
        {
            return true
        },
        isOuterTop: function ()
        {
            return true
        },
        isOuterBottom: function ()
        {
            return true
        },
        isOuter: function (side)
        {
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
        getElementAtSide: function (side)
        {
            if (side == Constants.SIDE.TOP || side == Constants.SIDE.LEFT) {
                return this.getPrevious();
            } else {
                return this.getNext();
            }
        },

        getNext: function ()
        {
            if (this.parent == null) return null;
            if (this.index + 1 < this.parent.children.length) {
                return this.parent.children[this.index + 1];
            } else {
                return null;
            }
        },
        getPrevious: function ()
        {
            if (this.parent == null) return null;
            if (this.index - 1 >= 0) {
                return this.parent.children[this.index - 1];
            } else {
                return null;
            }
        },

        // find most left and right column and use them to calculate the width of the parent
        getFullWidth: function ()
        {
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
        createAllDropspots: function ()
        {
            if (this instanceof blocks.elements.Block) {
                this.generateDropspots();
            } else {
                if (this.children.length > 0) {
                    for (var i = 0; i < this.children.length; i++) {
                        this.children[i].createAllDropspots();
                    }
                }

            }
            if (this.container != null) {
                this.container.createAllDropspots();
            }
        },

        // Creates rows or templates inside a column
        generateChildrenForColumn: function ()
        {
            var childType = Constants.ROW_CLASS;
            var rows = this.element.children("." + Constants.ROW_CLASS);
            if (rows.length == 0) {
                var rows = this.element.children();
                //var templates = true;
                for (var x = 0; i < rows.length; i++) {
                    if ($(rows[x].css('display') !== 'block')) {
                        //templates = false;
                        rows = [];
                        break;
                    }
                }
                childType = Constants.BLOCK_CLASS;
            }

            var innerZone = new blocks.elements.Surface(this.top, this.bottom, this.left, this.right);
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
                        this.children.push(new blocks.elements.Row(currentRow, this, i));
                    } else if (childType == Constants.BLOCK_CLASS) {
                        this.children.push(new blocks.elements.Block(currentRow, this, i));
                    }

                }


            }
        },

        generateChildrenForRow: function ()
        {
            // check only for columns
//                var columns = this.element.children("." + Constants.COLUMN_CLASS);
            var tColumns = this.element.children();
            var columns = [];

            for (var i = 0; i < tColumns.length; i++) {
                if (DOM.isColumn($(tColumns[i]))) {
                    columns.push($(tColumns[i]));
                }
            }
            var innerZone = new blocks.elements.Surface(this.top, this.bottom, this.left, this.right);

            if (columns.length > 0) {
                var rowWidth = 0;
                for (var x = 0; x < columns.length; x++) {
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


                        newColumn = new blocks.elements.Column(currentColumn, this, i);

                        var outside = this.parent != null && this.parent.parent != null && this.parent.parent instanceof blocks.elements.Container

                        if (oldColumn != null) {
                            this.resizeHandles.push(new blocks.elements.ResizeHandle(oldColumn, newColumn));
                        }
                        //else if (outside) {
                        //    this.resizeHandles.push(new templates.elements.ResizeHandle(oldColumn, newColumn));
                        //}


                        //if (outside && i == columnCount - 1) {
                        //    this.resizeHandles.push(new templates.elements.ResizeHandle(newColumn, null));
                        //}
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

                        newColumn = new blocks.elements.Column(currentColumn, this, i);

                    }


                    this.children.push(newColumn);
                    oldColumn = newColumn;
                }


            }
        },

        fillRows: function ()
        {
            var totalChildren = this.children.length;
            for (var i = 0; i < this.children.length; i++) {
                var child = this.children[i];
                var last = i + 1 == totalChildren;
                child.left = this.left;
                child.right = this.right;
                if (i == 0) {
                    child.top = this.top;
                }

                if (last) {
                    child.bottom = this.bottom;
                } else {
                    var next = this.children[i + 1];
                    var middle = Math.floor((child.bottom + next.top) / 2);
                    next.top = middle;
                    child.bottom = middle;

                }
                if (child instanceof  blocks.elements.Row) {
                    child.fillColumns();
                }
                if (child instanceof blocks.elements.Block) {
                    if (this.index > 0) child.overlay.addClass("left");
                    if (i > 0 || this.parent.index > 0) child.overlay.addClass("top");

                }
            }
        },

        fillColumns: function ()
        {
            var totalChildren = this.children.length;
            for (var i = 0; i < this.children.length; i++) {
                var child = this.children[i];
                var last = i + 1 == totalChildren;
                child.bottom = this.bottom;
                child.top = this.top;
                if (i == 0) {
                    child.left = this.left;
                }

                if (last) {
                    child.right = this.right;
                } else {
                    var next = this.children[i + 1];
                    var middle = Math.floor((child.right + next.left) / 2);
                    next.left = middle;
                    child.right = middle;

                }
                if (child instanceof  blocks.elements.Column) child.fillRows();
            }
        },

        getBlocks: function ()
        {
            if (this.totalBlocks != null) return this.totalBlocks;
            this.totalBlocks = 0;
            for (var i = 0; i < this.children.length; i++) {
                if (this.children[i] instanceof  blocks.elements.Block) {
                    this.totalBlocks += 1;
                } else {
                    this.totalBlocks += this.children[i].getBlocks();
                }
            }
            return this.totalBlocks;
        },

        getLayoutContainer: function ()
        {
            var retVal = null;
            if (this instanceof blocks.elements.Container && this.canLayout) {
                return this;
            }
            if (this instanceof blocks.elements.Property) {
                retVal = this.container.getLayoutContainer();
            } else {
                for (var i = 0; i < this.children.length; i++) {
                    retVal = this.children[i].getLayoutContainer();
                    if (retVal != null) return retVal;
                }
            }
            return retVal;
        },

        showOverlay: function ()
        {
            if (this.overlay != null) {
                this.overlay.css("width", (this.right - this.left) + "px");
                this.overlay.css("height", (this.bottom - this.top) + "px");

                if (this.overlay.parent().length > 0) this.overlay.remove();
                this.overlay.css("left", this.left + "px");
                this.overlay.css("top", this.top + "px");
                body.append(this.overlay);
            }
        },

        removeOverlay: function ()
        {
            if (this.overlay != null) this.overlay.remove();
        }
    });

}]);

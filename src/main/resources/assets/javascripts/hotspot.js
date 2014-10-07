/**
 * Created by wouter on 17/09/14.
 */
var Blocks = Blocks || {}

Blocks.Surface = my.Class({
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

Blocks.DropSpot = my.Class(Blocks.Surface, {
    STATIC: {
        SIDE: {
            TOP: 1,
            BOTTOM: 2,
            LEFT: 3,
            RIGHT: 4,
            NONE: 0
        },
        WIDTH: 5 //  defines the hotspot width on the side of the blocks
    },

    constructor: function(top, bottom, left, right, side, parent) {
        Blocks.DropSpot.Super.call(this, top, bottom, left, right);
        this.parent = parent;
        this.drawSurface = Blocks.Surface(top, bottom, left, right);
        this.side = side;
        this.calculateDrawSurface();
    },

    calculateDrawSurface: function() {
        var parent = this.parent;
      if (this.side == Blocks.DropSpot.SIDE.TOP || this.side == Blocks.DropSpot.SIDE.LEFT) {
          // search element that is not first or until parent == null
          while (parent.element != null && parent.element.before().length == 0) {
              parent = parent.parent;
          }
      } else {
          while (parent.element != null && parent.element.next().length == 0) {
              parent = parent.parent;
          }
      }

      if (this.side == Blocks.DropSpot.SIDE.LEFT) {
          this.drawSurface.left = parent.left;
          this.drawSurface.right = parent.left + Blocks.DropSpot.WIDTH;
      } else if (this.side == Blocks.DropSpot.SIDE.TOP) {
          this.drawSurface.top = parent.top;
          this.drawSurface.bottom = parent.top + Blocks.DropSpot.WIDTH;
      } else if (this.side == Blocks.DropSpot.SIDE.BOTTOM) {
          this.drawSurface.top = parent.bottom - Blocks.DropSpot.WIDTH;
          this.drawSurface.bottom = parent.bottom;
      } else if (this.side == Blocks.DropSpot.SIDE.RIGHT) {
          this.drawSurface.left = parent.right - Blocks.DropSpot.WIDTH;
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

Blocks.ResizeHandle = my.Class(Blocks.Surface, {
    STATIC: {
        WIDTH: 5 //  defines the hotspot width on the side of the blocks
    },

    constructor: function(top, bottom, left, right, leftColumn, rightColumn) {
        Blocks.ResizeHandle.Super.call(this, top, bottom, left, right);
        this.leftColumn = leftColumn;
        this.rightColumn = rightColumn
    },

    isTriggered: function(event) {
        var retVal = false;
        if (this.isEventInSurface(event)) {
            retVal = true;
        }
        return retVal;
    }

});

Blocks.LayoutElement = my.Class(Blocks.Surface, {
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
        Blocks.LayoutElement.Super.call(this, top, bottom, left, right);
        this.parent = parent;
        this.element = element;
        this.children = [];
        this.triggers = [];
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

    findTriggeredTrigger: function(event) {
        var retVal = null;
        if (this.isEventInSurface(event)) {
            // find triggers
            var triggerCount = this.triggers.length;
            for (var i=0; i < triggerCount; i++) {
                if (this.triggers[i].isTriggered(event)) {
                    return this.triggers[i];
                }
            }
        }
        return retVal;
    }
});

// A row inside a column or a container
// Can only contain columns
Blocks.Row = my.Class(Blocks.LayoutElement, {
    constructor: function(top, bottom, left, right, element, parent) {
        Blocks.Row.Super.call(this, top, bottom, left, right, element, parent);
        this.generateChildren();
        this.generateTriggers();
    },

    generateChildren: function() {
       // check only for columns
        var columns = this.element.children(".column");
        var innerZone = new Blocks.LayoutElement(this.top + Blocks.DropSpot.WIDTH, this.bottom - Blocks.DropSpot.WIDTH, this.left, this.right, null);

        if (columns.length > 0) {
            var columnCount = columns.length;
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

                var column_zone = new Blocks.Column(innerZone.top, innerZone.bottom, zoneLeft, zoneRight, currentColumn, this);
                this.children.push(column_zone);
            }
        }
    },

    generateTriggers: function() {
        var triggerTop = new Blocks.DropSpot(this.top, this.top + Blocks.DropSpot.WIDTH, this.left, this.right, Blocks.DropSpot.SIDE.TOP, this);
        var triggerBottom = new Blocks.DropSpot(this.bottom - Blocks.DropSpot.WIDTH, this.bottom, this.left, this.right, Blocks.DropSpot.SIDE.BOTTOM, this);
        this.triggers.push(triggerTop);
        this.triggers.push(triggerBottom);
    }
});

// special kind of row that defines the region where blocks can be dragged
Blocks.Container = my.Class(Blocks.Row, {
    constructor: function(element) {
        Blocks.Container.Super.call(this, this.calculateTop(element), this.calculateBottom(element), this.calculateLeft(element), this.calculateRight(element), element, null);
    }
});

// Special kind of row that can contain content
Blocks.Block = my.Class(Blocks.Row, {

});

// A column (inside a row) -> Can contain rows or blocks
Blocks.Column = my.Class(Blocks.LayoutElement, {
    constructor: function(top, bottom, left, right, element, parent) {
        Blocks.Column.Super.call(this, top, bottom, left, right, element, parent);
        this.generateChildren();
        this.generateTriggers();
    },

    generateChildren: function() {
        var rows = this.element.children(".row");

        var innerZone = new Blocks.LayoutElement(this.top, this.bottom, this.left + Blocks.DropSpot.WIDTH, this.right - Blocks.DropSpot.WIDTH, null);
        if (this.element.siblings(".column") > 0 ) {
            innerZone.top = this.top + Blocks.DropSpot.WIDTH;
            innerZone.bottom = this.bottom - Blocks.DropSpot.WIDTH;
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

                var row_zone = new Blocks.Row(zoneTop, zoneBottom, innerZone.left, innerZone.right, currentRow, this);
                this.children.push(row_zone);
            }
        }
    },

    generateTriggers: function() {
        var triggerLeft = new Blocks.DropSpot(this.top, this.bottom, this.left, this.left + Blocks.DropSpot.WIDTH, Blocks.DropSpot.SIDE.LEFT, this);
        var triggerRight = new Blocks.DropSpot(this.top, this.bottom, this.right - Blocks.DropSpot.WIDTH, this.right, Blocks.DropSpot.SIDE.RIGHT, this);
        this.triggers.push(triggerLeft);
        this.triggers.push(triggerRight);
        if (this.element.siblings(".column").length > 0) {
            var triggerTop = new Blocks.DropSpot(this.top, this.top + Blocks.DropSpot.WIDTH, this.left, this.right, Blocks.DropSpot.SIDE.TOP, this);
            var triggerBottom = new Blocks.DropSpot(this.bottom - Blocks.DropSpot.WIDTH, this.bottom, this.left, this.right, Blocks.DropSpot.SIDE.BOTTOM, this);
            this.triggers.push(triggerTop);
            this.triggers.push(triggerBottom);
        }
    }
});

Blocks.LayoutTreeFactory = my.Class({

    constructor: function() {
        this.generateHotspots();
    },

    generateHotspots: function() {
        // We create some sort of a heat map. We define boxes for all draggable blocks
        // we can add left and rigth from each column
        // and left and right from container if container has more than 1 row
        // select each row and add bottom
        // if row has +1 colunms, we can add also to bottom of columns
        // except if column has +1 rows

        Logger.debug("Calculate hotspots");
        var _this = this;
        this.layoutTrees = [];
        $(".can-layout").each(function() {
            var containerElement = $(this);
            // get size of container including border & padding but not margins
            var container = new Blocks.Container(containerElement);
            Logger.debug(container);
            _this.layoutTrees.push(container);
        });
    }
})
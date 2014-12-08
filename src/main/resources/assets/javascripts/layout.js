/**
 * Layouter handles the layout of a page.
 * Layouter creates a virtual tree of all rows, blocks and columns that can be layouted
 * Layouter is the bridge between the virtual tree and the dom
 *
 * getLayoutTree exposes the layoutTree to the outside world
 * LayoutTree is a tree made of elements (instances) from classes from the Elements plugin
 * array [tree of (Container -> Rows -> Columns -> Blocks)]
 *
 * Layouter functions take instances from Elements classes as parameters and not jQuery objects
 * except when the parameter has Element in it's name
 *
 * this plugin listens to DO_REFRESH_LAYOUT event and DOM_DID_CHANGE event
 * DO_REFRESH_LAYOUT is for when the we have to rebuild the layout tree, but the dom did not change (e.g. window resize)
 * DOM_DID CHANGE is for ... well, ...
 *
 *
 */

blocks.plugin("blocks.core.Layouter", ["blocks.core.Elements", "blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.DomManipulation", function (Elements, Broadcaster, Constants, DOM) {
    var Layouter = this;
    // Helper function to return the jQuery Element of the drop location
    // and prepare the dropLocation for the drop
    // 1. if drop location is block with siblings in column and side left/right
    //      -> wrap in rows
    // 2. if droplocation has can-layout class prevent drop outside layoutable area
    //      -> take first or last row
    var findDropLocationElement = function (dropLocation, side) {
        var dropLocationElement = dropLocation.element;
        var retVal = dropLocationElement;

        // If we drop on the outer edge of the container we wrap everything inside a new container
        // Or we drop on the 1 element inside the container
        if (dropLocation instanceof Elements.Container) {
            var childrenColumns = dropLocationElement.children();
            if (childrenColumns.length > 1) {
                retVal = DOM.createRow().append(childrenColumns.remove());
                dropLocationElement.append(DOM.wrapBlockInColumn(retVal, 12));
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

    var dropOnFunctions = {
        "drop-vertical-on-block": function (droppedElement, dropLocationElement, side) {
            // do nothing?
            if (DOM.isColumn(droppedElement) || DOM.isRow(droppedElement)) {
                dropLocationElement = DOM.wrapSiblingBlocksInRows(dropLocationElement).parent().parent(); // return row
            }
            return dropLocationElement;
        },

        "drop-vertical-on-row": function (droppedElement, dropLocationElement, side) {
            // Do nothing
            return dropLocationElement;
        },

        "drop-vertical-on-column": function (droppedElement, dropLocationElement, side) {
            // wrap column in row
            dropLocationElement = DOM.wrapColumnInColumn(dropLocationElement);
            return dropLocationElement.children().first(); // return row
        },

        "drop-horizontal-on-block": function (droppedElement, dropLocationElement, side) {
            dropLocationElement = DOM.wrapSiblingBlocksInRows(dropLocationElement);
            return dropLocationElement.parent(); // return column
        },

        "drop-horizontal-on-row": function (droppedElement, dropLocationElement, side) {
            // wrap block in row
            dropLocationElement = DOM.wrapRowInRow(dropLocationElement);
            return dropLocationElement.children().first(); // return column
        },

        "drop-horizontal-on-column": function (droppedElement, dropLocationElement, side) {
            // Do nothing
            return dropLocationElement;
        }
    }

    var droppedFunctions = {

        "drop-block-vertical" : function(droppedElement, dropLocationElement, side) {
            // wrap block in row
            if (DOM.isColumn(dropLocationElement) || DOM.isRow(dropLocationElement)) {
                droppedElement = DOM.wrapBlockInRow(droppedElement);
            }
            return droppedElement;
        },

        "drop-row-vertical" : function(droppedElement, dropLocationElement, side) {
            // do nothing
            return droppedElement;
        },

        "drop-column-vertical" : function(droppedElement, dropLocationElement, side) {
            // wrap column in row
            return DOM.wrapColumnInRow(droppedElement);
        },

        "drop-block-horizontal" : function(droppedElement, dropLocationElement, side) {
            return DOM.wrapBlockInColumn(droppedElement);
        },

        "drop-column-horizontal" : function(droppedElement, dropLocationElement, side) {
            // wrap column in row
            return droppedElement
        },

        "drop-row-horizontal" : function(droppedElement, dropLocationElement, side) {
            return DOM.wrapRowInColumn(droppedElement);
        }
    }

    var drop =  function(droppedElement, dropLocationElement, side) {
        // If we drop on the edge of the container, wrap the container so we drop inside
        // (because we drop always outside the droplocation)
        var subj = "block";
        if (DOM.isColumn(droppedElement)) {
            subj = "column";
        } else if (DOM.isRow(droppedElement)) {
            subj = "row";
        }
        var obj = "block";
        if (DOM.isColumn(dropLocationElement)) {
            obj = "column";
        } else if (DOM.isRow(dropLocationElement)) {
            obj = "row";
        }
        var s = (side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) ? "vertical" : "horizontal";
        var dropString = "drop-" + s + "-on-" + obj;
        var droppedString = "drop-"+ subj +"-"+ s;
        dropLocationElement = dropOnFunctions[dropString](droppedElement, dropLocationElement);
        droppedElement = droppedFunctions[droppedString](droppedElement, dropLocationElement);

        DOM.appendElement(droppedElement, dropLocationElement, side, function() {
            Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DOM_DID_CHANGE);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        });

    };

    // remove block and add it at side of droplocation
    this.changeBlockLocation = function (block, dropLocation, side) {
        // remove dropped block
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        // Get column width of the dropped element
        var columnWidthDroppedElement = 12;
        if (DOM.isColumn(block.element.parent())) {
            columnWidthDroppedElement = DOM.getColumnWidth(block.element.parent());
        } else if (DOM.isColumn(block.element.parent().parent())) {
            columnWidthDroppedElement = DOM.getColumnWidth(block.element.parent().parent());
        }
        DOM.removeBlock(block, 200, function() {
            var dropLocationElement = findDropLocationElement(dropLocation, side);
            drop(block.element, dropLocationElement, side);
        });



    };

    // Add new jquery Object at bottom of dropLocation
    this.addNewBlockAtLocation = function(blockElement, dropLocation) {
        dropLocationElement = dropLocation.element;
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        Broadcaster.send(Broadcaster.EVENTS.DOM_WILL_CHANGE);

        drop(blockElement, dropLocationElement, Constants.SIDE.BOTTOM);


        // TODO return false if invalid so we can cancel everything
    };

    // remove block
    this.removeBlock = function(block) {
        if (block instanceof Elements.Block) {
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            DOM.removeBlock(block, 300, function() {
                Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DOM_DID_CHANGE);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            })
        }
    };





}]);

blocks.config("blocks.core.Layouter", {
    FADE_TIME: 200
})

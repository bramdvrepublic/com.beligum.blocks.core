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

    // Helper function to return the jQuery Element of the drop location
    // and prepare the dropLocation for the drop
    // 1. if drop location is block with siblings in column and side left/right
    //      -> wrap in rows
    // 2. if droplocation has can-layout class prevent drop outside layoutable area
    //      -> take first or last row
    var findDropLocationElement = function (dropLocation, side) {
        var dropLocationElement = dropLocation.element;
        var retVal = dropLocationElement;
        if (dropLocation instanceof Elements.Block) {
            if (side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) {
                DOM.wrapSiblingBlocksInRows(dropLocationElement);
                retVal = dropLocationElement.parent();
            } else {
                // Do nothing
            }
        } else if (dropLocationElement.hasClass(Constants.CAN_LAYOUT_CLASS)) {
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

    // remove block and add it at side of droplocation
    this.changeBlockLocation = function (block, dropLocation, side) {
        // remove dropped block
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE)
        var columnWidth = DOM.getColumnWidth(block.parent.element);
        DOM.removeBlock(block, 200, function() {
            prepareDropLocation(block.element, dropLocation, side)
        });

        var prepareDropLocation =  function(droppedElement, dropLocation, side) {
            var dropLocationElement = findDropLocationElement(dropLocation, side);
            if (dropLocationElement.hasClass(Constants.COLUMN_CLASS)) {
                droppedElement = DOM.wrapBlockInColumn(droppedElement, columnWidth);
            } else if (dropLocationElement.hasClass(Constants.ROW_CLASS)) {
                droppedElement = DOM.wrapBlockInRow(droppedElement);
            } else if (dropLocationElement.hasClass(Constants.BLOCK_CLASS) && (side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT)) {
                //dropLocationElement = wrapChildBlocksInRows(dropLocationElement);
                droppedElement = DOM.wrapBlockInColumn(droppedElement, columnWidth);
            } else {
                // this should not be possible
            }
            DOM.appendElement(droppedElement, dropLocationElement, side, function() {
                Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DOM_DID_CHANGE);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE)
            });

        };

    };

    // Add new jquery Object at bottom of dropLocation
    this.addNewBlockAtLocation = function(blockElement, dropLocation) {
        dropLocationElement = dropLocation.element;
        if (blockElement.hasClass("block") && dropLocation instanceof Elements.Block) {
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            Broadcaster.send(Broadcaster.EVENTS.DOM_WILL_CHANGE);
            DOM.appendElement(blockElement, dropLocationElement, Constants.SIDE.BOTTOM, function() {
                Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DOM_DID_CHANGE);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            })
        }
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

    var layoutTree = null;

    this.getLayoutTree = function() {
        if (layoutTree == null) {
            buildLayoutTree();
        }
        return layoutTree;
    }

    var buildLayoutTree = function () {
        // We create some sort of a heat map. We define boxes for all draggable blocks
        // we can add left and rigth from each column
        // and left and right from container if container has more than 1 row
        // select each row and add bottom
        // if row has +1 colunms, we can add also to bottom of columns
        // except if column has +1 rows

        Logger.debug("Calculate hotspots");
        layoutTree = [];
        //_this.cleanLayout();
        $("." + Constants.CAN_LAYOUT_CLASS).each(function () {
            var containerElement = $(this);
            // get size of container including border & padding but not margins
            var container = new Elements.Container(containerElement);
            Logger.debug(container);
            layoutTree.push(container);
        });
        Broadcaster.send(Broadcaster.EVENTS.DID_REFRESH_LAYOUT);
    };

    Broadcaster.on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function() {
        buildLayoutTree();
    })

    Broadcaster.on(Broadcaster.EVENTS.DOM_DID_CHANGE, function() {
        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DO_REFRESH_LAYOUT);
    })




    // On Boot
    $(window).on("resize.blocks_core", function () {
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT);
    });



}]);

blocks.config("blocks.core.Layouter", {
    FADE_TIME: 200
})

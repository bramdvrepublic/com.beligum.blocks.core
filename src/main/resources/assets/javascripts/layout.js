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

    // remove block and add it at side of droplocation
    this.changeBlockLocation = function (block, dropLocation, side) {
        // remove dropped block
        Broadcaster.send(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
        var columnWidth = 12;
        if (DOM.isColumn(block.element.parent())) {
            columnWidth = DOM.getColumnWidth(block.element.parent());
        } else if (DOM.isColumn(block.element.parent().parent())) {
            columnWidth = DOM.getColumnWidth(block.element.parent().parent());
        }
        DOM.removeBlock(block, 200, function() {
            prepareDropLocation(block.element, dropLocation, side)
        });

        var prepareDropLocation =  function(droppedElement, dropLocation, side) {
            var dropLocationElement = findDropLocationElement(dropLocation, side);
            if (DOM.isColumn(dropLocationElement)) {
                if (DOM.isRow(droppedElement)) {
                    droppedElement = DOM.wrapRowInColumn(droppedElement, columnWidth);
                } else if (!DOM.isColumn(droppedElement)) {
                    droppedElement = DOM.wrapBlockInColumn(droppedElement, columnWidth);
                }
            } else if (DOM.isRow(dropLocationElement)) {
                if (DOM.isColumn(droppedElement)) {
                    droppedElement = DOM.wrapColumnInRow(droppedElement);
                } else if (!DOM.isRow(droppedElement)) {
                    droppedElement = DOM.wrapBlockInRow(droppedElement);
                }
            } else { // Block, no layout info
                if ((side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM)) {
                    if (DOM.isRow(droppedElement)) {
                        // We drop row on top of block
                        // wrap all sibling blocks in rows
                        DOM.wrapSiblingBlocksInRows(dropLocationElement);
                        dropLocationElement = dropLocationElement.parent().parent(); // droplocation is row
                    } else if (DOM.isColumn(droppedElement)) {
                        droppedElement = DOM.wrapColumnInRow(droppedElement);
                        DOM.wrapSiblingBlocksInRows(dropLocationElement);
                        dropLocationElement = dropLocationElement.parent().parent(); // droplocation is row
                    }
                } else if ((side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT)) {
                    //dropLocationElement = wrapChildBlocksInRows(dropLocationElement);
                    DOM.wrapSiblingBlocksInRows(dropLocationElement);
                    dropLocationElement = dropLocationElement.parent(); // droplocation is column
                    if (DOM.isRow(droppedElement)) {
                        droppedElement = DOM.wrapRowInColumn(droppedElement, columnWidth);
                    } else if (!DOM.isColumn(droppedElement)) {
                        droppedElement = DOM.wrapBlockInColumn(droppedElement, columnWidth);
                    }
                }
            }
            DOM.appendElement(droppedElement, dropLocationElement, side, function() {
                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DOM_DID_CHANGE());
                Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE())
            });

        };

    };

    // Add new jquery Object at bottom of dropLocation
    this.addNewBlockAtLocation = function(blockElement, dropLocation) {
        dropLocationElement = dropLocation.element;
        if (dropLocation instanceof Elements.Block) {
            Broadcaster.send(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
            Broadcaster.send(new Broadcaster.EVENTS.DOM_WILL_CHANGE());
            DOM.appendElement(blockElement, dropLocationElement, Constants.SIDE.BOTTOM, function() {
                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DOM_DID_CHANGE());
                Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());
            })
        }
        // TODO return false if invalid so we can cancel everything
    };

    // remove block
    this.removeBlock = function(block) {
        if (block instanceof Elements.Block) {
            Broadcaster.send(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
            DOM.removeBlock(block, 300, function() {
                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DOM_DID_CHANGE());
                Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());
            })
        }
    };

    // The parent element where the tree is build
    // if null this is automatically set to the container
    var layoutParentElement = null;
    var layoutTree = null;

    this.getLayoutTree = function() {
        if (layoutTree == null) {
            buildLayoutTree();
        }
        return layoutTree;
    };

    var buildLayoutTree = function () {
        // We create some sort of a heat map. We define boxes for all draggable blocks
        // we can add left and right from each column
        // and left and right from container if container has more than 1 row
        // select each row and add bottom
        // if row has +1 colunms, we can add also to bottom of columns
        // except if column has +1 rows

        Logger.debug("Calculate hotspots");
        layoutTree = [];
        //_this.cleanLayout();
        if (layoutParentElement == null) {
            layoutParentElement = $("." + Constants.CONTAINER_CLASS);
        }

        layoutParentElement.children("." + Constants.ROW_CLASS).each(function () {
            var parentRow = $(this);
            var container = new Elements.Container(parentRow);
            Logger.debug(container);
            layoutTree.push(container);
        });
        Broadcaster.send(new Broadcaster.EVENTS.DID_REFRESH_LAYOUT());
    };

    Broadcaster.on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, "blocks.core.Layouter", function() {
        buildLayoutTree();
    })

    Broadcaster.on(Broadcaster.EVENTS.DOM_DID_CHANGE, "blocks.core.Layouter", function() {
        Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DO_REFRESH_LAYOUT());
    })

    // On Boot
    $(window).on("resize.blocks_core", function () {
        Broadcaster.send(new Broadcaster.EVENTS.DO_REFRESH_LAYOUT());
    });



}]);

blocks.config("blocks.core.Layouter", {
    FADE_TIME: 200
})

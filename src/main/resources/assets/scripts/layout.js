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

base.plugin("blocks.core.Layouter", ["blocks.core.Broadcaster", "base.core.Constants", "blocks.core.DomManipulation", function (Broadcaster, Constants, DOM)
{
    var Layouter = this;


    // When dropped on a container we have to find the right element to drop on
    var findDropLocationElement = function (dropLocation, side)
    {
        var dropLocationElement = dropLocation.element;
        var retVal = dropLocationElement;

        // If we drop on the outer edge of the container we wrap everything inside a new container
        // Or we drop on the 1 element inside the container
        if (dropLocation instanceof blocks.elements.Container) {
            var childrenRows = dropLocationElement.children();
            if (side == Constants.SIDE.TOP) {
                retVal = $(childrenRows[0]);
            } else if (side == Constants.SIDE.BOTTOM) {
                retVal = $(childrenRows[childrenRows.length - 1]);
            } else {
                if (childrenRows.length > 1) {
                    retVal = DOM.createRow().append(DOM.createColumn().append(childrenRows.remove()));
                    dropLocationElement.append(retVal);
                } else if (childrenRows.length == 1) {
                    if (side == Constants.SIDE.LEFT) {
                        // first column inside row insdide container
                        retVal = $(childrenRows[0].children[0]);
                    } else {
                        // last column inside row insdide container
                        retVal = $(childrenRows[0].children[childrenRows[0].children.length - 1]);
                    }
                } else {
                    Logger.debug("This should never happen!")
                }
            }

        }
        return retVal;
    };

    /*
     * Wrap (if necessary)  so a correct bootstrap layout is preserved.
     * This allows us to drop on columns and rows and not only on templates
     * */
    var dropOnFunctions = {
        "drop-vertical-on-block": function (droppedElement, dropLocationElement, side)
        {
            // do nothing?
            if (DOM.isColumn(droppedElement) || DOM.isRow(droppedElement)) {
                dropLocationElement = DOM.wrapSiblingBlocksInRows(dropLocationElement).parent().parent(); // return row
            }
            return dropLocationElement;
        },

        "drop-vertical-on-row": function (droppedElement, dropLocationElement, side)
        {
            // Do nothing
            return dropLocationElement;
        },

        "drop-vertical-on-column": function (droppedElement, dropLocationElement, side)
        {
            // wrap column in row
            dropLocationElement = DOM.wrapColumnInColumn(dropLocationElement);
            return dropLocationElement.children().first(); // return row
        },

        "drop-horizontal-on-block": function (droppedElement, dropLocationElement, side)
        {
            dropLocationElement = DOM.wrapSiblingBlocksInRows(dropLocationElement);
            return dropLocationElement.parent(); // return column
        },

        "drop-horizontal-on-row": function (droppedElement, dropLocationElement, side)
        {
            // wrap block in row
            dropLocationElement = DOM.wrapRowInRow(dropLocationElement);
            return dropLocationElement.children().first(); // return column
        },

        "drop-horizontal-on-column": function (droppedElement, dropLocationElement, side)
        {
            // Do nothing
            return dropLocationElement;
        }
    }

    /*
     * Wrap (if necessary) the droppedElement so a correct bootstrap layout is preserved.
     * This allows us to drop on columns and rows and not only on templates
     * */
    var droppedFunctions = {

        "drop-block-vertical": function (droppedElement, dropLocationElement, side)
        {
            // wrap block in row
            if (DOM.isColumn(dropLocationElement) || DOM.isRow(dropLocationElement)) {
                droppedElement = DOM.wrapBlockInRow(droppedElement);
            }
            return droppedElement;
        },

        "drop-row-vertical": function (droppedElement, dropLocationElement, side)
        {
            // do nothing
            return droppedElement;
        },

        "drop-column-vertical": function (droppedElement, dropLocationElement, side)
        {
            // wrap column in row
            return DOM.wrapColumnInRow(droppedElement);
        },

        "drop-block-horizontal": function (droppedElement, dropLocationElement, side)
        {
            return DOM.wrapBlockInColumn(droppedElement);
        },

        "drop-column-horizontal": function (droppedElement, dropLocationElement, side)
        {
            // wrap column in row
            return droppedElement
        },

        "drop-row-horizontal": function (droppedElement, dropLocationElement, side)
        {
            return DOM.wrapRowInColumn(droppedElement);
        }
    }

    /*
     * Function to call to add a block relative to a block
     * This function changes the dom
     * */
    var drop = function (droppedElement, dropLocationElement, droppedContainer, dropContainer, side)
    {

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
        var droppedString = "drop-" + subj + "-" + s;
        var originalDroppedElement = droppedElement;
        dropLocationElement = dropOnFunctions[dropString](droppedElement, dropLocationElement);
        droppedElement = droppedFunctions[droppedString](droppedElement, dropLocationElement);

        var finish = function() {
            droppedElement.toggle(200, function() {
                Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            });
        };

        droppedElement.toggle(200, function() {

            droppedElement.hide();
            DOM.appendElement(droppedElement, dropLocationElement, side, function () {
            if (droppedContainer == null || droppedContainer == dropContainer) {
                DOM.cleanup(dropContainer, finish);
            } else {
                DOM.cleanup(droppedContainer, function() {});
                DOM.cleanup(dropContainer, finish);
            }

            });
        });

    };


    /*
     * This is the external interface to drop a block.
     * First it removes the dropped block and then adds it relative to the droplocation
     * */
    this.changeBlockLocation = function (block, dropLocation, side)
    {
        // remove dropped block
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        droppedElement = block.element;
        dropLocationElement = findDropLocationElement(dropLocation, side);
        drop(droppedElement, dropLocationElement, block.getContainer().element, dropLocation.getContainer().element, side);


    };

    // Add new jquery Object at bottom of dropLocation
    this.addNewBlockAtLocation = function (blockElement,  dropLocation, side)
    {
        dropLocationElement = dropLocationElement = findDropLocationElement(dropLocation, side);
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);

        drop(blockElement, dropLocationElement, null, dropLocation.getContainer().element, side);

        // TODO return false if invalid so we can cancel everything
    };

    // remove block
    this.removeBlock = function (block)
    {
        if (block instanceof blocks.elements.Block) {
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            DOM.removeBlock(block, 300, function ()
            {
                Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            })
        }
    };


}]);


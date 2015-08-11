base.plugin("blocks.core.Hover", ["constants.blocks.core", function (BlocksConstants)
{
    var Hover = this;

    //-----VARIABLES-----
    var hoveredBlock = null;
    var hoveredProperty = null;

    var pageBlock = null;
    var focusedBlock = null;
    var layoutTree = null;

    //-----METHODS-----
    this.setHoveredBlock = function (layoutElement)
    {
        hoveredBlock = layoutElement;
    };
    this.getHoveredBlock = function ()
    {
        return hoveredBlock;
    };
    this.setFocusedBlock = function (layoutElement)
    {
        focusedBlock = layoutElement;
    };
    this.getFocusedBlock = function ()
    {
        return focusedBlock;
    };

    this.getPageBlock = function ()
    {
        return pageBlock;
    };

    /*
     We create some sort of a heat map. We define boxes for all draggable templates
     we can add left and right from each column
     and left and right from container if container has more than 1 row
     select each row and add bottom
     if row has +1 colunms, we can add also to bottom of columns
     except if column has +1 rows
     */
    this.createPageBlock = function ()
    {
        pageBlock = new blocks.elements.Page();

        return pageBlock;
    };

    this.showHoverOverlays = function ()
    {
        if (Hover.getPageBlock() != null) {
            var elements = Hover.getPageBlock().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                //only valid bootstrapped-layouted blocks can be dragged (eg. the first element in a column in a row)
                if (elements[i].canDrag) {
                    elements[i].showOverlay();
                }
            }
        }
    };

    //TODO this gets called way too much and should be tied into some more general 'reset hover and focus' method
    this.removeHoverOverlays = function ()
    {
        if (Hover.getPageBlock() != null) {
            var elements = Hover.getPageBlock().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                elements[i].removeOverlay();
            }
        }
    };

    this.removeResizeHandles = function ()
    {
        if (Hover.getPageBlock() != null) {
            var elements = Hover.getPageBlock().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i] instanceof blocks.elements.Row) {
                    elements[i].removeOverlay();
                }
            }
        }
    };

    this.showResizeHandles = function ()
    {
        if (Hover.getPageBlock() != null) {
            var elements = Hover.getPageBlock().findElements(0, 9);
            for (var i = 0; i < elements.length; i++) {
                if (elements[i] instanceof blocks.elements.Row) {
                    elements[i].showOverlay();
                }
            }
        }
    };

    //note: these are not really overlays, but actually classes on siblings etc
    this.showFocusOverlays = function (selectedElement)
    {
        // Blur everything visually
        selectedElement.parents().siblings().addClass(BlocksConstants.OPACITY_CLASS);
        selectedElement.siblings().addClass(BlocksConstants.OPACITY_CLASS);

        // Draw an outline around the focused element, but not around the entire page
        if (!selectedElement.hasClass(BlocksConstants.PAGE_CONTENT_CLASS)) {
            selectedElement.addClass(BlocksConstants.PROPERTY_EDIT_CLASS);
        }
    };

    //note: these are not really overlays, but actually classes on siblings etc
    this.removeFocusOverlays = function()
    {
        $("." + BlocksConstants.OPACITY_CLASS).removeClass(BlocksConstants.OPACITY_CLASS);
        $("." + BlocksConstants.PREVENT_BLUR_CLASS).removeClass(BlocksConstants.PREVENT_BLUR_CLASS);
        $("." + BlocksConstants.PROPERTY_EDIT_CLASS).removeClass(BlocksConstants.PROPERTY_EDIT_CLASS);
        $("." + BlocksConstants.BLOCK_EDIT_CLASS).removeClass(BlocksConstants.BLOCK_EDIT_CLASS);
    };

    this.findFirstParentPropertyOrTemplate = function(block, element)
    {
        //we go hunting for the first property up the chain, starting from the specific element we did the mouseup on
        var propertyOrTagElement = element;
        while (!(propertyOrTagElement.hasAttribute("property") || propertyOrTagElement.hasAttribute("data-property")) && propertyOrTagElement.prop("tagName").indexOf("-") == -1 && propertyOrTagElement.prop("tagName") != "BODY") {
            propertyOrTagElement = propertyOrTagElement.parent();
        }
        //if we hit the body boundary, we actually didn't find anything
        if (propertyOrTagElement.prop("tagName") == "BODY") {
            propertyOrTagElement = null;
        }
        //if we hit a template boundary and it's not the same as the startBlock, we didn't find anything (weird situation though...)
        else if (propertyOrTagElement.prop("tagName").indexOf("-") != -1) {
            if (propertyOrTagElement!=block) {
                propertyOrTagElement = null;
            }
        }

        return propertyOrTagElement;
    };
}]);
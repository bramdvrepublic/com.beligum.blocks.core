/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

base.plugin("blocks.core.Hover", ["constants.blocks.core", "blocks.core.DOM", function (BlocksConstants, DOM)
{
    var Hover = this;

    //-----VARIABLES-----
    var hoveredBlock = null;
    var hoveredProperty = null;

    var pageBlock = null;
    var focusedBlock = null;

    //-----METHODS-----
    this.setHoveredBlock = function (surface)
    {
        hoveredBlock = surface;
    };
    this.getHoveredBlock = function ()
    {
        return hoveredBlock;
    };
    this.setFocusedBlock = function (surface)
    {
        focusedBlock = surface;
    };
    this.getFocusedBlock = function ()
    {
        return focusedBlock;
    };

    this.getPageBlock = function ()
    {
        return pageBlock;
    };

    // This will bootstrap some sort of a heat map.
    // We define 'overlay boxes' for all draggable templates on the page.
    // Rules:
    // we can add left and right from each column
    // and left and right from container if container has more than 1 row
    // select each row and add bottom
    // if row has +1 columns, we can add also to bottom of columns
    // except if column has +1 rows
    this.createPageBlock = function ()
    {
        pageBlock = new blocks.elements.Page();

        return pageBlock;
    };

    // this.showHoverOverlays = function ()
    // {
    //     if (Hover.getPageBlock() != null) {
    //         var elements = Hover.getPageBlock().findElements(0, 9);
    //         for (var i = 0; i < elements.length; i++) {
    //             //only valid bootstrapped-layouted blocks can be dragged (eg. the first element in a column in a row)
    //             if (elements[i].canDrag) {
    //                 elements[i].showOverlay();
    //             }
    //         }
    //     }
    // };
    //
    // //TODO this gets called way too much and should be tied into some more general 'reset hover and focus' method
    // this.removeHoverOverlays = function ()
    // {
    //     if (Hover.getPageBlock() != null) {
    //         var elements = Hover.getPageBlock().findElements(0, 9);
    //         for (var i = 0; i < elements.length; i++) {
    //             elements[i].removeOverlay();
    //         }
    //     }
    // };

    // this.removeResizeHandles = function ()
    // {
    //     if (Hover.getPageBlock() != null) {
    //         var elements = Hover.getPageBlock().findElements(0, 9);
    //         for (var i = 0; i < elements.length; i++) {
    //             if (elements[i] instanceof blocks.elements.Row) {
    //                 elements[i].removeOverlay();
    //             }
    //         }
    //     }
    // };
    //
    // this.showResizeHandles = function ()
    // {
    //     if (Hover.getPageBlock() != null) {
    //         var elements = Hover.getPageBlock().findElements(0, 9);
    //         for (var i = 0; i < elements.length; i++) {
    //             if (elements[i] instanceof blocks.elements.Row) {
    //                 elements[i].showOverlay();
    //             }
    //         }
    //     }
    // };

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
    this.removeFocusOverlays = function ()
    {
        $("." + BlocksConstants.OPACITY_CLASS).removeClass(BlocksConstants.OPACITY_CLASS);
        $("." + BlocksConstants.PREVENT_BLUR_CLASS).removeClass(BlocksConstants.PREVENT_BLUR_CLASS);
        $("." + BlocksConstants.PROPERTY_EDIT_CLASS).removeClass(BlocksConstants.PROPERTY_EDIT_CLASS);
    };

    /**
     * @param block The block we started on, can be null if we're creating a new block
     * @param element The target element from the event
     * @param event
     * @returns {*}
     */
    this.createHoverClickObject = function (block, element, event)
    {
        var retVal = null;

        //we go hunting for the first property up the chain, starting from the specific element we did the mouseup on
        var propertyOrTagElement = Hover.findFirstParentPropertyOrTemplate(block, element);

        if (propertyOrTagElement) {
            //note: use the original event because the event system wraps the events and the coordinates get lost
            var hotspot = {
                left: event.originalEvent.clientX,
                top: event.originalEvent.clientY
            };

            //this means we tinkered with the element, so adapts the hotspot
            if (propertyOrTagElement != element) {
                var elemWidth = $(propertyOrTagElement).width();
                var elemHeight = $(propertyOrTagElement).height();
                var elemPos = $(propertyOrTagElement).offset();
                var elemBottom = elemPos.top + elemHeight;
                var elemRight = elemPos.left + elemWidth;

                var mouseIsOnProperty = (hotspot.left >= elemPos.left && hotspot.left <= elemRight) && (hotspot.top >= elemPos.top && hotspot.top <= elemBottom);
                if (!mouseIsOnProperty) {
                    hotspot.left = elemPos.left + 1;
                    hotspot.top = elemPos.top + 1;
                }
            }

            retVal = {
                //this is the surface block all events started on (holds a reference to both the overlay and the template block)
                block: block,
                //this is the specific 'deep' html element at this mouse position that was clicked (possible because we disabled the events of the overlays during mousedown)
                element: element,
                //this is the html element 'on the way up'
                propertyElement: propertyOrTagElement,
                //the (possibly changed) hotspot to be used in eg. the focus system (like for editor)
                hotspot: hotspot
            };
        }
        else {
            Logger.error("Hover.findFirstParentPropertyOrTemplate() resulted in a null property object");
        }

        return retVal;
    };

    this.findFirstParentPropertyOrTemplate = function (block, element)
    {
        //this will help solving the "I clicked below a block, but it seemed like that block extended all the way down, because it has a large block next to it"
        //note that if element is null, this is overridden by that
        var clickedElement = element;
        if (clickedElement != null) {
            //if we didn't click on a tag inside the block we hovered on, just select the block element (to start with, see below)
            if (!block.element.is(clickedElement)) {
                if (block.element.find(clickedElement).length == 0) {
                    clickedElement = block.element;
                }
            }
        }

        // if we clicked on the block (or made it look like that) and that block has a property (or more) as a direct child
        // it makes sense to fall through and select the element holding that property
        if (clickedElement.is(block.element)) {
            var directProperties = clickedElement.find('> [property], > [data-property]');
            if (directProperties.length > 0) {
                clickedElement = directProperties;
            }
        }

        //we go hunting for the first property up the chain, starting from the specific element we did the mouseup on
        var propertyOrTagElement = clickedElement;

        // find parents until parent is <body> or until parent has property attribute
        // first property enable editing
        while (!(propertyOrTagElement.hasAttribute("property") || propertyOrTagElement.hasAttribute("data-property")) && propertyOrTagElement.prop("tagName").indexOf("-") == -1 && propertyOrTagElement.prop("tagName") != "BODY") {
            propertyOrTagElement = propertyOrTagElement.parent();
        }
        //if we hit the body boundary, we actually didn't find anything
        if (propertyOrTagElement.prop("tagName") == "BODY") {
            propertyOrTagElement = null;
        }
        //if we hit a template boundary and it's not the same as the startBlock, we didn't find anything (weird situation though...)
        else if (propertyOrTagElement.prop("tagName").indexOf("-") != -1) {
            if (!propertyOrTagElement.is(block.element)) {
                // note: this happens when you click on a tag template inside a block (without a property set) that doesn't have a widget registered to it
                // so it's safe to select the outer block, when it's a child of the block, I assume?
                if (block.element.find(propertyOrTagElement).length) {
                    propertyOrTagElement = block.element;
                }
                //notify there's something wrong
                else {
                    propertyOrTagElement = null;
                }
            }
        }

        return propertyOrTagElement;
    };
}]);
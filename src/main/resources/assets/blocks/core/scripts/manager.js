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

/**
 * The manager is the central point of the editor and solves the circular dependency issue.
 * It's basically a main dispatcher where all events arrive (and acted upon), so we can reach it from
 * all other submodules by letting them send out events without having to inject this manager
 * into them (and create a circular dependency).
 *
 * Rule of thumb: this manager will probably inject all submodules (to fire up their methods as a reaction
 * on events), but this manager will never be injected as a dependency of a submodule, only be letting
 * them send out events that are listened to in the manager.
 *
 * Created by wouter on 19/01/15.
 */
base.plugin("blocks.core.Manager", ["constants.base.core.internal", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DragDrop", "blocks.core.Resizer", "blocks.core.Hover", "blocks.core.DOM", "blocks.core.Sidebar", "blocks.core.UI", function (Constants, BlocksConstants, BlocksMessages, Broadcaster, Mouse, DragDrop, Resizer, Hover, DOM, Sidebar, UI)
{
    var Manager = this;

    //-----CONSTANTS-----

    //-----VARIABLES-----
    // flag to enable/disable layout functionality (create, resize, move and delete)
    var allowLayout = true;

    // flag to enable/disable editing of existing blocks (giving them focus to start editing)
    var allowEdit = true;

    //the surface of the page
    var pageSurface = null;

    //the currently focused surface
    var focusedSurface = null;

    //-----EVENT LISTENERS-----
    /**
     * Sent out by menu.js when the sidebar was opened and the editor needs to boot up
     */
    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function (event)
    {
        //note that this encapsulates DO_REFRESH_LAYOUT, but initializes a few other things first
        // Broadcaster.send(Broadcaster.EVENTS.DOM_CHANGED, event);

        //start off by showing the layouter
        // Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, event);

        //create the page model
        pageSurface = new blocks.elements.Page();

        //use the generalized method to put focus on the newly created page
        switchFocus(pageSurface, pageSurface.element, event);

        //start listening for clicks
        Mouse.activate();
    });

    /**
     * Sent out by menu.js when the sidebar was closed and the editor needs to shut down
     */
    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function (event)
    {
        //some cleanup: helps bugs when closing the bar during focus
        focusSwitch(Hover.getPageBlock());

        //TODO revise this (needs to be pause?)
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
    });

    /**
     * Sent out by numerous modules when the editing of the page needs to be
     * temporarily halted, eg. during saving, dialogs, resizing, etc.
     * so the page is frozen while we wait for something to complete.
     */
    $(document).on(Broadcaster.EVENTS.PAUSE_BLOCKS, function (event)
    {
        Mouse.deactivate();

        //TODO revise
        //Hover.removeHoverOverlays();
        // DragDrop.setActive(false);
        // DOM.enableTextSelection(true);
    });

    /**
     * Un-pause the editor
     */
    $(document).on(Broadcaster.EVENTS.RESUME_BLOCKS, function (event)
    {
        Mouse.activate();
    });

    /**
     * Sent out by mouse.js when a click is registered.
     *
     * @param eventData has this structure:
     *   eventData.surface: the surface of the block
     *   eventData.element: the low-level DOM element (in the block element) we clicked
     *   eventData.originalEvent: the mousedown event that originated the click
     */
    $(document).on(Broadcaster.EVENTS.MOUSE.CLICK, function (event, eventData)
    {
        // Logger.info(eventData.surface);
        // Logger.info(eventData.element[0].outerHTML.split('>')[0]);

        var switchToPage = true;

        //we clicked on a surface
        if (eventData.surface) {

            //option 1) check if we need to switch focus to a block
            if (
                //clicking on a surface is the first event to edit it, so block it if we don't have permission
                allowEdit
                //we don't allow to switch focus from one block to the next, let's always go back to the page first
                && (!focusedSurface || focusedSurface === pageSurface)
                //make sure the block we received is a valid block
                && eventData.surface.isBlock()
                //if we click on (instead of dragging) the new block button, do nothing and let the popover do it's thing
                && !eventData.surface.isNewBlock()) {

                switchFocus(eventData.surface, eventData.element, eventData.originalEvent);

                switchToPage = false;
            }
            //option 2) we re-clicked on the focused surface (this should be impossible because pointer-events are disabled on focus)
            else if (focusedSurface && eventData.surface === focusedSurface) {
                switchToPage = false;
            }
        }
        //we didn't click on a surface
        else if (!eventData.surface) {
            // there's currently focus on a block
            if (focusedSurface.isBlock()) {
                //option 3) since we disable pointer-events for the focused surface, additional clicks inside that surface will be tied to regular DOM elements inside the block;
                //          make sure we don't switch back to the page so the user can interact with the content of the block
                //          Note: $.closest() begins with the current element, so the block itself is also included.
                if (eventData.element.closest(focusedSurface.element).length > 0) {
                    switchToPage = false;
                }
                //option 4) if the click was not on a real surface-overlay, but also not in the page (but eg. in the sidebar, a dialog, etc), ignore it
                else if (eventData.element.closest(UI.pageContent).length == 0) {
                    switchToPage = false;
                }
            }
        }

        // in all other cases, we switch back to the page
        if (switchToPage && !focusedSurface.isPage()) {
            switchFocus(pageSurface, pageSurface.element, eventData.originalEvent);
        }
    });

    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG_START, function (event, eventData)
    {
        UI.overlayWrapper.addClass(BlocksConstants.OVERLAY_DRAGGING_CLASS);
    });
    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG_MOVE, function (event, eventData)
    {
        // if (eventData.prevHoveredSurface) {
        //     eventData.prevHoveredSurface.resetDropspots();
        // }

        if (eventData.hoveredSurface) {
            eventData.hoveredSurface.showDropspot(eventData.dragVector);
        }
    });
    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG_STOP, function (event, eventData)
    {
        UI.overlayWrapper.removeClass(BlocksConstants.OVERLAY_DRAGGING_CLASS);

        if (eventData.hoveredSurface) {
            //eventData.hoveredSurface.resetDropspots();
        }
    });

    //-----PRIVATE METHODS-----
    var switchFocus = function (surface, clickedElement, clickEvent)
    {
        //since the css below is animated, the sidebar will be filled by the time
        //the animation is finished, so boot it first
        Sidebar.init(surface, clickedElement, clickEvent);

        if (surface.isBlock()) {
            UI.overlayWrapper.addClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            surface.overlay.addClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            Mouse.enableDragging(false);
        }
        else {
            UI.overlayWrapper.removeClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            UI.surfaceWrapper.children().removeClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            Mouse.enableDragging(true);
        }

        focusedSurface = surface;
    };
    /**
     * @param block the block that should get focus (not null)
     * @param element the low-level element that we clicked on (may be null, if we didn't click on anything)
     * @param propertyElement the first property element or template element on the way up from element (may be null)
     * @param hotspot the (possibly changed) mouse coordinates that function as the 'hotspot' for this event (object with top and left like offset())
     */
    var focusSwitch = function (block, element, propertyElement, hotspot, event)
    {
        //this will make sure we always 'go back' to the page first, instead of directly focussing the next clicked block
        // except when we click on an element that's inside the currently focused block
        var previousFocusedBlock = Hover.getFocusedBlock();

        if (previousFocusedBlock == null || (previousFocusedBlock != Hover.getPageBlock() && previousFocusedBlock.element.find(propertyElement).length == 0)) {
            Sidebar.init(Hover.getPageBlock(), Hover.getPageBlock().element, Hover.getPageBlock().element.offset(), event);
            Hover.removeFocusOverlays();
            Hover.setFocusedBlock(Hover.getPageBlock());
            Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS, event);
        }
        else {
            //if we got a property, use it, otherwise focus the entire block
            var selectedElement = propertyElement == null ? block.element : propertyElement;

            Sidebar.init(block, selectedElement, hotspot, event);
            Hover.showFocusOverlays(block.element);
            Hover.setFocusedBlock(block);
            //TODO revise
            Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

            enableFocusBlurDetection(block, selectedElement);
        }
    };

    //-----TODO UNCHECKED-----
    //TODO revise (RESUME_BLOCKS)
    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event)
    {
        //start listening for mousedown, mouseup, mouseleave
        Mouse.activate();
        //collapse selection, prevent text selection, disable ondragstart
        DOM.enableTextSelection(false);
        //show surfaces
        //Hover.showHoverOverlays();
        //enable dragging
        DragDrop.setActive(true);

        //TODO move this to DO_REFRESH_LAYOUT?
        //fire up drag-and-drop subsystem if we have enough room
        var windowWidth = $(window).width();
        //TODO make constant
        var MIN_SCREEN_DND_THRESHOLD = 1030;
        if (windowWidth >= MIN_SCREEN_DND_THRESHOLD) {
            enableLayout(true);
        }
        else {
            Logger.debug("Available page screen size is less than " + MIN_SCREEN_DND_THRESHOLD + " (" + windowWidth + "), disabling drag-and-drop.");
            enableLayout(false);
        }
    });

    $(document).on(Broadcaster.EVENTS.DOM_CHANGED, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function (event)
    {
        //this will end up in menu.js triggering updateContainerWidth()
        Broadcaster.send(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, event);

        //hide the overlays while redrawing
        //Hover.removeHoverOverlays();

        //make sure the page content wrapper block is at least the height of the body (to support good, natural blur, see comments below)
        updatePageContentHeight();

        //we always start off with a focused page
        var pageBlock = Hover.createPageBlock();
        //note: we can't fill in the last argument because it's not a property or a template tag
        focusSwitch(pageBlock, pageBlock.element, null);

        //redrawing done
        //Hover.showHoverOverlays();

        Broadcaster.send(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function (event)
    {
        Mouse.resetMouse();
    });

    //-----EVENTS FOR DRAGGING-----
    /**
     * Called when a user starts dragging a block (fired after a minimum threshold is exceeded)
     */
    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event, eventData)
    {
        //Broadcaster.zoom();
        DOM.enableContextMenu(false);
        DragDrop.dragStarted(event, eventData);
    });

    /**
     * Called when a user aborted dragging a block
     */
    $(document).on(Broadcaster.EVENTS.ABORT_DRAG, function (event)
    {
        //Broadcaster.unzoom();
        DOM.enableContextMenu(true);
        DragDrop.dragAborted(event);
    });

    /**
     * Called when a user ended dragging a block
     */
    $(document).on(Broadcaster.EVENTS.END_DRAG, function (event)
    {
        //Broadcaster.unzoom();

        DOM.enableContextMenu(true);
        DragDrop.dragEnded(event);
    });

    /**
     * Called all the time when a user is dragging one block over another block
     */
    $(document).on(Broadcaster.EVENTS.DRAG_OVER_BLOCK, function (event, eventData)
    {
        if (DragDrop.isDragging()) {
            DragDrop.dragOverBlock(event, eventData);
        }
    });

    //-----EVENTS FOR DRAGGING-----
    var updatePageContentHeight = function ()
    {
        var body = $('html');
        var bodyBottom = body.position().top + body.outerHeight(true);

        var page = $('.' + BlocksConstants.PAGE_CONTENT_CLASS);
        var pageBottom = page.position().top + page.outerHeight(true);
        //Note: we must always set the out height to the body height (that's why it's commented out)
        // because we want the page content to scroll independently from the sidebar (css is set to overflow-y auto)
        //if (pageBottom<bodyBottom) {
        page.outerHeight(bodyBottom - page.position().top);
        //}
    };

    var enableFocusBlurDetection = function (block, focusedElement)
    {
        // this basically comes down to this:
        // we'll make the entire block a hotspot, not just the focusedElement
        // and all clicks inside that block will get a new focus (if it's not again the same element we clicked on)
        // as soon as we click outside of the block, it is blurred and focus goes to the page (always first the page)
        block.element.on("mousedown.manager_focus_end", function (event)
        {
            var element = $(event.target);
            if (element.length) {
                var hoverObj = Hover.createHoverClickObject(block, element, event);
                if (hoverObj && !hoverObj.propertyElement.is(focusedElement)) {
                    //unregister first, because the next run will add it again
                    block.element.off("mousedown.manager_focus_end");
                    //this will mainly end up in sidebar.js
                    Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, hoverObj);
                }
            }

            //don't prevent default or the text editor won't function
            //e.preventDefault();
            event.stopPropagation();
        });

        //Note: everything focus/blur related is more or less depending on clicking on the  PAGE_CONTENT_CLASS block,
        // so make sure it at least occupies the entire page (left of the sidebar) to create the illusion we can click
        // outside any block (even when we're not clicking on another block) to have a block lose focus.
        // Because setting the height of the PAGE_CONTENT_CLASS element using css was too error prone, we decided to set it using scripting,
        // see above in updatePageContentHeight()
        //Note 2: we can't eg; make this $(document), because it would blur too fast (eg. when clicking on the sidebar or alert dialogs)
        var page = $('.' + BlocksConstants.PAGE_CONTENT_CLASS);
        page.on("mousedown.manager_focus_end", function (event)
        {
            event.preventDefault();
            event.stopPropagation();

            block.element.off("mousedown.manager_focus_end");
            page.off("mousedown.manager_focus_end");

            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
        });
    };
    /**
     * Called when we want to enable/disable the layouting of the page
     * (eg. the whole drag-and-drop system to create, move and/or resize surfaces)
     */
    var enableLayout = function (enable)
    {
        if (enable) {
            //Mouse.enableLayout(true);
            DragDrop.setActive(true);
            Resizer.activate(true);

            if (UI.newBlockBtn) {
                UI.newBlockBtn.removeAttr("disabled");
            }
        }
        else {
            //Mouse.enableLayout(false);
            DragDrop.setActive(false);
            Resizer.activate(false);

            if (UI.newBlockBtn) {
                UI.newBlockBtn.attr("disabled", "");
                UI.newBlockBtn.attr("title", BlocksMessages.pageTooSmallToLayout);
            }
        }
    };

}]);

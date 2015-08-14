/**
 * Created by wouter on 19/01/15.
 *
 * The manager is the central point. here we catch all the events to keep an overview
 */
base.plugin("blocks.core.Manager", ["constants.blocks.core", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DragDrop", "blocks.core.Resizer", "blocks.core.Hover", "blocks.core.DomManipulation", "blocks.core.Sidebar", "blocks.core.UI", function (Constants, Broadcaster, Mouse, DragDrop, Resizer, Hover, DOM, Sidebar, UI)
{
    var Manager = this;

    //-----EVENTS-----
    //main entry point for blocks after all the GUI events are handled
    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function (event)
    {
        //load in all the elements
        UI.init();

        //note that this encapsulates DO_REFRESH_LAYOUT, but initializes a few other things first
        Broadcaster.send(Broadcaster.EVENTS.DOM_CHANGED, event);

        //start off by showing the layouter
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, event);
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function (event)
    {
        //some cleanup: helps bugs when closing the bar during focus
        focusSwitch(Hover.getPageBlock());

        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
    });

    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event)
    {
        Mouse.activate();
        DOM.disableTextSelection();
        Hover.showHoverOverlays();
        DragDrop.setActive(true);

        var windowWidth = $(window).width();
        var MIN_SCREEN_DND_THRESHOLD = 1030;
        if (windowWidth >= MIN_SCREEN_DND_THRESHOLD) {
            Broadcaster.send(Broadcaster.EVENTS.ENABLE_DND, event);
            Mouse.allowDrag();
        }
        else {
            Logger.debug("Available page screen size is less than " + MIN_SCREEN_DND_THRESHOLD + " (" + windowWidth + "), disabling drag-and-drop.");
            Broadcaster.send(Broadcaster.EVENTS.DISABLE_DND, event);
            Mouse.disallowDrag();
        }
    });

    $(document).on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, function (event)
    {
        Mouse.deactivate();
        Hover.removeHoverOverlays();
        DragDrop.setActive(false);
        DOM.enableTextSelection();
    });

    $(document).on(Broadcaster.EVENTS.DOM_CHANGED, function (event)
    {
        //update the max z-index of positioned elements
        DOM.calculateMaxIndex();

        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function (event)
    {
        //this will end up in menu.js triggering updateContainerWidth()
        Broadcaster.send(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, event);

        //hide the overlays while redrawing
        Hover.removeHoverOverlays();

        //we always start off with a focused page
        var pageBlock = Hover.createPageBlock();
        //note: we can't fill in the last argument because it's not a property or a template tag
        focusSwitch(pageBlock, pageBlock.element, null);

        //redrawing done
        Hover.showHoverOverlays();

        Broadcaster.send(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function (event)
    {
        Mouse.resetMouse();
    });

    //-----EVENTS FOR DRAGGING-----
    /**
     * Called when we want to enable the whole drag-and-drop system
     */
    $(document).on(Broadcaster.EVENTS.ENABLE_DND, function (event)
    {
        Mouse.allowDrag();
        DragDrop.setActive(true);
        Resizer.activate(true);

        if (UI.newBlockBtn) {
            UI.newBlockBtn.removeAttr("disabled");
        }
    });

    /**
     * Called when we want to disable the whole drag-and-drop system
     */
    $(document).on(Broadcaster.EVENTS.DISABLE_DND, function (event)
    {
        Mouse.disallowDrag();
        DragDrop.setActive(false);
        Resizer.activate(false);

        if (UI.newBlockBtn) {
            UI.newBlockBtn.attr("disabled", "");
            UI.newBlockBtn.attr("title", "Your window is not wide enough to drag and drop new blocks.");
        }
    });

    /**
     * Called when a user starts dragging a block
     */
    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event, eventData)
    {
        //Broadcaster.zoom();
        DOM.disableContextMenu();
        DragDrop.dragStarted(event, eventData);
    });

    /**
     * Called when a user aborted dragging a block
     */
    $(document).on(Broadcaster.EVENTS.ABORT_DRAG, function (event)
    {
        //Broadcaster.unzoom();
        DOM.enableContextMenu();
        DragDrop.dragAborted(event);
    });

    /**
     * Called when a user ended dragging a block
     */
    $(document).on(Broadcaster.EVENTS.END_DRAG, function (event)
    {
        //Broadcaster.unzoom();

        DOM.enableContextMenu();
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
    $(document).on(Broadcaster.EVENTS.FOCUS_BLOCK, function (event, eventData)
    {
        focusSwitch(eventData.block, eventData.element, eventData.propertyElement, eventData.hotspot, event);
    });

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

        if (previousFocusedBlock==null || (previousFocusedBlock != Hover.getPageBlock() && previousFocusedBlock.element.find(propertyElement).length == 0)) {
            Sidebar.focusBlock(Hover.getPageBlock(), Hover.getPageBlock().element, Hover.getPageBlock().element.offset(), event);
            Hover.removeFocusOverlays();
            Hover.setFocusedBlock(Hover.getPageBlock());
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, event);
        }
        else {
            //if we got a property, use it, otherwise focus the entire block
            var selectedElement = propertyElement == null ? block.element : propertyElement;

            Sidebar.focusBlock(block, selectedElement, hotspot, event);
            Hover.showFocusOverlays(block.element);
            Hover.setFocusedBlock(block);
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);

            enableFocusBlurDetection(block, selectedElement);
        }
    };

    var enableFocusBlurDetection = function(block, focusedElement)
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
        var page = $('.'+Constants.PAGE_CONTENT_CLASS);
        page.on("mousedown.manager_focus_end", function (event)
        {
            event.preventDefault();
            event.stopPropagation();

            block.element.off("mousedown.manager_focus_end");
            page.off("mousedown.manager_focus_end");

            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
        });
    };
}]);

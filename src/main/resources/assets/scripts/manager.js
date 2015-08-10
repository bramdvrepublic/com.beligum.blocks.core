/**
 * Created by wouter on 19/01/15.
 *
 * The manager is the central point. here we catch all the events to keep an overview
 */
base.plugin("blocks.core.Manager", ["constants.blocks.core", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DragDrop", "blocks.core.Resizer", "blocks.core.Overlay", "blocks.core.Edit", "blocks.core.DomManipulation", "blocks.core.Sidebar", function (Constants, Broadcaster, Mouse, DragDrop, Resizer, Overlay, Edit, DOM, Sidebar)
{
    /*
     * Because there is no good place to put this we hang this here to the base inside a utils package
     * This is referenced from different lot of locations
     * */
    base.utils = base.functions || {};
    base.utils.maxZIndex = 0;
    /**
     * Finds the maximum z-index in the DOM tree of elements that are relative or absolutely positioned.
     * Returns 1 when no such elements were found.
     */
    base.utils.calculateMaxIndex = function ()
    {
        this.maxIndex = Math.max.apply(null, $.map($('body  *'), function (e, n)
            {
                if ($(e).css('position') == 'absolute' || $(e).css('position') == 'relative') {
                    return parseInt($(e).css('z-index')) || 1;
                }
            })
        );
    };

    //-----EVENTS-----
    //main entry point for blocks after all the GUI events are handled
    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function (event)
    {
        //we're currently not dragging inside anything
        Overlay.setContainer(null);

        //start off with a clean and empty sidebar
        Sidebar.clear();

        //start registering the movements of the mouse (TODO: already needed here?)
        //Broadcaster.registerMouseMove();

        //note that this encapsulates DO_REFRESH_LAYOUT, but initializes a few other things first
        Broadcaster.send(Broadcaster.EVENTS.DOM_CHANGED, event);

        //start off by showing the layouter
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, event);
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function (event)
    {
        Sidebar.clear();
        //Broadcaster.unregisterMouseMove();
        Overlay.removeOverlays();
        Overlay.setContainer(null);

        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
    });

    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event)
    {
        Mouse.activate();
        DOM.disableSelection();
        Overlay.showOverlays();
        DragDrop.setActive(true);
        Sidebar.enableEditing();

        var windowWidth = $(window).width();
        var MIN_SCREEN_DND_THRESHOLD = 1030;
        if (windowWidth >= MIN_SCREEN_DND_THRESHOLD) {
            Broadcaster.send(Broadcaster.EVENTS.ENABLE_DND, event);
            Mouse.allowDrag();
        }
        else {
            Logger.debug("Available page screen size is less than " + MIN_SCREEN_DND_THRESHOLD + " ("+windowWidth+"), disabling drag-and-drop.");
            Broadcaster.send(Broadcaster.EVENTS.DISABLE_DND, event);
            Mouse.disallowDrag();
        }
    });

    $(document).on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, function (event)
    {
        Mouse.deactivate();
        Overlay.removeOverlays();
        DragDrop.setActive(false);
        Sidebar.disableEditing();
        DOM.enableSelection();
    });

    $(document).on(Broadcaster.EVENTS.DOM_CHANGED, function (event)
    {
        //update the max z-index of positioned elements
        base.utils.calculateMaxIndex();

        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function (event)
    {
        //this will end up in menu.js triggering updateContainerWidth()
        Broadcaster.send(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, event);

        //hide the overlays while redrawing
        Overlay.removeOverlays();

        Overlay.buildLayoutTree();

        //redrawing done
        Overlay.showOverlays();

        Broadcaster.send(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function (event)
    {
        Mouse.resetMouse();
    });

    /*
     * EVENTS FOR DRAGGING
     */
    /**
     * Called when we want to enable the whole drag-and-drop system
     */
    $(document).on(Broadcaster.EVENTS.ENABLE_DND, function (event)
    {
        Mouse.allowDrag();
        DragDrop.setActive(true);
        Resizer.activate(true);
    });

    /**
     * Called when we want to disable the whole drag-and-drop system
     */
    $(document).on(Broadcaster.EVENTS.DISABLE_DND, function (event)
    {
        Mouse.disallowDrag();
        DragDrop.setActive(false);
        Resizer.activate(false);
    });

    /**
     * Called when a user starts dragging a block
     */
    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event, eventData)
    {
        //Broadcaster.zoom();
        DOM.disableContextMenu();
        DragDrop.dragStarted(event, eventData);
        Sidebar.disableEditing();
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

    /*
     * Edit properties
     * */
    $(document).on(Broadcaster.EVENTS.START_EDIT_FIELD, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
    });

    $(document).on(Broadcaster.EVENTS.END_EDIT_FIELD, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DOM_CHANGED, event);
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, event);
    });
}]);

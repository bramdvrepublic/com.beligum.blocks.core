/**
 * Created by wouter on 19/01/15.
 *
 * The manager is the central point. here we catch all the events to keep an overview
 */
base.plugin("blocks.core.Manager", ["constants.blocks.common", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DragDrop", "blocks.core.Resizer", "blocks.core.Highlighter", "blocks.core.Overlay", "blocks.core.Edit", "blocks.core.DomManipulation", "blocks.core.Sidebar", function (Constants, Broadcaster, Mouse, DragDrop, Resizer, Highlighter, Overlay, Edit, DOM, Sidebar)
{

    /*
     * Because there is no good place to put this we hang this here to the base inside a utils package
     * This is referenced from different lot of locations
     * */
    base.utils = base.functions || {};
    base.utils.maxIndex = 0;
    base.utils.calculateMaxIndex = function ()
    {
        this.maxIndex = Math.max.apply(null, $.map($('body  *'), function (e, n)
            {
                if ($(e).css('position') == 'absolute' || $(e).css('position') == 'relative')
                    return parseInt($(e).css('z-index')) || 1;
            })
        );
    };

    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function ()
    {
        Broadcaster.setContainer(null);
        Broadcaster.registerMouseMove();

        //TODO annoying while debugging
        //window.onbeforeunload = function() {
        //    return 'Ben je zeker dat je deze pagina wil verlaten?';
        //};

        // prevent all clicks to links
        //$(document).on("click.blocks_manager", function (event)
        //{
        //    if (event.which == 1) {
        //        event.preventDefault();
        //    }
        //});

        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE, null);
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function ()
    {
        window.onbeforeunload = function() {};
        Broadcaster.unregisterMouseMove();
        Overlay.removeOverlays();
        Broadcaster.setContainer(null);

        $(document).off("click.blocks_manager");

        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
    });


    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event)
    {
        Mouse.activate();
        DOM.disableSelection();
        Broadcaster.resetHover();

        DragDrop.setActive(true);
        Sidebar.enableEditing();
        //DragCreate.activate();
        Highlighter.showBlockOverlay(Broadcaster.block().current);
        Highlighter.showPropertyOverlay(Broadcaster.property().current);
    });

    $(document).on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, function ()
    {
        Mouse.deactivate();
        Broadcaster.resetHover();
        DragDrop.setActive(false);
        Sidebar.disableEditing();
        DOM.enableSelection();
    });


    $(document).on(Broadcaster.EVENTS.DOM_DID_CHANGE, function ()
    {
        Broadcaster.resetHover();
        base.utils.calculateMaxIndex();
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, null);
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT);
        Overlay.removeOverlays();
        Broadcaster.buildLayoutTree();
        Highlighter.showBlockOverlay(Broadcaster.block().current);
        Highlighter.showPropertyOverlay(Broadcaster.property().current);
        //Logger.debug("Calculate overlay: ");
        //Logger.debug(Broadcaster.getContainer().findElements(0, 9));
        //

        Overlay.showOverlays();
        Broadcaster.send(Broadcaster.EVENTS.DID_REFRESH_LAYOUT);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function ()
    {
        Mouse.resetMouse();
    });


    /*
     * HOVER EVENTS
     * */
    $(document).on(Broadcaster.EVENTS.HOVER_ENTER_PROPERTY, function (event)
    {
        Highlighter.showPropertyOverlay(event.property.current);

    });

    $(document).on(Broadcaster.EVENTS.HOVER_LEAVE_PROPERTY, function (event)
    {
        Highlighter.removePropertyOverlay()

    });

    $(document).on(Broadcaster.EVENTS.HOVER_ENTER_BLOCK, function (event)
    {
        Highlighter.showBlockOverlay(event.block.current);
    });

    $(document).on(Broadcaster.EVENTS.HOVER_LEAVE_BLOCK, function (event)
    {
        Highlighter.removeBlockOverlay();

    });

    $(document).on(Broadcaster.EVENTS.HOVER_OVER_BLOCK, function (event)
    {
    });


    /*
     * EVENTS FOR DRAGGING
     * */

    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event)
    {
        //Broadcaster.zoom();
        DOM.disableContextMenu();
        Highlighter.removeBlockOverlay();
        Highlighter.removePropertyOverlay();
        DragDrop.dragStarted(event);
        Sidebar.disableEditing();
    });

    $(document).on(Broadcaster.EVENTS.END_DRAG, function (event)
    {
        //Broadcaster.unzoom();

        DOM.enableContextMenu();
        DragDrop.dragEnded(event);
    });

    $(document).on(Broadcaster.EVENTS.ABORT_DRAG, function (event)
    {
        //Broadcaster.unzoom();
        DOM.enableContextMenu();
        DragDrop.dragAborted(event);
        Highlighter.showBlockOverlay(Broadcaster.block().current);
        Highlighter.showPropertyOverlay(Broadcaster.property().current);
    });

    $(document).on(Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG, function (event)
    {
        Mouse.disallowDrag();
        DragDrop.setActive(false);
        Resizer.activate(false);
    });

    $(document).on(Broadcaster.EVENTS.DO_ALLOW_DRAG, function (event)
    {
        Mouse.allowDrag();
        DragDrop.setActive(true);
        Resizer.activate(true);
    });
    $(document).on(Broadcaster.EVENTS.ENABLE_BLOCK_DRAG, function (event)
    {
        DragDrop.setActive(true);
        Resizer.activate(true);
    });

    $(document).on(Broadcaster.EVENTS.DISABLE_BLOCK_DRAG, function (event)
    {
        DragDrop.setActive(false);
    });

    $(document).on(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK, function (event)
    {
        DragDrop.dragLeaveBlock(event)
    });
    $(document).on(Broadcaster.EVENTS.DRAG_OVER_BLOCK, function (event)
    {
        DragDrop.dragOverBlock(event)
    });
    $(document).on(Broadcaster.EVENTS.DRAG_ENTER_BLOCK, function (event)
    {
        DragDrop.dragEnterBlock(event)
    });

    /*
     * Edit properties
     * */

    $(document).on(Broadcaster.EVENTS.START_EDIT_FIELD, function (event)
    {
        Highlighter.removeBlockOverlay();
        Highlighter.removePropertyOverlay();
        Overlay.removeOverlays();
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
    });

    $(document).on(Broadcaster.EVENTS.END_EDIT_FIELD, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
        Overlay.showOverlays();
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
    });


}]);

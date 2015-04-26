/**
 * Created by wouter on 19/01/15.
 *
 * The manager is the central point. here we catch all the events to keep an overview
 */
base.plugin("blocks.core.Manager", ["blocks.core.Constants", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DragDrop", "blocks.core.Resizer", "blocks.core.BlockMenu", "blocks.core.Highlighter", "blocks.core.Overlay", "blocks.core.Edit", "blocks.core.DomManipulation", "blocks.core.DragCreate", function (Constants, Broadcaster, Mouse, DragDrop, Resizer, BlockMenu, Highlighter, Overlay, Edit, DOM, DragCreate)
{

    // On Window resize
    var resizeTimeout = null;
    $(window).on("resize.blocks_broadcaster", function ()
    {

        if (resizeTimeout != null) {
            clearTimeout(resizeTimeout);
            resizeTimeout = null;
            Logger.debug("timeout cleared")
        } else {
            Logger.debug("timeout not cleared");
            Overlay.removeOverlays();
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        }
        var layoutContainer = Broadcaster.getContainer() == null ? null : Broadcaster.getContainer().element;
        resizeTimeout = setTimeout(function ()
        {
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, layoutContainer);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            resizeTimeout = null;
        }, 700);

    });

    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function ()
    {
        Broadcaster.setContainer(null);
        Broadcaster.registerMouseMove();
        BlockMenu.createMenu();

        // prevent all clicks to links
        $(document).on("click.blocks_manager", function (event)
        {
            if (event.which == 1) {
                event.preventDefault();
            }
        });

        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE, null);
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function ()
    {
        Broadcaster.unregisterMouseMove();
        Overlay.removeOverlays();
        Broadcaster.setContainer(null);
        BlockMenu.removeMenu();

        $(document).off("click.blocks_manager");

        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
    });


    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event)
    {
        Mouse.activate();
        DOM.disableSelection();
        Broadcaster.resetHover();

        DragDrop.setActive(true);
        DragCreate.activate();
        Highlighter.showBlockOverlay(Broadcaster.block().current);
        Highlighter.showPropertyOverlay(Broadcaster.property().current);


    });

    $(document).on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, function ()
    {
        Mouse.deactivate();
        Broadcaster.resetHover();
        DragCreate.deactivate()
        DragDrop.setActive(false);
        DOM.enableSelection();

    });


    $(document).on(Broadcaster.EVENTS.DOM_DID_CHANGE, function ()
    {
        Broadcaster.resetHover();
        Constants.calculateMaxIndex();
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
    $(document).on(Broadcaster.EVENTS.HOOVER_ENTER_PROPERTY, function (event)
    {
        Highlighter.showPropertyOverlay(event.property.current);

    });

    $(document).on(Broadcaster.EVENTS.HOOVER_LEAVE_PROPERTY, function (event)
    {
        Highlighter.removePropertyOverlay()

    });

    $(document).on(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK, function (event)
    {
        Highlighter.showBlockOverlay(event.block.current);
        BlockMenu.showMenu(event.block.current);
    });

    $(document).on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, function (event)
    {
        Highlighter.removeBlockOverlay();
        if (!BlockMenu.mouseOverMenu()) {
            BlockMenu.hideMenu();
        }
    });

    $(document).on(Broadcaster.EVENTS.HOOVER_OVER_BLOCK, function (event)
    {
    });


    /*
     * EVENTS FOR DRAGGING
     * */

    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event)
    {

        //Broadcaster.zoom();
        DOM.disableContextMenu();
        BlockMenu.hideMenu();
        Highlighter.removeBlockOverlay();
        Highlighter.removePropertyOverlay();
        DragDrop.dragStarted(event);
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
        BlockMenu.hideMenu();
        Broadcaster.property().current.editFunction(event);

    });

    $(document).on(Broadcaster.EVENTS.END_EDIT_FIELD, function (event)
    {
        Edit.endEdit();
        Overlay.showOverlays();
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
    });


}]);

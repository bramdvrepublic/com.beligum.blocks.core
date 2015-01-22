/**
 * Created by wouter on 19/01/15.
 */
blocks.plugin("blocks.core.Manager", ["blocks.core.Constants", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DragDrop", "blocks.core.Resizer", "blocks.core.BlockMenu", "blocks.core.Highlighter",  function(Constants, Broadcaster, Mouse, DragDrop, Resizer, BlockMenu, Highlighter) {

    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function() {
        Broadcaster.setContainer(null);
        Broadcaster.registerMouseMove();
        BlockMenu.createMenu();

        // prevent all clicks to links
        $(document).on("click.blocks_manager", function(event) {
            if (event.which == 1) {
                event.preventDefault();
            }
        });

        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, null);
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function() {
        Broadcaster.unregisterMouseMove();
        Broadcaster.setContainer(null);
        BlockMenu.removeMenu();

        $(document).off("click.blocks_manager");

        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
    });


    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event) {
        Mouse.activate();
        Broadcaster.resetHover();

        DragDrop.setActive(true)
        Resizer.activate(true);
        Highlighter.showBlockOverlay(Broadcaster.block().current);
        Highlighter.showPropertyOverlay(Broadcaster.property().current);


    });

    $(document).on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, function () {
        Mouse.deactivate();
        Broadcaster.resetHover();

        DragDrop.setActive(false)
        Resizer.activate(false);
        Highlighter.removeBlockOverlay();
        Highlighter.removePropertyOverlay();
        Mouse.enableSelection();

    });



    $(document).on(Broadcaster.EVENTS.DOM_DID_CHANGE, function() {
        var layoutContainer = Broadcaster.getContainer().element;
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, layoutContainer);
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function(event) {
        Broadcaster.send(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT);
        Broadcaster.setLayoutParent(event.custom);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function () {
        Mouse.resetMouse();

    });


    /*
    * HOVER EVENTS
    * */
    $(document).on(Broadcaster.EVENTS.HOOVER_ENTER_PROPERTY, function (event) {
        Highlighter.showPropertyOverlay(event.property.current);

    });

    $(document).on(Broadcaster.EVENTS.HOOVER_LEAVE_PROPERTY, function (event) {
        Highlighter.removePropertyOverlay()

    });

    $(document).on(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK, function (event) {
        Resizer.manageActiveResizeHandle(event)
        Highlighter.showBlockOverlay(event.block.current);
        BlockMenu.showMenu(event.block.current);
    });

    $(document).on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, function (event) {
        Resizer.manageActiveResizeHandle(event);
        Highlighter.removeBlockOverlay()
        if(!BlockMenu.mouseOverMenu()) {
            BlockMenu.hideMenu();
        }
    });

    $(document).on(Broadcaster.EVENTS.HOOVER_OVER_BLOCK, function (event) {
        Resizer.manageActiveResizeHandle(event)
    });




    /*
    * EVENTS FOR DRAGGING
    * */

    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event) {
        Broadcaster.zoom();
        Mouse.disableContextMenu();
        BlockMenu.hideMenu();
        Highlighter.removeBlockOverlay();
        Highlighter.removePropertyOverlay();
        DragDrop.dragStarted(event)
        Resizer.startDrag(event)
    });

    $(document).on(Broadcaster.EVENTS.END_DRAG, function (event) {
        Broadcaster.unzoom();
        Broadcaster.resetHover();
        Mouse.enableContextMenu();
        DragDrop.dragEnded(event);
        Resizer.endDrag(event);
        Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
        Highlighter.showBlockOverlay(Broadcaster.block().current);
        Highlighter.showPropertyOverlay(Broadcaster.property().current);
    });

    $(document).on(Broadcaster.EVENTS.ABORT_DRAG, function (event) {
        Broadcaster.unzoom();
        Broadcaster.resetHover();
        Mouse.enableContextMenu();
        DragDrop.dragAborted(event);
        Resizer.endDrag(event)
        Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
        Highlighter.showBlockOverlay(Broadcaster.block().current);
        Highlighter.showPropertyOverlay(Broadcaster.property().current);
    });

    $(document).on(Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG, function (event) {
        Logger.debug("dragging not allowed");
        Mouse.disallowDrag();
        DragDrop.setActive(false)
        Resizer.activate(false);
    });

    $(document).on(Broadcaster.EVENTS.DO_ALLOW_DRAG, function (event) {
        Logger.debug("dragging allowed");
        Mouse.allowDrag();
        DragDrop.setActive(true)
        Resizer.activate(true);
    });
    $(document).on(Broadcaster.EVENTS.ENABLE_BLOCK_DRAG, function (event) {
        DragDrop.setActive(true)
        Resizer.activate(true);
    });

    $(document).on(Broadcaster.EVENTS.DISABLE_BLOCK_DRAG, function (event) {
        Logger.debug("dragging disabled");
        DragDrop.setActive(false);
    });

    $(document).on(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK, function (event) {
        DragDrop.dragLeaveBlock(event)
    });
    $(document).on(Broadcaster.EVENTS.DRAG_OVER_BLOCK, function (event) {
        DragDrop.dragOverBlock(event)
    });
    $(document).on(Broadcaster.EVENTS.DRAG_ENTER_BLOCK, function (event) {
        DragDrop.dragEnterBlock(event)
    });








}]);

blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Overlay", function(Broadcaster, Overlay) {

    var editor = null;

    Broadcaster.on(Broadcaster.EVENTS.DOUBLE_CLICK_BLOCK, "blocks.core.Edit", function(event) {
        editBlock(event.blockEvent);
    });

    var editBlock = function(blockEvent) {
        if (blockEvent.block.current != null) {
            if (blockEvent.block.current.canEdit()) {
                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
                var element = blockEvent.block.current;
                Overlay.createForBlock(blockEvent.block.current, function() {
                    Broadcaster.send(new Broadcaster.EVENTS.DOM_DID_CHANGE());
                    Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());
                    editor.destroy();
                    $(element).attr("contenteditable", "");

                });
                $(blockEvent.block.current.element).attr("contenteditable", true);
                $(blockEvent.block.current.element).focus();
                editor = $(blockEvent.block.current.element).ckeditor().editor;
            }
        }
    }



}]);
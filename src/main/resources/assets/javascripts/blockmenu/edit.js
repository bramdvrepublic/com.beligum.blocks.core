blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Overlay", "blocks.core.BlockMenu", function(Broadcaster, Overlay, Menu) {
    var edit = this;
    var editor = null;

    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-pencil"></i></div>');

    var isIframe = function(currentBlock) {
        return currentBlock.element.find("iframe").length == 1;
    };

    var enabled = function(currentBlock) {
        retVal = false;
        if (currentBlock.canEdit || isIframe(currentBlock)) {
            retVal = true;
        }
        return retVal;
    };

    Menu.addButton({
        element: button,
        priority: 100,
        enabled: enabled
    });

    button.click(function() {
        edit.editBlock(Menu.currentBlock());
    });

    // Double click is edit
    Broadcaster.on(Broadcaster.EVENTS.DOUBLE_CLICK_BLOCK, "blocks.core.Edit", function(event) {
        edit.editBlock(event.blockEvent.block.current);
    });

    this.editBlock = function(block) {
        if (block != null) {
            if (enabled(block) ) {
                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
                var element = block.element;
                if (!isIframe(block) && editor == null) {
                    Overlay.createForBlock(block, function () {
                        Broadcaster.send(new Broadcaster.EVENTS.DOM_DID_CHANGE());
                        Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());
                        editor.destroy();
                        editor = null;
                        $(element).removeAttr("contenteditable");

                    });
                    $(element).attr("contenteditable", true);
                    $(element).focus();
                    editor = $(element).ckeditor().editor;
                } else {
                    var iframe = element.find("iframe");
                    Overlay.createForBlock(block, function () {
                        $(iframe.removeClass("edit"));
                        Broadcaster.send(new Broadcaster.EVENTS.DOM_DID_CHANGE());
                        Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());

                    });
                    $(iframe.addClass("edit"));
                }
            }
        }
    }



}]);
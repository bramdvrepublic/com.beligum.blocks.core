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
    $(document).on(Broadcaster.EVENTS.DOUBLE_CLICK_BLOCK, function(event) {
        edit.editBlock(event.block.current);
    });

    this.editBlock = function(block) {
        if (block != null) {
            if (enabled(block) ) {
                Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
                doEditText(block);
                doEditIframe(block);
            }
        }
    }

    var doEditText = function(block) {
        if (!isIframe(block) && editor == null) {
            var element = block.element;
            Overlay.createForBlock(block, function () {
                Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                editor.destroy();
                editor = null;
                $(element).removeAttr("contenteditable");

            });
            $(element).attr("contenteditable", true);
            $(element).focus();
            editor = $(element).ckeditor().editor;
        }
    }

    var doEditIframe = function(block) {
        if (isIframe(block)) {
            editor == null;
            var iframe = block.element.find("iframe");
            Overlay.createForBlock(block, function () {
                $(iframe.removeClass("edit"));
                Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);

            });
            $(iframe.addClass("edit"));
        }
    }





}]);
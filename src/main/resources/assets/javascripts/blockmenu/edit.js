blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Overlay", "blocks.core.DomManipulation", function(Broadcaster, Overlay, DOM) {
    var Edit = this;
    var editor = null;

    var isIframe = function(currentBlock) {
        return currentBlock.element.find("iframe").length == 1;
    };

    // Double click is edit
    $(document).on(Broadcaster.EVENTS.CLICK_BLOCK, function(event) {
        event.preventDefault();
        event.stopPropagation();
//        if (event.block != null && event.block.current != null && enabled(event.block.current)) {

            Edit.editBlock(event);
//        }
    });

    this.editBlock = function(event) {
        var block = event.block.current;
        if (editor != null) {
            event.preventDefault();
            event.stopPropagation();
        } else if (block != null) {
            // find property with can-edit
            var property = block.getProperty(event.pageX, event.pageY);
            if (DOM.canLayout(block.element)) {
                doZoom(block);
            } else if (DOM.canEdit(block.element)) {
                doEditText(block);
            } else if (property != null) {
                if (property.element.prop("tagName") == 'IFRAME') {
                    doEditIframe(property.element);
                } else {
                    doEditTextInline(property.element);
                }

            }
        }
    };

    var doEditText = function(block) {

        var element = block.element;
        Overlay.createForBlock(block, function () {
            removeEditor();
            Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            editor.destroy();
            editor = null;
            $(element).removeAttr("contenteditable");
        });

        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        $(element).attr("contenteditable", true);
        $(element).focus();
        editor = $(element).ckeditor().editor;
    };


    var doEditTextInline = function(element) {

        Overlay.createForElement(element, function () {
            element.off("click.blocks-edit");
            Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            element.removeAttr("contenteditable");
            removeEditor();
        });

        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        element.attr("contenteditable", true);
        editor = new Medium({element: element[0], mode: Medium.inlineMode});
        element.on("click.blocks-edit", function(e) {
            e.preventDefault();
            e.stopPropagation();
        });
        element.focus();
    };

    var doEditIframe = function(element) {
        var iframe = element;
        removeEditor();
        Overlay.createForElement(block, function () {
            $(iframe.removeClass("edit"));
            Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        });

        $(iframe.addClass("edit"));
    };

    var doZoom = function(block) {
        removeEditor();
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        Broadcaster.setLayoutParent($(block.element));
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);

        Overlay.createForBlock(block, function () {
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            Broadcaster.setLayoutParent(null);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        });
    }

    var removeEditor = function() {
        if (editor != null) {
            editor.destroy();
            editor = null;
        }
    }



}]);
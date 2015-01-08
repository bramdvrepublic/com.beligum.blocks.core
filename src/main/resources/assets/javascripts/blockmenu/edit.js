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
            var doEdit = null;
            var editableElement = null;
            if (DOM.canLayout(block.element)) {
                doEdit = doZoom;
                editableElement = block.element;
            } else if (DOM.canEdit(block.element) && property == null) {
                doEdit = editFunction(block.element);
                editableElement = block.element;
            } else if (property != null && DOM.canEdit(property.element)) {
                doEdit = editFunction(property.element);
                editableElement = property.element;
            }
            if (doEdit != null) {
                doEdit(editableElement);
            }
        }
    };

    var doEditText = function(element) {

        Overlay.createForElement(element, function () {
            element.off("click.blocks-edit");
            removeEditor();
            Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            removeEditor();
            $(element).removeAttr("contenteditable");
        });

        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        $(element).attr("contenteditable", true);
        $(element).focus();

        element.on("click.blocks-edit", function(e) {
            e.preventDefault();
            e.stopPropagation();
        });

        var zindex = $(element).css("z-index");
        editor = $(element).ckeditor().editor;
        $(element).css("z-index", zindex);
    };


    var doEditTextInline = function(element) {
        //var element = event.block.current.element;

        Overlay.createForElement(element, function () {
            element.off("click.blocks-edit");
            Overlay.unhighlightElementAsProperty(element);
            Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            element.removeAttr("contenteditable");
            removeEditor();
        });

        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        element.attr("contenteditable", true);
        Overlay.highlightElementAsProperty(element);
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

    var doZoom = function(element) {
        removeEditor();
        var lastParent = Broadcaster.layoutParentElement;
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        Broadcaster.setLayoutParent($(element));
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);

        Overlay.createForElement(element, function () {
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            Broadcaster.setLayoutParent(lastParent);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        });
    }

    var removeEditor = function() {
        if (editor != null) {
            editor.destroy();
            editor = null;
        }
    }

    var registeredByType = {};
    this.registerByType = function(type, callback) {
        registeredByType[type] = callback;
    }

    var registeredByTag = {};
    this.registerByTag = function(tag, callback) {
        registeredByTag[tag] = callback;
    }

    var editFunction = function(element) {
        var retVal = null
        if (DOM.isEntity(element)) {
            var t = element.attr(Constants.IS_ENTITY);
            retVal = registeredByType[t];
        }

        if (retVal == null) {
            retVal = registeredByTag[element.prop("tagName")];
        }
        return retVal;
    }

    Edit.registerByTag("IFRAME", doEditIframe);
    Edit.registerByTag("DIV", doEditText);
    Edit.registerByTag("P", doEditText);

    Edit.registerByTag("H1", doEditTextInline);
    Edit.registerByTag("H2", doEditTextInline);
    Edit.registerByTag("H3", doEditTextInline);
    Edit.registerByTag("H4", doEditTextInline);
    Edit.registerByTag("H5", doEditTextInline);
    Edit.registerByTag("H6", doEditTextInline);
    Edit.registerByTag("A", doEditTextInline);

}]);
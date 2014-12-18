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
            } else if (DOM.isProperty(block.element) && DOM.canEdit(block.element)) {
                if (block.getProperties() > 0) {
                    doZoom(block);
                } else {
                    // edit based in html tag
                    doEditTextInline(event);
                }
            } else if (DOM.canEdit(block.element)) {
                doEditText(block);
            } else if (property != null) {
                if (property.element.prop("tagName") == 'IFRAME') {
                    doEditIframe(property.element);
                } else {
                    doEditTextInline(event);
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
            removeEditor();
            $(element).removeAttr("contenteditable");
        });

        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        $(element).attr("contenteditable", true);
        $(element).focus();
        var zindex = $(element).css("z-index");
        editor = $(element).ckeditor().editor;
        $(element).css("z-index", zindex);
    };


    var doEditTextInline = function(event) {
        var element = event.block.current.element;

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
        getMouseEventCaretRange(event.pageX, event.pageY);
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


    // http://stackoverflow.com/questions/18643515/how-to-create-a-range-object-from-a-point-x-and-y-coordinates
    function getMouseEventCaretRange(evt) {
        var range, x = evt.clientX, y = evt.clientY;

        // Try the simple IE way first
        if (document.body.createTextRange) {
            range = document.body.createTextRange();
            range.moveToPoint(x, y);
        }

        else if (typeof document.createRange != "undefined") {
            // Try Mozilla's rangeOffset and rangeParent properties,
            // which are exactly what we want
            if (typeof evt.rangeParent != "undefined") {
                range = document.createRange();
                range.setStart(evt.rangeParent, evt.rangeOffset);
                range.collapse(true);
            }

            // Try the standards-based way next
            else if (document.caretPositionFromPoint) {
                var pos = document.caretPositionFromPoint(x, y);
                range = document.createRange();
                range.setStart(pos.offsetNode, pos.offset);
                range.collapse(true);
            }

            // Next, the WebKit way
            else if (document.caretRangeFromPoint) {
                range = document.caretRangeFromPoint(x, y);
            }
        }

        window.setTimeout(function() {
            selectRange(range);
        }, 10);
    }

    var selectRange = function(range) {
        if (range) {
            if (typeof range.select != "undefined") {
                range.select();
            } else if (typeof window.getSelection != "undefined") {
                var sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(range);
            }
        }
    }



}]);
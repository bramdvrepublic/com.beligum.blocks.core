blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Overlay", "blocks.core.DomManipulation", function(Broadcaster, Overlay, DOM) {
    var Edit = this;
    var editors = [];
    var lastPoint = {x:0, y:0};

    CKEDITOR.disableAutoInline = true;

    var isIframe = function(currentBlock) {
        return currentBlock.element.find("iframe").length == 1;
    };


    this.makeEditable = function(element) {

            if (DOM.canEdit(element)) {
                doEdit = editFunction(element);
            }

            if (doEdit != null) {
                doEdit(element);
            }

    };

    var doEditText = function(element) {

//        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);

        $(element).attr("contenteditable", true);
        var editor = $(element).ckeditor().editor;


        editors.push(function() {
            editor.destroy();
            element.removeAttr("contenteditable");
        });

    };


    var doEditTextInline = function(element) {

//        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        element.attr("contenteditable", true);
        var editor = new Medium({element: element[0], mode: Medium.inlineMode});

        editors.push(function() {
            editor.destroy();
            element.removeAttr("contenteditable");
        });
    };

//    var doEditIframe = function(element) {
//        var iframe = element;
//        removeEditor();
//        Overlay.createForElement(block, function () {
//            $(iframe.removeClass("edit"));
//            Broadcaster.send(Broadcaster.EVENTS.DOM_DID_CHANGE);
//            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
//        });
//
//        $(iframe.addClass("edit"));
//    };

//    var doZoom = function(element) {
//        removeEditor();
//
//    }

    var removeEditors = function() {
        for (var i = 0; i < editors.length; i++) {
            editors[i]();
        }
        editors = [];
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

//    Edit.registerByTag("IFRAME", doEditIframe);
    Edit.registerByTag("DIV", doEditText);
    Edit.registerByTag("P", doEditText);

    Edit.registerByTag("H1", doEditTextInline);
    Edit.registerByTag("H2", doEditTextInline);
    Edit.registerByTag("H3", doEditTextInline);
    Edit.registerByTag("H4", doEditTextInline);
    Edit.registerByTag("H5", doEditTextInline);
    Edit.registerByTag("H6", doEditTextInline);
    Edit.registerByTag("A", doEditTextInline);
    Edit.registerByTag("SPAN", doEditTextInline);

    $(document).on(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, function() {
        removeEditors();
    });

}]);
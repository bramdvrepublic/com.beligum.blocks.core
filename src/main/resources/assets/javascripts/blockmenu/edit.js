blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.Overlay", "blocks.core.DomManipulation", function(Broadcaster, Constants, Overlay, DOM) {
    var Edit = this;
    var editors = [];
    var lastPoint = {x:0, y:0};

    CKEDITOR.disableAutoInline = true;

    var isIframe = function(currentBlock) {
        return currentBlock.element.find("iframe").length == 1;
    };


    var makeEditable = function(event) {
            var element = event.property.current.element;
            var doEdit = null;
            if (DOM.canEdit(element)) {
                doEdit = editFunction(event);
            }

            if (doEdit != null) {
                Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
                doEdit(event);
            }
    };


    var doEditText = function(blockEvent) {
        element = blockEvent.property.current.element;
        // Preparation
        $(element).attr("contenteditable", true);
        var editor = $(element).ckeditor().editor;

        var setCursor = function() {
            var caretPosition = document.caretRangeFromPoint(blockEvent.clientX, blockEvent.clientY);

            var sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(caretPosition);
        };

        if (editor.status == "unloaded") {
            editor.on("instanceReady", function() {
                setCursor();
            })
        } else {
            setCursor();
        }

        editor.on("blur", function() {
            editor.destroy();
            element.removeAttr("contenteditable");
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        })



    };


    var doEditTextInline = function(blockEvent) {
        element = blockEvent.property.current.element;

        element.attr("contenteditable", true);
        var editor = new Medium({element: element[0], mode: Medium.inlineMode});

        var setCursor = function() {
            var caretPosition = document.caretRangeFromPoint(blockEvent.clientX, blockEvent.clientY);

            var sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(caretPosition);
        };
        setCursor();

        element.on("blur", function() {
            editor.destroy();
            element.removeAttr("contenteditable");
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        })

    };


    var removeEditors = function() {
        var temp = editors;
        editors = [];
//        for (var i = 0; i < temp.length; i++) {
        while(temp.length > 0) {
            try {
                var editor = temp.pop();
                editor();

            } catch (e) {
                var x = 0;
            }

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

    var editFunction = function(event) {
        var element = event.property.current.element;
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

    $(document).on(Broadcaster.EVENTS.REGISTER_FIELD, function(event) {

        makeEditable(event);
    });

    $(document).on(Broadcaster.EVENTS.UNREGISTER_FIELDS, function(event) {
        removeEditors();
    });


    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function() {
        removeEditors();
    })

}]);
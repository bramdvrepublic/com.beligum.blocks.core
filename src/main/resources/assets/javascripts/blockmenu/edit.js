blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.Overlay", "blocks.core.DomManipulation", function(Broadcaster, Constants, Overlay, DOM) {
    var Edit = this;
    var editors = [];
    var lastPoint = {x:0, y:0};

    CKEDITOR.disableAutoInline = true;

    var isIframe = function(currentBlock) {
        return currentBlock.element.find("iframe").length == 1;
    };


    var makeEditable = function(element) {
            var doEdit = null;
            if (DOM.canEdit(element)) {
                doEdit = editFunction(element);
            } else if (DOM.canLayout(element)) {
                doEdit = doLayout;
            }

            if (doEdit != null) {
                doEdit(element);
            }

    };

    var doLayout = function(element) {
        $(element).addClass(Constants.PROPERTY_CLASS);



        $(element).on("click.edit_canlayout", function(event) {
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, element);
        });

        editors.push(function() {
            $(element).off("click.edit_canlayout");
            $(element).removeClass(Constants.PROPERTY_CLASS);
        });
    };

    var doEditText = function(element) {

        // Preparation
        $(element).attr("contenteditable", true);
        var editor = $(element).ckeditor().editor;
        $(element).addClass(Constants.PROPERTY_CLASS);

        // Forced Click
        $(element).on(Broadcaster.EVENTS.FAKE_FIELD_CLICK, function(fakeFieldEvent) {
            var setCursor = function() {
                var caretPosition = document.caretRangeFromPoint(fakeFieldEvent.custom.clientX, fakeFieldEvent.custom.clientY);

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

        });


        // to remove
        editors.push(function() {
            $(element).off(Broadcaster.EVENTS.FAKE_FIELD_CLICK);
            $(element).removeClass(Constants.PROPERTY_CLASS);
            editor.destroy();
            element.removeAttr("contenteditable");
        });





    };


    var doEditTextInline = function(element) {


        element.attr("contenteditable", true);
        var editor = new Medium({element: element[0], mode: Medium.inlineMode});
        $(element).addClass(Constants.PROPERTY_CLASS);


        $(element).on(Broadcaster.EVENTS.FAKE_FIELD_CLICK, function(fakeFieldEvent) {
            var setCursor = function() {
                var caretPosition = document.caretRangeFromPoint(fakeFieldEvent.custom.clientX, fakeFieldEvent.custom.clientY);

                var sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(caretPosition);
            };
            setCursor();

        });

        editors.push(function() {
            $(element).off(Broadcaster.EVENTS.FAKE_FIELD_CLICK)
            $(element).removeClass(Constants.PROPERTY_CLASS);
            editor.destroy();
            element.removeAttr("contenteditable");
        });



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

    $(document).on(Broadcaster.EVENTS.REGISTER_FIELD, function(event) {
        makeEditable(event.custom);
    });

    $(document).on(Broadcaster.EVENTS.UNREGISTER_FIELDS, function(event) {
        removeEditors();
    });


    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function() {
        removeEditors();
    })

}]);
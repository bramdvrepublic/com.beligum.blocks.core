blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.Overlay", "blocks.core.DomManipulation", function(Broadcaster, Constants, Overlay, DOM) {
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


        $(element).attr("contenteditable", true);
        var editor = $(element).ckeditor().editor;
        $(element).addClass(Constants.PROPERTY_CLASS);

        editors.push(function() {
            $(element).removeClass(Constants.PROPERTY_CLASS);
            editor.destroy();
            element.removeAttr("contenteditable");
        });



    };


    var doEditTextInline = function(element) {

        $(element).on("focus", function() {
            Overlay.overlayForElement(element);
        });

        element.attr("contenteditable", true);
        var editor = new Medium({element: element[0], mode: Medium.inlineMode});
        $(element).addClass(Constants.PROPERTY_CLASS);
        editors.push(function() {
            $(element).removeClass(Constants.PROPERTY_CLASS);
            editor.destroy();
            element.removeAttr("contenteditable");
        });

//        $(element).on("mouseup", function() {
//            var editor = new Medium({element: element[0], mode: Medium.inlineMode});
//        })
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

    $(document).on(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, function() {
        removeEditors();
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function() {
        removeEditors();
    })

}]);
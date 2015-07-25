/**
 * Created by wouter on 8/07/15.
 */

base.plugin("blocks.core.MediumEditorExtensions", ["base.core.Class", function (Class)
{
    var MediumEditorExtensions = this;

    this.ID_PREFIX = "medium-editor-";

    //extends the dest object with all the properties in source and returns dest
    var extendOptions = function (dest, source)
    {
        var prop;
        dest = dest || {};
        for (prop in source) {
            if (source.hasOwnProperty(prop) && !dest.hasOwnProperty(prop)) {
                dest[prop] = source[prop];
            }
        }
        return dest;
    };

    //-----CLASS DEFINITIONS-----
    //used this as a reference: https://github.com/arcs-/MediumButton
    this.StylesPicker = Class.create(MediumEditor.FormExtension, {

        //-----CONSTANTS-----
        STATIC: {
            NAME: "styles-picker",
            STYLES: []
        },

        editorStyles: [],

        //-----CONSTRUCTORS-----
        constructor: function (options)
        {
            MediumEditorExtensions.StylesPicker.Super.call(this, options);

            this.name = MediumEditorExtensions.StylesPicker.NAME;
            this.options = extendOptions(options, {});
            this.isFormVisible = false;
            this.hasForm = false;
            this.editorStyles = [];
        },

        //-----OVERLOADED FUNCTIONS-----
        createButton: function ()
        {
            return this._createButtonElement().get(0);
        },

        isDisplayed: function ()
        {
            return this.isFormVisible;
        },

        handleClick: function (event)
        {
            //noop, bubble up to bootstrap instead
        },

        getForm: function ()
        {
        },

        hideForm: function ()
        {
        },

        checkState: function ()
        {
            //var html = getCurrentSelection();
            //if (this.options.start != '' && html.indexOf(this.options.start) > -1 && html.indexOf(this.options.end) > -1) {
            //    this.button.classList.add('medium-editor-button-active');
            //}
        },

        //-----OWN FUNCTIONS-----

        /**
         * Styles is an array with objects
         * object is of type {value: "", text: ""}
         * value = "p:red" -> text before the colon is the tag, text after the colon are the classes that will be added
         * nothing after colon will remove all classes, nothing before colon will not touch the tag
         * text is the text in the dropdown
         *
         * ----- example config -----
         * var styles = [
         * This will clear the existing classes, but leave the tag alone
         * {value: ":", text: Messages.p},
         *
         * This is generally the default case: a p without classes (eg. when hitting enter in editor)
         * {value: "p:", text: Messages.p},
         *
         * Will change the tag to <h1> and remove existing classes
         * {value: "h1:", text: Messages.h1},
         *
         * Will change the tag to <h1> clear existing classes and add the classes after the colon
         * {value: "h1:red", text: Messages.h1Red}
         * ];
         * ----------------------------
         */
        setStyles: function (newStyles)
        {
            editorStyles = newStyles;
        },
        getStyles: function ()
        {
            return editorStyles;
        },

        //-----PRIVATE FUNCTIONS-----
        _createButtonElement: function ()
        {
            var button = $('<div class="dropdown btn-group medium-editor-action"/>');
            var toggle = $('<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Style <span class="caret"></span></button>');

            var styles = $('<ul class="dropdown-menu"/>');

            var valueAttr = "data-value";
            for (var i = 0; i < MediumEditorExtensions.StylesPicker.STYLES.length; i++) {
                var val = MediumEditorExtensions.StylesPicker.STYLES[i];

                //note that we bind to this, but pass the data in the function()
                var btn = $('<a href="javascript:void(0)" ' + valueAttr + '="' + val.value + '">' + val.text + '</a>');
                btn.click(btn.attr(valueAttr), function (event)
                {
                    this._onSelect(event.data);
                }.bind(this));

                styles.append($('<li></li>').append(btn));
            }

            button.append(toggle).append(styles);

            return button;
        },
        _onSelect: function (configValue)
        {
            this.base.saveSelection();

            var arguments = configValue.split(':');
            var tag = arguments[0].trim();
            var classes = arguments[1].trim();

            var selectedElements = this._findSelection();

            for (var i = 0; i < selectedElements.length; i++) {
                var el = selectedElements[i].element;
                if (tag != "") {
                    var ne = $("<" + tag + "/>");
                    ne.html(el.html());
                    el.replaceWith(ne);
                    selectedElements[i].element = ne;
                }

                if (classes != "") {
                    selectedElements[i].element.addClass(arguments[1].trim());
                } else {
                    selectedElements[i].element.attr("class", "");
                }
            }

            //make sure the caret is at the same position as before
            this.base.restoreSelection();

            //re-positions the toolbar
            this.base.checkSelection();
        },
        _findSelection: function ()
        {
            var blockContainerElementNames = ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'pre'];
            var elements = this.base.getSelection().getSelectedElements(this.document);
            if (elements.length == 0) {
                elements.push(this.base.getSelection().getSelectedParentElement(this.base.getSelection().getSelectionRange(this.document)));
            }
            // Filter all elements that we will change with our style
            var parents = [];
            var lastElement = null;
            for (var i = 0; i < elements.length; i++) {
                var el = $(elements[i]);
                // From all selected elements, we need only the root block elements
                // so filter the children out
                if (lastElement == null || lastElement.has(el).length == 0) {
                    //don't let the loop go all the way up the DOM, block at body
                    while (!el.attr("content-editable") && el[0].nodeName.toLowerCase() !== "body" && blockContainerElementNames.indexOf(el[0].nodeName.toLowerCase()) == -1) {
                        el = el.parent()
                    }
                    //if we searched all the way up till the body, we couldn't find the parent and something's wrong
                    if (el[0].nodeName.toLowerCase() !== "body") {
                        lastElement = el;
                        // only block elements inside our container can be styled
                        if (!el.attr("content-editable")) {
                            // store original values so we can restore on cancel
                            var block = {};
                            block.element = el;
                            block.classes = el.attr("class") ? el.attr("class") : "";
                            block.tagName = el[0].tagName.toLowerCase();
                            parents.push(block);
                        }
                    }
                }
            }

            return parents;
        }
    });

    this.LinkInput = Class.create(MediumEditor.extensions.anchor, {

        //-----CONSTANTS-----
        STATIC: {
            NAME: "link-input"
        },

        //-----CONSTRUCTORS-----
        constructor: function (options)
        {
            MediumEditorExtensions.LinkInput.Super.call(this, options);

            this.name = MediumEditorExtensions.LinkInput.NAME;
            this.options = extendOptions(options, {});
            this.cssPrefix = this.name + '-';
            this.inputClass = this.cssPrefix + 'input';
            this.confirmBtnClass = this.cssPrefix + 'confirm';
            this.cancelBtnClass = this.cssPrefix + 'cancel';
        },

        //-----OVERLOADED FUNCTIONS-----
        createForm: function ()
        {
            var form = $('<div id="'+('medium-editor-toolbar-form-anchor-' + this.getEditorId())+'" class="form-inline medium-editor-toolbar-form"></div>');
            var formGroup = $('<div class="form-group"></div>').appendTo(form);
            var inputGroup = $('<div class="input-group"></div>').appendTo(formGroup);
            var input = $('<input type="text" class="form-control ' + this.inputClass + '" placeholder="' + this.placeholderText + '">').appendTo(inputGroup);
            var clearBtn = $('<span class="input-btn input-btn-clear"><i class="fa fa-times"></span>').appendTo(inputGroup);
            input.on("change keyup", function (e)
            {
                if (input.val() == null || input.val() == '') {
                    clearBtn.removeClass("show");
                }
                else {
                    clearBtn.addClass("show");
                }
            });
            clearBtn.click(function (e)
            {
                input.val('');
                input.change();
                input.focus();
            });

            var okBtn = $('<a class="btn btn-primary ' + this.confirmBtnClass + '"><i class="fa fa-check"></i></a>').appendTo(form);
            var cancelBtn = $('<a class="btn btn-link" class="' + this.cancelBtnClass + '">cancel</a>').appendTo(form);

            okBtn.click(this.handleSaveClick.bind(this));
            cancelBtn.click(this.handleCloseClick.bind(this));

            return form.get(0);
        },
        getInput: function ()
        {
            return this.getForm().querySelector('.' + this.inputClass);
        },

        //-----OWN FUNCTIONS-----

        //-----PRIVATE FUNCTIONS-----
    });

}]);

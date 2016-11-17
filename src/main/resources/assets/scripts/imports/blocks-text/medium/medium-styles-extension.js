/**
 * Created by wouter on 8/07/15.
 */

base.plugin("blocks.core.MediumEditorExtensions", ["base.core.Class", "blocks.imports.Widget", "blocks.core.Sidebar", "base.core.Commons", "constants.blocks.core", function (Class, Widget, Sidebar, Commons, BlocksConstants)
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
    this.StylesPicker = Class.create(MediumEditor.extensions.form, {

        //-----CONSTANTS-----
        STATIC: {
            NAME: "styles-picker",
            VALUE_ATTR: "data-value"
        },

        editorStyles: [],
        //will keep an object with tag names that contain objects
        allTags: {},

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
            this.editorStyles = newStyles;
        },
        getStyles: function ()
        {
            return this.editorStyles;
        },

        //-----PRIVATE FUNCTIONS-----
        _createButtonElement: function ()
        {
            var id = Commons.generateId();
            var button = $('<div class="dropdown btn-group ' + BlocksConstants.TEXT_EDITOR_STYLES_CLASS + '"/>');
            var toggle = $('<button id="' + id + '" type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Style <span class="caret"></span></button>');

            //indicate the selected style on opening of the dropdown
            var _this = this;
            toggle.click(function (e)
            {
                //only trigger while opening, not on close
                if (toggle.attr('aria-expanded') == 'false') {
                    //reset all combo list items
                    button.find('ul.dropdown-menu li').removeClass('active');

                    var selectedElements = _this._findSelectedElements();
                    for (var i = 0; i < selectedElements.length; i++) {
                        var el = selectedElements[i];

                        //contains a reference in our allTags structure
                        if (el.ref) {
                            el.ref.li.addClass('active');
                        }
                    }
                }
            });

            this.allTags = {};
            var styles = $('<ul class="dropdown-menu" aria-labelledby="' + id + '"/>');
            for (var i = 0; i < this.editorStyles.length; i++) {
                var val = this.editorStyles[i];

                // we'll use the texts with value null as a means to let the user define custom html (like subtitles)
                //note that you should know what's you're doing when using this
                if (val.text == null) {
                    if (val.value != null) {
                        styles.append(val.value);
                    }
                }
                //if it's not a special html case, add a link
                else {
                    //note that we bind to this, but pass the data in the function()
                    var btn = $('<a href="javascript:void(0)" ' + MediumEditorExtensions.StylesPicker.VALUE_ATTR + '="' + val.value + '">' + val.text + '</a>');
                    btn.click(btn.attr(MediumEditorExtensions.StylesPicker.VALUE_ATTR), function (event)
                    {
                        this._onSelect(event.data);

                        //close the dropdown on click, apparently this didn't work automatically...
                        $('#' + id).dropdown("toggle");
                    }.bind(this));


                    var newLi = $('<li></li>').append(btn);
                    styles.append(newLi);

                    //save everything in a structured list
                    //let's void weird config values
                    if (val.value.indexOf(':') >= 0) {
                        var config = val.value.split(':');
                        var tag = config[0].trim().toLowerCase();
                        var clazz = config[1].trim();
                        //uniformly handle empty tags and classes
                        if (!tag) {
                            tag = '';
                        }
                        if (!clazz) {
                            clazz = '';
                        }
                        //don't keep references to empty tags
                        //new entry, create the object
                        if (!(tag in this.allTags)) {
                            this.allTags[tag] = {};
                        }

                        //create a structure (the li will be filled later on, see below)
                        this.allTags[tag][clazz] = {
                            tag: tag,
                            clazz: clazz,
                            text: val.text,
                            value: val.value,
                            li: newLi
                        };
                    }
                }
            }

            button.append(toggle).append(styles);

            return button;
        },
        _onSelect: function (configValue)
        {
            this.base.saveSelection();

            var arguments = configValue.split(':');
            var tag = arguments[0].trim().toLowerCase();
            var clazz = arguments[1].trim();

            var selectedElements = this._findSelectedElements();

            for (var i = 0; i < selectedElements.length; i++) {
                var el = selectedElements[i].element;
                if (tag != "") {
                    var newEl = $("<" + tag + "/>");
                    newEl.html(el.html());
                    el.replaceWith(newEl);
                    selectedElements[i].element = newEl;
                }

                if (clazz != "") {
                    selectedElements[i].element.addClass(clazz);
                }
                else {
                    selectedElements[i].element.removeAttr("class");
                }
            }

            //make sure the caret is at the same position as before
            this.base.restoreSelection();

            //re-positions the toolbar
            this.base.checkSelection();
        },
        _findSelectedElements: function ()
        {
            var _this = this;
            //this will contain all the elements that are currently selected, both parents and children (eg. both <a> and <p> if nested)
            var elements = MediumEditor.selection.getSelectedElements(this.document);
            if (elements.length == 0) {
                elements.push(MediumEditor.selection.getSelectedParentElement(MediumEditor.selection.getSelectionRange(this.document)));
            }

            // Filter all elements that we will change with our style
            var retVal = [];
            for (var i = 0; i < elements.length; i++) {
                var el = $(elements[i]);
                var elTag = el[0].nodeName.toLowerCase();

                //1: only select elements _inside_ our editor (note that Medium Editor overloads the standard content-editable attribute)
                //2: safety check: don't let the loop go all the way up the DOM, block at body
                while (!el.attr("content-editable") && elTag !== "body") {

                    //We distinguish between three major cases:
                    // 1) Both class and tag match (eg. h1:red)
                    // 2) Only tag matches (eg. h1:)
                    // 3) Only class matches (eg. :red)

                    //1) and 2)
                    if (elTag in this.allTags) {
                        //1) first try to find the most specific match: tag+class
                        var addedNew = false;
                        $.each(this.allTags[elTag], function (key, value)
                        {
                            if (key!='' && el.hasClass(key)) {
                                retVal.push({
                                    element: el,
                                    nodeName: elTag,
                                    ref: _this.allTags[elTag][key]
                                });
                                addedNew = true;
                            }
                        });

                        //2) if no matching class was found, use the general case
                        //note that we nee to include this match because this method is also used to return matches that will receive a new class
                        if (!addedNew) {
                            retVal.push({
                                element: el,
                                nodeName: elTag,
                                ref: this.allTags[elTag]['']
                            });
                        }
                    }

                    //3)
                    //Note: the empty tag is used to store all the general classes; it must be there to make sense
                    //Also note this always need to run, because we can have multiple matches
                    if (this.allTags['']) {
                        //iterate over all general classes
                        $.each(this.allTags[''], function (key, value)
                        {
                            if (key!='' && el.hasClass(key)) {
                                retVal.push({
                                    element: el,
                                    nodeName: elTag,
                                    ref: _this.allTags[''][key]
                                });
                            }
                        });
                    }

                    //Move up the tag hierarchy
                    el = el.parent();
                    elTag = el[0].nodeName.toLowerCase();
                }
            }

            return retVal;
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
            this.confirmBtnClass = this.cssPrefix + 'confirm';
            this.cancelBtnClass = this.cssPrefix + 'cancel';
        },

        //-----OVERLOADED FUNCTIONS-----
        createForm: function ()
        {
            var form = $('<div id="' + ('medium-editor-toolbar-form-anchor-' + this.getEditorId()) + '" class="form-inline medium-editor-toolbar-form"></div>');

            //TODO this is a fast hack to make the createTextInput() method below work cause we're not subclassing from Widget
            var dummyWidget = new Widget.Class();

            //var inputActions = this.buildInputActions(Sidebar, true, true, null);
            //TODO add the inputActions to the constructor below, but make it work with the sidebar finder
            var formGroup = dummyWidget.createTextInput(Sidebar,
                function getterFunction()
                {
                    //return element.attr(attribute);
                },
                function setterFunction(val)
                {
                    //return element.attr(attribute, val);
                },
                null, this.placeholderText, false, null).appendTo(form);

            var okBtn = $('<a class="btn btn-primary ' + this.confirmBtnClass + '"><i class="fa fa-check"></i></a>').appendTo(form);
            var cancelBtn = $('<a class="btn btn-link" class="' + this.cancelBtnClass + '">cancel</a>').appendTo(form);

            okBtn.click(this.handleSaveClick.bind(this));
            cancelBtn.click(this.handleCloseClick.bind(this));

            return form.get(0);
        },
        getInput: function ()
        {
            return this.getForm().querySelector('input');
        },

        //-----OWN FUNCTIONS-----

        //-----PRIVATE FUNCTIONS-----
    });

}]);

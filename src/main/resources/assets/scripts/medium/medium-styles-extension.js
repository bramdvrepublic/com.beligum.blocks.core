/**
 * Created by wouter on 8/07/15.
 */

base.plugin("blocks.core.MediumEditorExtensions", ["base.core.Class", function (Class)
{
    var MediumEditorExtensions = this;

    this.ID_PREFIX = "medium-editor-";

    //-----CLASS DEFINITION-----
    //used this as a reference: https://github.com/arcs-/MediumButton
    this.StylesPickerButton = Class.create({

        STATIC: {
            NAME: "styles-picker"
        },

        constructor: function (options)
        {
            this.options = this._extend(options, {

            });
            this.hasForm = false;
            this.isFormVisible = false;
            this.createButton();
        },

        createButton: function ()
        {
            this._createButtonElement();
            this._bindButtonClick();
        },

        isDisplayed: function ()
        {
            return this.isFormVisible;
        },

        getButton: function ()
        {
            return this.button.get(0);
        },

        getForm: function ()
        {
        },

        onHide: function ()
        {
        },

        hideForm: function ()
        {
        },

        show: function ()
        {
            //this.isFormVisible = true;
            //this.builder.show(this.button.offsetLeft);
            //this.button.classList.add('medium-editor-button-active');
            //var elements = document.getElementsByClassName('medium-editor-table-builder-grid');
            //for (var i = 0; i < elements.length; i++) {
            //    // TODO: what is 16 and what is 2?
            //    elements[i].style.height = (16 * this.options.rows + 2) + 'px';
            //    elements[i].style.width = (16 * this.options.columns + 2) + 'px';
            //}
        },

        checkState: function ()
        {
            //var html = getCurrentSelection();
            //if (this.options.start != '' && html.indexOf(this.options.start) > -1 && html.indexOf(this.options.end) > -1) {
            //    this.button.classList.add('medium-editor-button-active');
            //}
        },

        _createButtonElement: function ()
        {
            //this.button = document.createElement('button');
            //this.button.className = 'medium-editor-action';
            //this.button.innerHTML = '<i class="fa fa-table"></i>';

            //this.button = $('<button/>');
            this.button = $('<div class="dropdown btn-group medium-editor-action"/>');
            var toggle = $('<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Style <span class="caret"></span></button>');
            var styles = $('<ul class="dropdown-menu"/>');
            styles.append('<li><a href="#">Action</a></li>');
            styles.append('<li><a href="#">Another action</a></li>');

            this.button.append(toggle).append(styles);

            //this.button.addClass('medium-editor-action');
            //this.button.append('<i class="fa fa-table"></i>');
        },

        _bindButtonClick: function ()
        {
            //this.button.addEventListener('click', function (e)
            //{
            //    e.preventDefault();
            //    this[this.isFormVisible === true ? 'hideForm' : 'show']();
            //}.bind(this));
        },

        //-----UTILS-----
        //extends the dest object with all the properties in source and returns dest
        _extend: function (dest, source)
        {
            var prop;
            dest = dest || {};
            for (prop in source) {
                if (source.hasOwnProperty(prop) && !dest.hasOwnProperty(prop)) {
                    dest[prop] = source[prop];
                }
            }
            return dest;
        }
    });

















    var editorStyles = [];

    // Styles is an array with objects
    // object is of type {value: "", text: ""}
    // value = "p:red" -> text before the colon is the tag, text after the colon are the classes that will be added
    // nothing after colon will remove all classes, nothing before colon will not touch the tag
    // text is the text in the dropdown
    this.setStyles = function (newStyles)
    {
        editorStyles = newStyles;
    };

    this.getStyles = function ()
    {
        return editorStyles;
    };

    this.styleExtension = MediumEditor.FormExtension.extend({

        name: 'styles-picker',
        //action: 'styles',
        aria: 'add styles to paragraphs',
        contentDefault: '&#xB1;', // ±
        contentFA: '<i class="fa fa-magic"></i>',

        init: function ()
        {
            MediumEditor.FormExtension.prototype.init.apply(this, arguments);
        },

        // Called when the button the toolbar is clicked
        // Overrides ButtonExtension.handleClick
        handleClick: function (event)
        {
            event.preventDefault();
            event.stopPropagation();

            if (!this.isDisplayed()) {
                // Get fontsize of current selection (convert to string since IE returns this as number)
                var fontSize = this.document.queryCommandValue('fontSize') + '';
                this.showForm(fontSize);
            }

            return false;
        },

        // Called by medium-editor to append form to the toolbar
        getForm: function ()
        {
            if (!this.form) {
                this.form = this.createForm();
            }
            return this.form;
        },

        // Used by medium-editor when the default toolbar is to be displayed
        isDisplayed: function ()
        {
            return this.getForm().style.display === 'block';
        },

        hideForm: function ()
        {
            this.getForm().style.display = 'none';
        },

        showForm: function (fontSize)
        {
            var input = this.getInput();

            this.base.saveSelection();
            this.hideToolbarDefaultActions();
            this.getForm().style.display = 'block';
            this.setToolbarPosition();

            var blockContainerElementNames = ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'pre'];
            var elements = this.base.getSelection().getSelectedElements(this.document);
            if (elements.length == 0) {
                elements.push(this.base.getSelectedParentElement());
            }
            // Filter all elements that we will change with our style
            var parents = [];
            var lastElement = null;
            for (var i = 0; i < elements.length; i++) {
                var el = $(elements[i]);
                // From all selected elements, we need only the root block elements
                // so filter the children out
                if (lastElement == null || lastElement.has(el).length == 0) {
                    //TODO: something's wrong here
                    while (!el.attr("content-editable") && blockContainerElementNames.indexOf(el[0].nodeName.toLowerCase()) == -1) {
                        el = el.parent()
                    }
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
            this.blocks = parents;

        },

        // Called by core when tearing down medium-editor (destroy)
        destroy: function ()
        {
            if (this.isDisplayed()) {
                this.doFormCancel();
            }
            this.blocks = null;
            this.base = null;

            if (!this.form) {
                return false;
            }

            if (this.form.parentNode) {
                this.form.parentNode.removeChild(this.form);
            }

            delete this.form;
        },

        // core methods

        doFormSave: function ()
        {
            this.base.restoreSelection();
            this.base.checkSelection();
        },

        doFormCancel: function ()
        {
            for (var i = 0; i < this.blocks.length; i++) {
                var block = this.blocks[i];
                block.element.attr("class", block.classes);
                if (block.element[0].nodeName != block.tagName) {
                    var ne = $("<" + block.tagName + "/>");
                    ne.html(block.element.html());
                    block.element.replaceWith(ne);
                }
            }
            this.base.restoreSelection();
            this.base.checkSelection();
        },

        // form creation and event handling
        createForm: function ()
        {
            var doc = this.document,
                form = doc.createElement('div'),
                select = doc.createElement('select'),
                close = doc.createElement('a'),
                save = doc.createElement('a');

            // Font Size Form (div)
            form.className = 'medium-editor-toolbar-form';
            form.id = 'medium-editor-toolbar-form-styles-' + this.getEditorId();

            this.select = $(select);

            for (var i = 0; i < editorStyles.length; i++) {
                var val = editorStyles[i];
                this.select.append("<option value='" + val.value + "'>" + val.text + "</option>")
            }


            form.appendChild(select);

            // Add save buton
            save.setAttribute('href', '#');
            save.className = 'medium-editor-toobar-save';
            save.innerHTML = this.getEditorOption('buttonLabels') === 'fontawesome' ?
                '<i class="fa fa-check"></i>' :
                '&#10003;';
            form.appendChild(save);


            // Add close button
            close.setAttribute('href', '#');
            close.className = 'medium-editor-toobar-close';
            close.innerHTML = this.getEditorOption('buttonLabels') === 'fontawesome' ?
                '<i class="fa fa-times"></i>' :
                '&times;';
            form.appendChild(close);

            // Handle clicks on the form itself
            this.on(form, 'click', this.handleFormClick.bind(this));

            // Handle typing in the textbox
            this.on(select, 'change', this.handleStyleChange.bind(this));

            // Handle save button clicks (capture)
            this.on(save, 'click', this.handleSaveClick.bind(this), true);

            // Handle close button clicks
            this.on(close, 'click', this.handleCloseClick.bind(this));

            return form;
        },

        getInput: function ()
        {
            return this.getForm().querySelector('input.medium-editor-toolbar-input');
        },

        clearFontSize: function ()
        {

        },

        handleStyleChange: function ()
        {
            var arguments = this.select.val().split(':');
            var tag = arguments[0].trim();
            var classes = arguments[1].trim();

            for (var i = 0; i < this.blocks.length; i++) {
                var el = this.blocks[i].element;
                if (tag != "") {
                    var ne = $("<" + tag + "/>");
                    ne.html(el.html());
                    el.replaceWith(ne);
                    this.blocks[i].element = ne;
                }

                if (classes != "") {
                    this.blocks[i].element.addClass(arguments[1].trim());
                } else {
                    this.blocks[i].element.attr("class", "");
                }


            }
            this.base.restoreSelection();
        },

        handleFormClick: function (event)
        {
            // make sure not to hide form when clicking inside the form
            event.stopPropagation();
        },

        handleSaveClick: function (event)
        {
            // Clicking Save -> create the font size
            event.preventDefault();
            this.doFormSave();
        },

        handleCloseClick: function (event)
        {
            // Click Close -> close the form
            event.preventDefault();
            this.doFormCancel();
        }
    });

}]);


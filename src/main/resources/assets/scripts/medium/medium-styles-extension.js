/**
 * Created by wouter on 8/07/15.
 */

base.plugin("blocks.core.MediumEditorExtensions", [function() {

    this.styleExtension = MediumEditor.FormExtension.extend({

        name: 'styles-picker',
        //action: 'styles',
        aria: 'add styles to paragraphs',
        contentDefault: '&#xB1;', // ±
        contentFA: '<i class="fa fa-magic"></i>',

        init: function () {
            MediumEditor.FormExtension.prototype.init.apply(this, arguments);
        },

        // Called when the button the toolbar is clicked
        // Overrides ButtonExtension.handleClick
        handleClick: function (event) {
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
        getForm: function () {
            if (!this.form) {
                this.form = this.createForm();
            }
            return this.form;
        },

        // Used by medium-editor when the default toolbar is to be displayed
        isDisplayed: function () {
            return this.getForm().style.display === 'block';
        },

        hideForm: function () {
            this.getForm().style.display = 'none';
        },

        showForm: function (fontSize) {
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
            for (var i=0; i < elements.length; i++) {
                var el = $(elements[i]);
                // From all selected elements, we need only the root block elements
                // so filter the children out
                if (lastElement == null || lastElement.has(el).length == 0) {
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
        destroy: function () {
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

        doFormSave: function () {
            this.base.restoreSelection();
            this.base.checkSelection();
        },

        doFormCancel: function () {
            for (var i=0; i < this.blocks.length; i++) {
                var block = this.blocks[i];
                block.element.attr("class", block.classes);
                if (block.element[0].nodeName != block.tagName) {
                    var ne = $("<"+block.tagName + "/>");
                    ne.html(block.element.html());
                    block.element.replaceWith(ne);
                }
            }
            this.base.restoreSelection();
            this.base.checkSelection();
        },

        // form creation and event handling
        createForm: function () {
            var doc = this.document,
                form = doc.createElement('div'),
                select = doc.createElement('select'),
                close = doc.createElement('a'),
                save = doc.createElement('a');

            // Font Size Form (div)
            form.className = 'medium-editor-toolbar-form';
            form.id = 'medium-editor-toolbar-form-styles-' + this.getEditorId();

            this.select = $(select);
            this.select.append("<option value=':'>Normale tekst</option>");
            this.select.append("<option value='h1:'>Header 1</option>");
            this.select.append("<option value='h1:red'>Header 1 rood</option>");


            // Add font size slider
            //input.setAttribute('type', 'range');
            //input.setAttribute('min', '1');
            //input.setAttribute('max', '7');
            //input.className = 'medium-editor-toolbar-input';
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

        getInput: function () {
            return this.getForm().querySelector('input.medium-editor-toolbar-input');
        },

        clearFontSize: function () {

        },

        handleStyleChange: function () {
            var arguments = this.select.val().split(':');
            var tag = arguments[0].trim();
            var classes = arguments[1].trim();

            for (var i=0; i < this.blocks.length; i++) {
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

        handleFormClick: function (event) {
            // make sure not to hide form when clicking inside the form
            event.stopPropagation();
        },

        handleSaveClick: function (event) {
            // Clicking Save -> create the font size
            event.preventDefault();
            this.doFormSave();
        },

        handleCloseClick: function (event) {
            // Click Close -> close the form
            event.preventDefault();
            this.doFormCancel();
        }
    });

}]);


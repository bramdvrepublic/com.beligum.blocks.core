/**
 * Created by bram on 25/02/16.
 */
base.plugin("blocks.imports.BlocksFicheEntry", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Notification", "base.core.Commons", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar, Notification, Commons)
{
    var BlocksFicheEntryText = this;
    this.TAGS = ["blocks-fiche-entry"];

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----
        //this contains a mapping between ontology urls and the data objects returned by the server
        _termMappings: null,

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksFicheEntryText.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        getConfigs: function (block, element)
        {
            var retVal = BlocksFicheEntryText.Class.Super.prototype.getConfigs.call(this, block, element);

            retVal.push(this._createCombobox(block, element));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetFicheEntryTitle;
        },

        //-----PRIVATE METHODS-----
        /**
         * Mainly created to lazy-load the combobox; we return a combobox directly, which we will fill with the data from an endpoint.
         */
        _createCombobox: function (block, element)
        {
            var PROPERTY_ATTR = "property";
            var DATATYPE_ATTR = "datatype";
            var CONTENT_ATTR = "content";
            //makes sense to use the curie name of the terms and classes in the ontologies; it's short and future-flexible
            var TERM_NAME_FIELD = "curieName";

            //this is the label that belongs to the value
            var labelElement = element.find("[data-property='" + BlocksConstants.FICHE_ENTRY_NAME_PROPERTY + "']");
            //this is the element that holds the true value of the entry
            var propElement = element.find("." + BlocksConstants.FICHE_ENTRY_PROPERTY_CLASS);

            var _this = this;
            var combobox = this.addUniqueAttributeValueAsync(Sidebar, propElement, "Property type", PROPERTY_ATTR, "/blocks/admin/rdf/properties/", "title", TERM_NAME_FIELD,
                function changeListener(oldValueTerm, newValueTerm)
                {
                    //for now, we don't allow the combobox to switch to an "empty" value, so ignore if that happens (probably during initialization)
                    if (!newValueTerm) {
                        return;
                    }

                    // don't change anything if they're both the same
                    if (oldValueTerm && oldValueTerm[TERM_NAME_FIELD] == newValueTerm[TERM_NAME_FIELD]) {
                        return;
                    }

                    //This method gets called every time the user focuses a fiche entry, because the combobox is re-loaded
                    //every time, resulting in a change from undefined to the currently configured value.
                    //We can't really start building the html from scratch every time, because that would mean we'd lose our previously entered data.
                    //To detect a 'real change', we check three attributes on the propElement: the property, the data type and the widget class.
                    //Note that we can't just use the property attribute to check if everything is ok, because when this method is called,
                    //that attribute has just been set (since we requested it by passing PROPERTY_ATTR to addUniqueAttributeValueAsync).
                    //When all three are ok, we conclude nothing needs to be changed and it's not a 'real change', but rather a 'focus' event.
                    var skipHtmlChange = false;
                    if (
                        propElement.hasAttribute(PROPERTY_ATTR) && propElement.attr(PROPERTY_ATTR) == newValueTerm[TERM_NAME_FIELD] &&
                        propElement.hasAttribute(DATATYPE_ATTR) && propElement.attr(DATATYPE_ATTR) == newValueTerm.dataType[TERM_NAME_FIELD] &&
                        propElement.hasClass(newValueTerm.widgetType)
                    ) {
                        //we can't return straight away because we need to initialize the extra controls in the sidebar
                        skipHtmlChange = true;
                    }

                    //If we reach this point, the element needs to change. Because there's a lot of attributes coming in from other plugin modules,
                    //our approach is to strip all attributes and html off and start over. Note that we can't just replace the element because too many other
                    //closures are hooked to it,

                    if (!skipHtmlChange) {
                        //first copy the attributes to remove if we don't do this it causes problems
                        //iterating over the array we're removing elements from
                        var attributes = $.map(propElement[0].attributes, function (item)
                        {
                            return item.name;
                        });
                        // now remove the attributes
                        $.each(attributes, function (i, item)
                        {
                            propElement.removeAttr(item);
                        });

                        //here, we have a clean element, so we can start building again...
                        propElement.addClass(BlocksConstants.FICHE_ENTRY_PROPERTY_CLASS);
                        propElement.html(BlocksMessages.widgetFicheEntryDefaultValue);

                        //-- Initialize the html
                        //we don't really allow this for now (it resets the html back to the default state if we pass undefined or null as newValue)
                        if (!newValueTerm) {
                            labelElement.html(BlocksMessages.widgetFicheEntryDefaultLabel);
                        }
                        else {
                            //set the label html
                            labelElement.html("<p>" + newValueTerm.label + "</p>");

                            //initialize the property attributes
                            propElement.attr(PROPERTY_ATTR, newValueTerm[TERM_NAME_FIELD]);
                            propElement.attr(DATATYPE_ATTR, newValueTerm.dataType[TERM_NAME_FIELD]);

                            //we need to add this class to have it picked up by widget-specific modules (like the editor)
                            propElement.addClass(newValueTerm.widgetType);

                            var defaultEditorHtml = "<p>Type your text here...</p>";
                            switch (newValueTerm.widgetType) {
                                case BlocksConstants.INPUT_TYPE_EDITOR:
                                    propElement.html(defaultEditorHtml);
                                    break;
                                case BlocksConstants.INPUT_TYPE_INLINE_EDITOR:
                                    propElement.html(defaultEditorHtml);
                                    //we're not a span, so force inline
                                    propElement.attr(BlocksConstants.TEXT_EDITOR_OPTIONS_ATTR, BlocksConstants.TEXT_EDITOR_OPTIONS_FORCE_INLINE + " " + BlocksConstants.TEXT_EDITOR_OPTIONS_NO_TOOLBAR);
                                    break;
                            }
                        }
                    }

                    //-- Initialize the sidebar
                    //this will reset any previously added widgets after the combobox
                    combobox.nextAll().remove();
                    switch (newValueTerm.widgetType) {
                        case BlocksConstants.INPUT_TYPE_BOOLEAN:
                            combobox.after(_this._createBooleanWidget(block, propElement, CONTENT_ATTR));
                            break;
                        case BlocksConstants.INPUT_TYPE_NUMBER:
                            var defaultValue = 0;
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'number', 'Value', defaultValue,
                                function setterFunction(newValue)
                                {
                                    if (newValue != '') {
                                        propElement.attr(CONTENT_ATTR, newValue);
                                        propElement.html(newValue);
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                }));
                            break;
                        case BlocksConstants.INPUT_TYPE_DATE:
                            var defaultValue = '<p><i>Please enter a valid date in the sidebar</i></p>';
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'date', 'Date', defaultValue,
                                function setterFunction(newValue)
                                {
                                    if (newValue != '') {
                                        propElement.attr(CONTENT_ATTR, newValue);
                                        propElement.html(newValue);
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                }));
                            break;
                        case BlocksConstants.INPUT_TYPE_TIME:
                            var defaultValue = '<p><i>Please enter a valid time in the sidebar</i></p>';
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'time', 'Time', defaultValue,
                                function setterFunction(newValue)
                                {
                                    if (newValue != '') {
                                        propElement.attr(CONTENT_ATTR, newValue);
                                        propElement.html(newValue);
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                }));
                            break;
                        case BlocksConstants.INPUT_TYPE_DATETIME:
                            var defaultValue = '<p><i>Please enter a valid date and time in the sidebar</i></p>';
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'datetime-local', 'Date and time', defaultValue,
                                function setterFunction(newValue)
                                {
                                    if (newValue != '') {
                                        propElement.attr(CONTENT_ATTR, newValue);
                                        propElement.html(newValue);
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                }));
                            break;
                        case BlocksConstants.INPUT_TYPE_COLOR:
                            var defaultValue = '<p><i>Please enter a color in the sidebar</i></p>';
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'color', 'Color', defaultValue,
                                function setterFunction(newValue)
                                {
                                    if (newValue != '' && newValue.charAt(0) == '#') {
                                        propElement.attr(CONTENT_ATTR, newValue);
                                        propElement.html('<div class="' + BlocksConstants.INPUT_TYPE_COLOR_VALUE_CLASS + '" style="background-color: ' + newValue + '"></div>');
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                }));
                            break;
                        case BlocksConstants.INPUT_TYPE_RESOURCE:
                            var defaultValue = '<p><i>Please search for a resource in the sidebar</i></p>';
                            combobox.after(_this._createAutocompleteWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, newValueTerm.widgetConfig, 'Resource', defaultValue,
                                //Note: this function receives the entire object as it was returned from the server endpoint (class AutocompleteSuggestion)
                                function setterFunction(newValue)
                                {
                                    if (newValue && newValue.value != '') {
                                        propElement.attr(CONTENT_ATTR, newValue.value);
                                        propElement.html('<a href="'+newValue.value+'">'+newValue.title+'</a>');
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                }));
                            break;
                    }
                });

            return combobox;
        },
        _createBooleanWidget: function (block, propElement, contentAttr)
        {
            var CONTENT_VALUE_TRUE = "true";
            var CONTENT_VALUE_FALSE = "false";

            var retVal = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');

            var toggleState = function (newState)
            {
                if (newState) {
                    propElement.attr(contentAttr, CONTENT_VALUE_TRUE);
                    propElement.html('<i class="fa fa-fw ' + onClass + '" />');
                }
                else {
                    propElement.attr(contentAttr, CONTENT_VALUE_FALSE);
                    propElement.html('<i class="fa fa-fw ' + offClass + '" />');
                }
            };

            var onClass = "fa-check";
            var offClass = "fa-close";
            var toggleButton = this.createToggleButton("Value",
                function initStateCallback()
                {
                    var retVal = propElement.attr(contentAttr) == CONTENT_VALUE_TRUE;

                    toggleState(retVal);

                    return retVal;
                },
                function switchStateCallback(oldState, newState)
                {
                    toggleState(newState);
                },
                BlocksMessages.toggleLabelYes,
                BlocksMessages.toggleLabelNo
            );

            retVal.append(toggleButton);

            return retVal;
        },
        _createInputWidget: function (block, propElement, contentAttr, inputTypeConstant, htmlInputType, labelText, initialValue, setterFunction)
        {
            var id = Commons.generateId();
            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            formGroup.addClass(inputTypeConstant);
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
            }
            var inputGroup = $('<div class="input-group"></div>').appendTo(formGroup);
            var input = $('<input id="' + id + '" type="' + htmlInputType + '" class="form-control">').appendTo(inputGroup);

            //init and attach the change listener
            input.on("change keyup focus", function (event)
            {
                setterFunction(input.val());
            });

            var firstValue = propElement.attr(contentAttr);

            //if the html widget is uninitialized, try to set it to a default value
            if (firstValue == BlocksMessages.widgetFicheEntryDefaultValue) {
                //initial value may be 0 or '', so check of type
                if (typeof initialValue !== typeof undefined) {
                    firstValue = initialValue;
                }
            }

            //this gives us a chance to skip this if it would be needed
            if (typeof firstValue !== typeof undefined) {
                //init the input
                input.val(firstValue);
                //fire the change (because the one above doesn't seem to do so)
                setterFunction(firstValue);
            }

            return formGroup;
        },
        _createAutocompleteWidget: function (block, propElement, contentAttr, inputTypeConstant, inputTypeArgs, labelText, initialValue, setterFunction)
        {
            var id = Commons.generateId();
            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            formGroup.addClass(inputTypeConstant);
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
            }
            var inputGroup = $('<div class="input-group"></div>').appendTo(formGroup);
            var input = $('<input id="' + id + '" type="text" class="form-control typeahead" placeholder="Search...">').appendTo(inputGroup);

            //init the typeahead plugin
            var engine = new Bloodhound(
                {
                    queryTokenizer: Bloodhound.tokenizers.whitespace,
                    datumTokenizer: Bloodhound.tokenizers.whitespace,
                    remote: {
                        //we'll add the wildcard to the end of the endpoint url: it will end up in the value section (after the '=' sign) of the query parameter
                        url: inputTypeArgs[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_ENDPOINT] + '%QUERY',
                        wildcard: '%QUERY'
                    },
                });

            var options = {
                highlight: false,
                minLength: 1,
                hint: true,
            };
            var dataSet = {
                name: id,
                source: engine,
                limit: 5,
                //sync this with the title field of com.beligum.blocks.fs.index.entries.PageIndexEntry
                display: 'title',
                templates: {
                    //we add title (hover) tags as well because the css will probably chop it off (ellipsis overflow)
                    suggestion: Handlebars.compile('<div><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_TITLE_CLASS + '" title="{{title}}">{{title}}</p><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_SUBTITLE_CLASS + '" title="{{subTitle}}">{{subTitle}}</p></div>')
                }
            };
            input.typeahead(options, dataSet);

            input.bind('typeahead:select', function (ev, suggestion)
            {
                setterFunction(suggestion);
            });

            ////init and attach the change listener
            //input.on("change keyup focus", function (event)
            //{
            //    setterFunction(input.val());
            //});
            //
            //var firstValue = propElement.attr(contentAttr);
            //
            ////if the html widget is uninitialized, try to set it to a default value
            //if (firstValue == BlocksMessages.widgetFicheEntryDefaultValue) {
            //    //initial value may be 0 or '', so check of type
            //    if (typeof initialValue !== typeof undefined) {
            //        firstValue = initialValue;
            //    }
            //}
            //
            ////this gives us a chance to skip this if it would be needed
            //if (typeof firstValue !== typeof undefined) {
            //    //init the input
            //    input.val(firstValue);
            //    //fire the change (because the one above doesn't seem to do so)
            //    setterFunction(firstValue);
            //}

            return formGroup;
        },

    })).register(this.TAGS);

}]);
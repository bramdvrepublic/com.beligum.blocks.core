/**
 * Created by bram on 25/02/16.
 */
base.plugin("blocks.imports.BlocksFicheEntry", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Notification", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar, Notification)
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
        _createCombobox: function (block, element)
        {
            var ATTRIBUTE_NAME = "property";
            var combobox = this.addUniqueAttributeValue(Sidebar, block.element, "Property type", ATTRIBUTE_NAME,
                [{
                    name: "Loading…",
                    value: ""
                }]
            );

            var _this = this;
            //note: we need this (instead of $.getJSON) to disable to async
            $.getJSON("/blocks/admin/rdf/properties/")
                .done(function (data)
                {
                    _this._termMappings = {};

                    var EMPTY_SEL_NAME = "Please select…";
                    var EMPTY_SEL_VALUE = "";
                    var comboEntries = [{
                        name: EMPTY_SEL_NAME,
                        value: EMPTY_SEL_VALUE
                    }];

                    $.each(data, function (idx, entry)
                    {
                        comboEntries.push({
                            name: entry.title,
                            value: entry.name
                        });

                        //save the object in a mapping structure for later
                        _this._termMappings[entry.name] = entry;
                    });

                    //sort on name
                    comboEntries.sort(function (a, b)
                    {
                        //let the empty selection come first
                        if (a.name === EMPTY_SEL_NAME) {
                            return -1;
                        }
                        else {
                            var aName = a.name == null ? null : a.name.toLowerCase();
                            var bName = b.name == null ? null : b.name.toLowerCase();

                            return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
                        }
                    });

                    //get the value of the attribute on the element
                    //we don't set the property on the element itself, but on a special wrapper element annotated with the .property attribute
                    var propElement = element.find("." + BlocksConstants.FICHE_ENTRY_PROPERTY_CLASS);
                    var labelElement = element.find("." + BlocksConstants.FICHE_ENTRY_NAME_CLASS);
                    var attr = propElement.attr(ATTRIBUTE_NAME);
                    // For some browsers, `attr` is undefined; for others, 'attr' is false.  Check for both.
                    var hasAttr = (typeof attr !== typeof undefined && attr !== false);
                    var attrFound = false;

                    //we externalized this method to be able to load the data lazily when an async json call completed
                    _this.reinitCombobox(combobox, comboEntries,
                        function initCallback(testValue)
                        {
                            var retVal = false;

                            //signal the caller to stop as soon as we found the value with the retVal
                            //note that we allow the caller to add an empty ("") value to indicate the selection where no attribute is set (yet)
                            //Note that this is more or less the same init code as Widget.addUniqueAttributeValue()
                            if (!attrFound && (attr == testValue || (testValue === "" && !hasAttr))) {
                                attrFound = true;
                                retVal = true;
                            }

                            //return true if this element needs to be selected
                            return retVal;
                        },
                        function changeCallback(oldValue, newValue)
                        {
                            var oldValueTerm = _this._termMappings[oldValue];
                            var newValueTerm = _this._termMappings[newValue];

                            if (newValue) {
                                propElement.attr(ATTRIBUTE_NAME, newValue);
                            }
                            else {
                                propElement.removeAttr(ATTRIBUTE_NAME);
                            }

                            //we save the widgetType as a class name, so we can filter with the widget classes
                            if (oldValueTerm) {
                                propElement.removeClass(oldValueTerm.widgetType);
                            }
                            if (newValueTerm) {
                                //we really switched to a new type (alternative is we load the page with newValueTerm as a class)
                                if (!propElement.hasClass(newValueTerm.widgetType)) {

                                    propElement.addClass(newValueTerm.widgetType);
                                    labelElement.html("<p>" + newValueTerm.label + "</p>");

                                    //this will reset any previously added widgets after the combobox
                                    combobox.nextAll().remove();

                                    var defaultEditorHtml = "<p>Type your text here...</p>";
                                    //some more type-specific post-processing
                                    switch (newValueTerm.widgetType) {
                                        case BlocksConstants.SIDEBAR_WIDGET_EDITOR:
                                            propElement.html(defaultEditorHtml);
                                            break;
                                        case BlocksConstants.SIDEBAR_WIDGET_INLINE_EDITOR:
                                            propElement.html(defaultEditorHtml);
                                            propElement.attr(BlocksConstants.TEXT_EDITOR_OPTIONS_ATTR, BlocksConstants.TEXT_EDITOR_OPTIONS_FORCE_INLINE+" "+BlocksConstants.TEXT_EDITOR_OPTIONS_NO_TOOLBAR);
                                            break;
                                        case BlocksConstants.SIDEBAR_WIDGET_TOGGLE:
                                            combobox.after(_this._createBooleanWidget(block, element, propElement));
                                            break;
                                    }
                                }
                                //initialize some widgets on page load
                                else {
                                    switch (newValueTerm.widgetType) {
                                        case BlocksConstants.SIDEBAR_WIDGET_TOGGLE:
                                            combobox.after(_this._createBooleanWidget(block, element, propElement));
                                            break;
                                    }
                                }
                            }
                            else {
                                labelElement.html(BlocksMessages.widgetFicheEntryDefaultLabel);
                                propElement.html(BlocksMessages.widgetFicheEntryDefaultValue);
                            }
                        }
                    );
                })
                .fail(function (xhr, textStatus, exception)
                {
                    Notification.error(BlocksMessages.generalServerDataError + (exception ? "; " + exception : ""), xhr);
                });

            return combobox;
        },
        _createBooleanWidget: function (block, element, propElement)
        {
            var retVal = $('<div class="' + BlocksConstants.SIDEBAR_WIDGET_WRAPPER_CLASS + '"></div>');

            var toggleState = function (newState)
            {
                if (newState) {
                    propElement.html('<i class="fa fa-fw '+onClass+'" />');
                } else {
                    propElement.html('<i class="fa fa-fw '+offClass+'" />');
                }
            };

            var onClass = "fa-check-square-o";
            var offClass = "fa-square-o";
            var toggleButton = this.createToggleButton("Value",
                function initStateCallback()
                {
                    var retVal = propElement.find('.'+onClass).length > 0;

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

    })).register(this.TAGS);

}]);
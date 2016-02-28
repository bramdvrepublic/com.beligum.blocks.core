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
        /**
         * Mainly created to lazy-load the combobox; we return a combobox directly, which we will fill with the data from an endpoint.
         */
        _createCombobox: function (block, element)
        {
            var PROPERTY_ATTR = "property";
            var DATATYPE_ATTR = "datatype";
            var CONTENT_ATTR = "content";

            //this is the label that belongs to the value
            var labelElement = element.find("[data-property='" + BlocksConstants.FICHE_ENTRY_NAME_PROPERTY + "']");
            //this is the element that holds the true value of the entry
            var propElement = element.find("." + BlocksConstants.FICHE_ENTRY_PROPERTY_CLASS);

            var _this = this;
            var combobox = this.addUniqueAttributeValueAsync(Sidebar, propElement, "Property type", PROPERTY_ATTR, "/blocks/admin/rdf/properties/", "title", "name",
                function changeListener(oldValueTerm, newValueTerm)
                {
                    //for now, we don't allow the combobox to switch to an "empty" value, so ignore if that happens (probably during initialization)
                    if (!newValueTerm) {
                        return;
                    }

                    // don't change anything if they're both the same
                    if (oldValueTerm && oldValueTerm.name==newValueTerm.name) {
                        return;
                    }

                    //This method gets called every time the user focuses a fiche entry, because the combobox is re-loaded
                    //every time, resulting in a change from undefined to the currently configured value.
                    //We can't really start from scratch every time, because that would mean we'd lose our previously entered data.
                    //To detect a 'real change', we check three attributes on the propElement: the property, the data type and the widget class.
                    //Note that we can't just use the property attribute to check if everything is ok, because when this method is called,
                    //that attribute has just been set (since we requested it by passing PROPERTY_ATTR to addUniqueAttributeValueAsync).
                    //When all three are ok, we conclude nothing needs to be changed and it's not a 'real change', but rather a 'focus' event.
                    if (
                        propElement.hasAttribute(PROPERTY_ATTR) && propElement.attr(PROPERTY_ATTR)==newValueTerm.name &&
                        propElement.hasAttribute(DATATYPE_ATTR) && propElement.attr(DATATYPE_ATTR)==newValueTerm.dataType &&
                        propElement.hasClass(newValueTerm.widgetType)
                    ) {
                        return;
                    }

                    //If we reach this point, the element needs to change. Because there's a lot of attributes coming in from other plugin modules,
                    //our approach is to strip all attributes and html off and start over. Note that we can't just replace the element because too many other
                    //closures are hooked to it,

                    //first copy the attributes to remove if we don't do this it causes problems
                    //iterating over the array we're removing elements from
                    var attributes = $.map(propElement[0].attributes, function(item) {
                        return item.name;
                    });
                    // now remove the attributes
                    $.each(attributes, function(i, item) {
                        propElement.removeAttr(item);
                    });

                    //here, we have a clean element, so we can start building again...
                    propElement.addClass(BlocksConstants.FICHE_ENTRY_PROPERTY_CLASS);
                    propElement.html(BlocksMessages.widgetFicheEntryDefaultValue);

                    if (!newValueTerm) {
                        labelElement.html(BlocksMessages.widgetFicheEntryDefaultLabel);
                    }
                    else {
                        //this will reset any previously added widgets after the combobox
                        combobox.nextAll().remove();

                        //set the label html
                        labelElement.html("<p>" + newValueTerm.label + "</p>");

                        //initialize the property attributes
                        propElement.attr(PROPERTY_ATTR, newValueTerm.name);
                        propElement.attr(DATATYPE_ATTR, newValueTerm.dataType);

                        //we need to add this class to have it picked up by widget-specific modules (like the editor)
                        propElement.addClass(newValueTerm.widgetType);

                        //some more type-specific post-processing
                        var defaultEditorHtml = "<p>Type your text here...</p>";
                        switch (newValueTerm.widgetType) {
                            case BlocksConstants.SIDEBAR_WIDGET_EDITOR:
                                propElement.html(defaultEditorHtml);
                                break;
                            case BlocksConstants.SIDEBAR_WIDGET_INLINE_EDITOR:
                                propElement.html(defaultEditorHtml);
                                //we're not a span, so force inline
                                propElement.attr(BlocksConstants.TEXT_EDITOR_OPTIONS_ATTR, BlocksConstants.TEXT_EDITOR_OPTIONS_FORCE_INLINE + " " + BlocksConstants.TEXT_EDITOR_OPTIONS_NO_TOOLBAR);
                                break;
                            case BlocksConstants.SIDEBAR_WIDGET_TOGGLE:
                                combobox.after(_this._createBooleanWidget(block, propElement, CONTENT_ATTR));
                                break;
                        }
                    }
                });

            return combobox;
        },
        _createBooleanWidget: function (block, propElement, contentAttr)
        {
            var CONTENT_VALUE_TRUE = "true";
            var CONTENT_VALUE_FALSE = "false";

            var retVal = $('<div class="' + BlocksConstants.SIDEBAR_WIDGET_WRAPPER_CLASS + '"></div>');

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

    })).register(this.TAGS);

}]);
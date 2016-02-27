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
                    //we need this code to abort early if we already have the correct class set on the property element
                    // this is because this code get's called too much and interferes with the editor-initialization code
                    if (newValueTerm && propElement.hasClass(newValueTerm.widgetType)) {
                        return;
                    }

                    //we can't replace the element because too many other closures are hooked to it,
                    //so we simply reset the existing element by removing all attributes and clearing it's html content

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

                    labelElement.html(BlocksMessages.widgetFicheEntryDefaultLabel);
                    if (newValueTerm) {

                        //this will reset any previously added widgets after the combobox
                        combobox.nextAll().remove();

                        //set the label html
                        labelElement.html("<p>" + newValueTerm.label + "</p>");

                        //initialize the property attributes
                        propElement.attr(PROPERTY_ATTR, newValueTerm.name);
                        propElement.attr(DATATYPE_ATTR, newValueTerm.dataType);
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
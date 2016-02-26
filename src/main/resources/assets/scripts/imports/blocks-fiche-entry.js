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
        _createCombobox: function(block, element)
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
                    var EMPTY_SEL_NAME = "Please select…";
                    var comboEntries = [{
                        name: EMPTY_SEL_NAME,
                        value: ""
                    }];

                    _this._termMappings = {};
                    $.each(data, function (idx, entry)
                    {
                        comboEntries.push({
                            name: entry.name,
                            value: entry.predicate
                        });

                        //save the object in a mapping structure for later
                        _this._termMappings[entry.predicate] = entry;
                    });

                    //sort on name
                    comboEntries.sort(function (a, b)
                    {
                        //let the empty selection come firt
                        if (a.name===EMPTY_SEL_NAME) {
                            return -1;
                        }
                        else {
                            var aName = a.name.toLowerCase();
                            var bName = b.name.toLowerCase();

                            return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
                        }
                    });

                    //get the value of the attribute on the element
                    //we don't set the property on the element itself, but on a special wrapper element annotated with the .property attribute
                    var propElement = element.find("."+BlocksConstants.FICHE_ENTRY_PROPERTY_CLASS);
                    var attr = propElement.attr(ATTRIBUTE_NAME);
                    // For some browsers, `attr` is undefined; for others,
                    // `attr` is false.  Check for both.
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
                                propElement.attr("property", newValue);
                            }
                            else {
                                propElement.removeAttr("property");
                            }

                            //we save the widgetType as a class name, so we can filter with the widget classes
                            if (oldValueTerm) {
                                propElement.removeClass(oldValueTerm.widgetType);
                            }
                            var changedHtml = false;
                            if (newValueTerm) {
                                propElement.addClass(newValueTerm.widgetType);

                                //some more type-specific post-processing
                                if (newValueTerm.widgetType==BlocksConstants.DATATYPE_WIDGET_EDITOR || newValueTerm.widgetType==BlocksConstants.DATATYPE_WIDGET_INLINE_EDITOR) {
                                    propElement.html("<p>Type your text here...</p>");
                                    changedHtml = true;
                                }
                            }

                            //if we didn't change the inner html above, revert to a default
                            if (!changedHtml) {
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

    })).register(this.TAGS);

}]);
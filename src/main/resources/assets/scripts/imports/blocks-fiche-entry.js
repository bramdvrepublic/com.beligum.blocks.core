/**
 * Created by bram on 25/02/16.
 */
base.plugin("blocks.imports.BlocksFicheEntry", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.base.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Notification", "base.core.Commons", function (Class, Block, BlocksConstants, BaseMessages, BlocksMessages, Sidebar, Notification, Commons)
{
    var BlocksFicheEntry = this;
    this.TAGS = ["blocks-fiche-entry"];

    //Some constants
    var BOOLEAN_ATTR_TRUE = 'true';
    var BOOLEAN_ATTR_FALSE = 'false';
    var PROPERTY_ATTR = "property";
    var DATATYPE_ATTR = "datatype";
    var TYPEOF_ATTR = "typeof";
    var RESOURCE_ATTR = "resource";
    var CONTENT_ATTR = "content";
    //makes sense to use the curie name of the terms and classes in the ontologies; it's short and future-flexible
    var TERM_NAME_FIELD = "curieName";

    //Formats for human readable date & time
    var DATE_TIME_LOCALE = BaseMessages.locale;
    var DATE_TIME_FORMAT = "dddd LL, LT";
    var DATE_FORMAT = "dddd LL";
    var TIME_FORMAT = "LT";
    var TIMEZONE_FORMAT = "Z";

    //Formats for XSD value of date-time
    //Note that 'moment.ISO_8601' is more or less the same as 'YYYY-MM-DD[T]HH:mm:ss.SSS[Z]', but not 100% --> see moments.js source code (but we can live with it)
    var DATE_TIME_VALUE_FORMAT = 'YYYY-MM-DD[T]HH:mm:ss.SSS[Z]';
    //Note that this is not 100% XSD: we omit the optional timezone to simplify this widget (if you need timezones, use the datetime widget instead)
    var DATE_VALUE_FORMAT = "YYYY-MM-DD";
    var TIME_VALUE_FORMAT = 'HH:mm:ss.SSS[Z]';

    //Input formats for the date/time widgets
    //More or less the same as 'YYYY-MM-DD[T]HH:mm:ss.SSS[Z]', but not 100% --> see moments.js source code
    var DATE_TIME_WIDGET_FORMAT = 'YYYY-MM-DDTHH:mm:ss';
    //Note that this is not 100% XSD: we omit the optional timezone to simplify this widget (if you need timezones, use the datetime widget instead)
    var DATE_WIDGET_FORMAT = 'YYYY-MM-DD';
    var TIME_WIDGET_FORMAT = 'HH:mm:ss';

    //are we editing a date, a time or a dateTime?
    var DATE_TIME_ENUM_DATE = 1;
    var DATE_TIME_ENUM_TIME = 2;
    var DATE_TIME_ENUM_DATETIME = 3;

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----
        //this contains a mapping between ontology urls and the data objects returned by the server
        _termMappings: null,
        _gmtSelected: false,
        _dateTimeEnum: null,
        _dateTimeFormat: null,
        _timezoneFormat: null,
        _dateTimeValueFormat: null,
        _dateTimeWidgetFormat: null,

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksFicheEntry.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        getConfigs: function (block, element)
        {
            var retVal = BlocksFicheEntry.Class.Super.prototype.getConfigs.call(this, block, element);

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
            //this is the label that belongs to the value
            var labelElement = element.find("[data-property='" + BlocksConstants.FICHE_ENTRY_NAME_PROPERTY + "']");
            //this is the element that holds the true value of the entry
            var propElement = element.find("." + BlocksConstants.FICHE_ENTRY_PROPERTY_CLASS);

            var _this = this;
            var endpointURL = "/blocks/admin/rdf/properties/";
            var pageTypeof = $('html').attr(TYPEOF_ATTR);
            if (pageTypeof) {
                endpointURL += "?resourceTypeCurie=" + pageTypeof;
            }
            var combobox = this.addUniqueAttributeValueAsync(Sidebar, propElement, "Property type", PROPERTY_ATTR, endpointURL, "title", TERM_NAME_FIELD,
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
                        (
                            //we either need a datatype (for literals) or a typeof (for references)
                            (propElement.hasAttribute(DATATYPE_ATTR) && propElement.attr(DATATYPE_ATTR) == newValueTerm.dataType[TERM_NAME_FIELD]) ||
                            (propElement.hasAttribute(TYPEOF_ATTR) && propElement.attr(TYPEOF_ATTR) == newValueTerm.dataType[TERM_NAME_FIELD])
                        ) &&
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

                            //Initialize the property attributes
                            //Note that we also could use the 'rel' attribute instead of 'property' when working with resources (not for literals)
                            // see https://www.w3.org/TR/rdfa-syntax/#chaining-with-property-and-typeof
                            // "The main differences between @property and @rel (or @rev) is that the former does not induce chaining.
                            //  (see this URL for what chaining is: https://www.w3.org/TR/rdfa-syntax/#inheriting-subject-from-resource)
                            //  The only exception to this rule is when @typeof is also present on the element. In that case the effect of @property is identical to @rel."
                            //Since we _are_ using "typeof", it truly is identical.
                            //
                            //More info from https://www.w3.org/TR/rdfa-syntax/#object-resolution-for-the-property-attribute
                            // "An object literal will be generated when @property is present and no resource attribute is present."
                            // illustrates the difference between resource and literal handling.
                            propElement.attr(PROPERTY_ATTR, newValueTerm[TERM_NAME_FIELD]);

                            //If we're dealing with a reference to another resource, we use the typeof attribute,
                            //otherwise (when dealing with a literal), we use the datatype attribute.
                            //We compare the use of @typeof (together with @resource) as the 'reference-equivalent' of using @datatype together with a literal.
                            if (newValueTerm.widgetType == BlocksConstants.INPUT_TYPE_RESOURCE) {
                                //note that despite it's name, this value will just contain a curie name to an RDF class
                                propElement.attr(TYPEOF_ATTR, newValueTerm.dataType[TERM_NAME_FIELD]);
                            }
                            else {
                                propElement.attr(DATATYPE_ATTR, newValueTerm.dataType[TERM_NAME_FIELD]);
                            }

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

                    //little odd, but didn't find any other way: we 'localize' the functions with the variable _this in the closure
                    //because I didn't know how to get to the correct _this inside these 3 functions
                    var dateTimeSetterFunction = function (propElement, defaultValue, newValue)
                    {
                        return _this._dateTimeSetterFunction(_this, propElement, defaultValue, newValue);
                    };
                    var dateTimeWidgetSetterFilterFunction = function (contentValue)
                    {
                        return _this._dateTimeWidgetSetterFilterFunction(_this, contentValue);
                    };
                    var dateTimeExtraHtmlFunction = function (updateCallback)
                    {
                        return _this._dateTimeExtraHtmlFunction(_this, updateCallback);
                    };

                    //this is a general initialization for time and dateTime, but doesn't harm any other types
                    //this will be the variable we use to save the state of the GMT checkbox
                    //Note that all values are saved in UTC, this is just the flag to control how it's displayed to the user
                    if (propElement.hasAttribute(BlocksConstants.INPUT_TYPE_TIME_GMT_ATTR)) {
                        //see the setter function below: this should be
                        _this._gmtSelected = propElement.attr(BlocksConstants.INPUT_TYPE_TIME_GMT_ATTR) === BOOLEAN_ATTR_TRUE;
                    }
                    else {
                        //we default to using the local timezone for entering times
                        _this._gmtSelected = false;
                    }

                    switch (newValueTerm.widgetType) {
                        case BlocksConstants.INPUT_TYPE_BOOLEAN:
                            combobox.after(_this._createBooleanWidget(block, propElement, CONTENT_ATTR));
                            break;
                        case BlocksConstants.INPUT_TYPE_NUMBER:
                            var defaultValue = 0;
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'number', {}, 'Value', defaultValue,
                                function setterFunction(propElement, defaultValue, newValue)
                                {
                                    if (newValue && newValue != '') {
                                        propElement.attr(CONTENT_ATTR, newValue);
                                        propElement.html(newValue);
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                },
                                null,
                                null));
                            break;
                        case BlocksConstants.INPUT_TYPE_DATE:
                            var defaultValue = '<p><i>Please enter a valid date in the sidebar</i></p>';
                            _this._dateTimeEnum = DATE_TIME_ENUM_DATE;
                            _this._dateTimeFormat = DATE_FORMAT;
                            _this._timezoneFormat = null;
                            _this._dateTimeValueFormat = DATE_VALUE_FORMAT;
                            _this._dateTimeWidgetFormat = DATE_WIDGET_FORMAT;
                            //Note that we save all date values as GMT (if you need timezone functionality, use the dateTime widget)
                            _this._gmtSelected = true;
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'date', {}, 'Date', defaultValue,
                                dateTimeSetterFunction, dateTimeWidgetSetterFilterFunction, null));
                            break;
                        case BlocksConstants.INPUT_TYPE_TIME:
                            var defaultValue = '<p><i>Please enter a valid time in the sidebar</i></p>';
                            _this._dateTimeEnum = DATE_TIME_ENUM_TIME;
                            _this._dateTimeFormat = TIME_FORMAT;
                            _this._timezoneFormat = TIMEZONE_FORMAT;
                            _this._dateTimeValueFormat = TIME_VALUE_FORMAT;
                            _this._dateTimeWidgetFormat = TIME_WIDGET_FORMAT;
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'time', {}, 'Time', defaultValue,
                                dateTimeSetterFunction, dateTimeWidgetSetterFilterFunction, dateTimeExtraHtmlFunction));
                            break;
                        case BlocksConstants.INPUT_TYPE_DATETIME:
                            var defaultValue = '<p><i>Please enter a valid date and time in the sidebar</i></p>';
                            _this._dateTimeEnum = DATE_TIME_ENUM_DATETIME;
                            _this._dateTimeFormat = DATE_TIME_FORMAT;
                            _this._timezoneFormat = TIMEZONE_FORMAT;
                            _this._dateTimeValueFormat = DATE_TIME_VALUE_FORMAT;
                            _this._dateTimeWidgetFormat = DATE_TIME_WIDGET_FORMAT;
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'datetime-local', {}, 'Date and time', defaultValue,
                                dateTimeSetterFunction, dateTimeWidgetSetterFilterFunction, dateTimeExtraHtmlFunction));
                            break;
                        case BlocksConstants.INPUT_TYPE_COLOR:
                            var defaultValue = '<p><i>Please enter a color in the sidebar</i></p>';
                            combobox.after(_this._createInputWidget(block, propElement, CONTENT_ATTR, newValueTerm.widgetType, 'color', {}, 'Color', defaultValue,
                                function setterFunction(propElement, defaultValue, newValue)
                                {
                                    if (newValue && newValue != '' && newValue.charAt(0) == '#') {
                                        propElement.attr(CONTENT_ATTR, newValue);
                                        propElement.html('<div class="' + BlocksConstants.INPUT_TYPE_COLOR_VALUE_CLASS + '" style="background-color: ' + newValue + '"></div>');
                                    }
                                    else {
                                        propElement.removeAttr(CONTENT_ATTR);
                                        propElement.html(defaultValue);
                                    }
                                },
                                null,
                                null));
                            break;
                        case BlocksConstants.INPUT_TYPE_RESOURCE:
                            var defaultValue = '<p><i>Please search for a resource in the sidebar</i></p>';
                            combobox.after(_this._createAutocompleteWidget(block, propElement, RESOURCE_ATTR, newValueTerm.widgetType, newValueTerm.widgetConfig, 'Resource', defaultValue,
                                //Note: this function receives the entire object as it was returned from the server endpoint (class AutocompleteSuggestion)
                                function setterFunction(propElement, initialValue, newValue)
                                {
                                    if (newValue && newValue.label != '') {

                                        //the real value of the property is the remote resource id
                                        //A nice illustration of this use is here: https://www.w3.org/TR/rdfa-syntax/#inheriting-subject-from-resource
                                        //
                                        //Regarding the relation between @resource, @href and @src, the docs say the following:
                                        // "If no @resource is present, then @href or @src are next in priority order for setting the object."
                                        // (see https://www.w3.org/TR/rdfa-syntax/#using-href-or-src-to-set-the-object)
                                        // thus supplying both a @resource with a wrapped @href as below is valid.
                                        propElement.attr(RESOURCE_ATTR, newValue.resourceUri);

                                        var labelHtml = newValue.label;
                                        //if the value has an image, it takes precedence of the label and we render an image instead of text
                                        if (newValue.image) {
                                            labelHtml = '<img src="' + newValue.image + '" alt="' + newValue.label + '">';
                                        }

                                        //if the value has a link, let's render a hyperlink
                                        if (newValue.link) {
                                            var link = $('<a href="' + newValue.link + '">' + labelHtml + '</a>');
                                            //little trick to get the hostname of an url: put it in a link element (which we need anyway) and query for the raw JS hostname
                                            //also note that we can force an external link server side with the externalLink property
                                            if (newValue.externalLink || link[0].hostname != document.location.hostname) {
                                                link.attr("target", "_blank");
                                            }
                                            propElement.html(link);
                                        }
                                        else {
                                            propElement.html(labelHtml);
                                        }
                                    }
                                    else {
                                        propElement.removeAttr(RESOURCE_ATTR);
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
        _createInputWidget: function (block, propElement, contentAttr, inputTypeConstant, htmlInputType, htmlInputAttrs, labelText, initialValue, setterFunction, widgetSetterFilterFunction, extraHtmlFunction)
        {
            var id = Commons.generateId();
            var retVal = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            retVal.addClass(inputTypeConstant);
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(retVal);
            }
            var inputGroup = $('<div class="input-group"></div>').appendTo(retVal);
            var input = $('<input id="' + id + '" type="' + htmlInputType + '" class="form-control">').appendTo(inputGroup);

            if (htmlInputAttrs) {
                $.each(htmlInputAttrs, function (key, value)
                {
                    input.attr(key, value);
                });
            }

            //init and attach the change listener
            var updateCallback = function ()
            {
                setterFunction(propElement, initialValue, input.val());
            };
            input.on("change keyup focus", function (event)
            {
                updateCallback();
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
                //init the input and filter it if needed;
                // this filter sits between the value in the @content attribute and the setter function for the input widget
                // so we can do some preprocessing before passing it to the widget
                if (widgetSetterFilterFunction) {
                    firstValue = widgetSetterFilterFunction(firstValue);
                }
                input.val(firstValue);
                //fire the change (because the one above doesn't seem to do so)
                setterFunction(propElement, initialValue, firstValue);
            }

            //note: this should come after the processing of widgetSetterFilterFunction() because
            //that one may initialize some stuff to initialize the extra HTML (like GMT flag for dateTime)
            if (extraHtmlFunction) {
                var extraHtml = extraHtmlFunction(updateCallback);
                if (extraHtml) {
                    var container = $("<div></div>");
                    container.append(retVal);
                    container.append(extraHtml);
                    retVal = container;
                }
            }

            return retVal;
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
                        //note: the prepare function below will add the correct query at the end
                        url: inputTypeArgs[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT],
                        prepare: function (query, settings)
                        {
                            settings.url = settings.url + encodeURIComponent(query);

                            return settings;
                        },
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
                //workaround for bug https://github.com/twitter/typeahead.js/issues/1201#issuecomment-185854471
                limit: parseInt(inputTypeArgs[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS]) - 1,
                //sync this with the title field of com.beligum.blocks.fs.index.entries.PageIndexEntry
                display: 'title',
                templates: {
                    empty: '<div class="tt-suggestion "' + BlocksConstants.INPUT_TYPE_RES_SUG_EMPTY_CLASS + '><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_TITLE_CLASS + '">' + 'No match for this query' + '</p></div>',
                    //we add title (hover) tags as well because the css will probably chop it off (ellipsis overflow)
                    suggestion: Handlebars.compile('<div><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_TITLE_CLASS + '" title="{{title}}">{{title}}</p><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_SUBTITLE_CLASS + '" title="{{subTitle}}">{{subTitle}}</p></div>')
                }
            };
            input.typeahead(options, dataSet);

            //gets called when a real selection is done
            input.bind('typeahead:select', function (ev, suggestion)
            {
                $.getJSON(inputTypeArgs[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT] + encodeURIComponent(suggestion.resourceId))
                    .done(function (data)
                    {
                        setterFunction(propElement, initialValue, data);
                    })
                    .fail(function (xhr, textStatus, exception)
                    {
                        Notification.error(BlocksMessages.generalServerDataError + (exception ? "; " + exception : ""), xhr);
                    });
            });

            ////init and attach the change listener

            var firstValue = propElement.attr(contentAttr);

            //if the html widget is uninitialized, try to set it to a default value
            if (typeof firstValue === typeof undefined) {
                //initial value may be 0 or '', so check of type
                if (typeof initialValue !== typeof undefined) {
                    //signal the setter function to reset the tag
                    setterFunction(propElement, initialValue, null);
                }
            }
            else {
                //this gives us a chance to skip this if it would be needed
                if (typeof firstValue !== typeof undefined) {
                    //if we have a real value, contact the resource endpoint to load the official name (not the more human friendly label) into the autocomplete box
                    if (firstValue != initialValue) {
                        $.getJSON(inputTypeArgs[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT] + encodeURIComponent(firstValue))
                            .done(function (data)
                            {
                                //init the input
                                input.typeahead('val', data.name);

                                //don't think we need to re-set the html here, just init the autocomplete box
                                //setterFunction(propElement, initialValue, data);
                            })
                            .fail(function (xhr, textStatus, exception)
                            {
                                Notification.error(BlocksMessages.generalServerDataError + (exception ? "; " + exception : ""), xhr);
                            });
                    }
                }
            }

            return formGroup;
        },
        _dateTimeSetterFunction: function (_this, propElement, defaultValue, newValue)
        {
            //here we have two possibilities:
            //1) the value comes in from the datetime-local (or date or time) widget, meaning it has no timezone specified
            //   (the name of the widget indicates it to be '-local', but instead it doesn't seem to have any timezone specified in the string-value at all; eg. 2016-01-01T01:00)
            //   Since we added a checkbox to allow the user to enter a GMT time (and to indicate the admin user should think about it in the first place),
            //   we should interpret the incoming value and convert it to a UTC/GMT time or a local timezone'd time
            //2) the value comes in from the @content attributes as saved in the html
            //   Since we save the value as xsd:dateTime, it should have a timezone specified
            //
            //For more details about the formats use by xsd:time, xsd:date and xsd:dateTime, see these:
            // https://www.w3.org/TR/xmlschema-2/#time
            // https://www.w3.org/TR/xmlschema-2/#date
            // https://www.w3.org/TR/xmlschema-2/#dateTime
            if (newValue && newValue != '') {

                //first of all, we need to decide if we're dealing with a time, a date or a dateTime
                //what comes in is the value as returned by the widget, so we try to detect as general as possible
                //See for details: https://github.com/moment/moment/issues/2397
                var momentFormat = null;
                switch (_this._dateTimeEnum) {
                    case DATE_TIME_ENUM_DATE:
                        momentFormat = 'YYYY-MM-DD';
                        break;
                    case DATE_TIME_ENUM_TIME:
                        momentFormat = 'HH:mm:ss';
                        break;
                    case DATE_TIME_ENUM_DATETIME:
                        momentFormat = moment.ISO_8601;
                        break;
                }

                //By default, moment parses and displays in local time. If you want to parse or display a moment in UTC,
                // you can use moment.utc() instead of moment()
                //Note that as specified in https://www.w3.org/TR/html-markup/references.html#refsRFC3339
                //the date/time coming back from the input widget should be ISO 8601 formatted
                //which is exactly the same standard as the one used by xsd:dateTime; https://www.w3.org/TR/xmlschema11-2/#dateTime
                var val = null;
                if (_this._gmtSelected) {
                    val = moment.utc(newValue, momentFormat);
                }
                else {
                    val = moment(newValue, momentFormat);
                }

                //sets the locale that is currently active (based on the URL)
                val.locale(DATE_TIME_LOCALE);

                //Set the user-friendly HTML
                var timezoneHtml = '';
                if (_this._timezoneFormat!=null) {
                    var timezone = val.format(_this._timezoneFormat);
                    var timezoneInnerHtml = null;
                    if (val.utcOffset() == 0) {
                        timezoneInnerHtml = '(UTC)';
                    }
                    else {
                        timezoneInnerHtml = '(UTC' + timezone + ')';
                    }
                    timezoneHtml = '<span class="' + BlocksConstants.INPUT_TYPE_TIME_TZONE_CLASS + '">' + timezoneInnerHtml + '</span>';
                }
                propElement.html(val.format(_this._dateTimeFormat) + timezoneHtml);

                //Set the @content value
                //Note that we save everything as UTC (important)
                propElement.attr(CONTENT_ATTR, val.utc().format(_this._dateTimeValueFormat));
                //we save the GMT flag to the input element because it's part of the settings of this widget
                // (and it's annonying to have to set it every time over when editing)
                //Note that we save it to a temp variable to only set it if we're setting a complete and good date or time
                propElement.attr(BlocksConstants.INPUT_TYPE_TIME_GMT_ATTR, _this._gmtSelected ? BOOLEAN_ATTR_TRUE : BOOLEAN_ATTR_FALSE);
            }
            else {
                propElement.removeAttr(CONTENT_ATTR);
                propElement.html(defaultValue);
            }
        },
        /**
         * This filter pre-processes the value in the @content attribute before passing it to the datetime html widget
         * We need to chop off the timezone and set the GMT checkbox accordingly because the HTML widget doesn't support timezones
         */
        _dateTimeWidgetSetterFilterFunction: function (_this, contentValue)
        {
            var retVal = contentValue;

            if (contentValue) {
                //Note: we save all @content values as UTC, so the GMT flag should always be activated
                //However, by default, we choose not to activate it because it's easier for the user to
                //edit dateTimes in local time. Can be changed with the flag below though.
                var val = moment.utc(contentValue, _this._dateTimeValueFormat);
                if (val.utcOffset() !== 0 || !_this._gmtSelected) {
                    //if the saved value is not UTC+0:00, we need to convert it
                    // to the local time zone to make correct adjustments, because
                    // by default (eg. when no GMT flag is on) moment parses and displays in local time.
                    if (_this._gmtSelected) {
                        //convert the val to GMT
                        val = val.utc();
                    }
                    else {
                        //convert the val to the local time zone
                        val = val.local();
                    }
                }
                else {
                    //NOOP
                }

                //the datetime widget expects the format to be without timezone (because it doesn't support it)
                retVal = val.format(_this._dateTimeWidgetFormat);
            }

            return retVal;
        },
        _dateTimeExtraHtmlFunction: function (_this, updateCallback)
        {
            var toggleButton = _this.createToggleButton("GMT/UTC?",
                function initStateCallback()
                {
                    //call it once during initializing
                    if (updateCallback) {
                        updateCallback();
                    }

                    return _this._gmtSelected;
                },
                function switchStateCallback(oldState, newState)
                {
                    _this._gmtSelected = newState;
                    if (updateCallback) {
                        updateCallback();
                    }
                },
                BlocksMessages.toggleLabelYes,
                BlocksMessages.toggleLabelNo
            );

            return toggleButton;
        }

    })).register(this.TAGS);

}]);
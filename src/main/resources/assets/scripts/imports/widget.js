/**
 * Created by bram on 8/13/15.
 */
/*
 * This is the abstract superclass that all widgets need to extend
 */
base.plugin("blocks.imports.Widget", ["constants.blocks.core", "messages.blocks.core", "base.core.Class", "base.core.Commons", "blocks.core.Notification", function (BlocksConstants, BlocksMessages, Class, Commons, Notification)
{
    var Widget = this;

    //-----CONSTANTS-----
    this.OLD_VAL_ATTR = 'data-reset';
    this.TEXT_INPUT_ACTION_OPTION_DISABLE = 'disable';
    this.TEXT_INPUT_ACTION_OPTION_ONSELECT = 'onSelect';

    this.Class = Class.create({

        //-----STATICS-----
        STATIC: {
            SELECTOR_INDEX: {},
            OBJ_REFS: {},

            /**
             * Register a new widget class for the supplied tags
             * @param selectors the array of jquery selectors you want to register this widget class to
             */
            register: function (selectors)
            {
                //there should always be a tags option specified
                if (selectors && selectors.length && selectors.length > 0) {
                    for (var i = 0; i < selectors.length; i++) {
                        var selector = selectors[i];
                        // note that this will happen when we extend eg. blocks-spacer in a subclass;
                        // the superclass will be registered first, and later overwritten by it's subclass
                        if (Widget.Class.SELECTOR_INDEX[selector]) {
                            //Logger.warn("Encountered a double Widget registration for '"+selector+"', overwriting.", this);
                        }

                        Widget.Class.SELECTOR_INDEX[selector] = this;
                    }
                }
                else {
                    throw Logger.error("Could not instantiate widget because the 'tags' option (an array containing the tags you want this widget to be registered to) was missing or wrong has the type.", selectors);
                }
            },

            /**
             * Factory method: create a new widget instance for the supplied element tag
             * @param element the html element you want to construct a widget for
             * @returns the instance or null if no such tag is registered
             */
            create: function (element)
            {
                var retVal = null;

                if (element != null) {
                    var clazz = null;

                    //search for the first selector that matches
                    $.each(Widget.Class.SELECTOR_INDEX, function (selector, widget)
                    {
                        if (element.is(selector)) {
                            clazz = widget;
                            return false; // == break
                        }
                    });

                    //if we found a class, instantiate it
                    if (clazz) {
                        retVal = new clazz();
                    }
                }

                return retVal;
            }
        },

        //-----PUBLIC VARIABLES-----
        id: null,
        creationStamp: null,

        //-----PRIVATE VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            this.id = Commons.generateId();
            this.creationStamp = new Date().getTime();

            //note: this.constructor returns the class
            if (!Widget.Class.OBJ_REFS[this.constructor]) {
                Widget.Class.OBJ_REFS[this.constructor] = {};
                this.init();
            }
            //add a reference to this object
            Widget.Class.OBJ_REFS[this.constructor][this.id] = this;
        },

        //-----'ABSTRACT' METHODS-----
        /**
         * This class gets called only once for each subclass: when the first object of this subclass is instantiated
         */
        init: function ()
        {
        },

        /**
         * @param block the block that should get focus (not null)
         * @param element one of these:
         *                - the first property element on the way up of the element that got clicked (inside the block)
         *                - the template element (then element==block.element) that got clicked
         *                - the page element
         * @param hotspot the (possibly changed) mouse coordinates that function as the 'hotspot' for this event (object with top and left like offset())
         * @param event the original event that triggered this all
         */
        focus: function (block, element, hotspot, event)
        {
        },

        /**
         * @param same parameters as in focus()
         */
        blur: function (block, element)
        {
        },

        /**
         * @param first two parameters as in focus()
         * @return an array containing config UI, created with the factory methods below (eg. addValueAttribute())
         */
        getConfigs: function (block, element)
        {
            return [];
        },

        /**
         * @return the name of the sidebar window for this widget if it's a block
         */
        getWindowName: function ()
        {
            return null;
        },

        //-----PUBLIC METHODS-----
        //Note: all the methods below can be used by subclasses to add widgets to the sidebar
        /**
         * Links the value of the class="" attribute to a dropdown list
         *
         * element: element to change
         * labelText: name to show as label
         * values = array of objects {value: 'a value to change', name: 'name of the value}
         * */
        addUniqueClass: function (Sidebar, element, labelText, values, changeListener)
        {
            var classFound = false;

            //small dryrun so we know if the (possible) empty value needs to be selected
            var hasNonEmptyClass = false;
            for (var i = 0; i < values.length; i++) {
                var cl = values[i].value;
                if (cl != null && cl != "" && element.hasClass(cl)) {
                    hasNonEmptyClass = true;
                }
            }

            var retVal = this.createCombobox(Sidebar, labelText, values,
                function initCallback(testValue)
                {
                    var retVal = false;

                    //second uses lazy testing: element doens't have the class, but the value is the empty string, so it should match
                    if (element.hasClass(testValue) || ((testValue == null || testValue == "") && !hasNonEmptyClass)) {
                        if (!classFound) {
                            classFound = true;
                            retVal = true;
                        }
                        else {
                            // If more then 1 value is selected, only keep the first value
                            element.removeClass(testValue);
                        }
                    }

                    return retVal;
                },
                function changeCallback(oldValue, newValue)
                {
                    //this will reset the classes even if newValue is ""
                    for (var i = 0; i < values.length; i++) {
                        element.removeClass(values[i].value);
                    }

                    element.addClass(newValue);

                    //propagate up if we have a someone listening
                    if (changeListener) {
                        changeListener(oldValue, newValue);
                    }
                });

            return retVal;
        },

        /**
         * Links the value of the class="" attribute to a toggle button that can be switched on or off.
         *
         * element: element to change
         * labelText: name to show as label
         * value: the class you want to enable/disable
         * changeListener: optional change listener
         * attribute: set this if you want to change an attribute instead of the class
         * */
        addOptionalClass: function (Sidebar, element, labelText, value, changeListener, attribute)
        {
            var retVal = this.createToggleButton(labelText,
                function initStateCallback()
                {
                    if (attribute) {
                        return element.hasAttribute(attribute);
                    }
                    else {
                        return element.hasClass(value);
                    }
                },

                function switchStateCallback(oldValue, newValue)
                {
                    if (attribute) {
                        if (newValue) {
                            element.attr(attribute, '')
                        } else {
                            element.removeAttr(attribute)
                        }
                    }
                    else {
                        if (newValue) {
                            element.addClass(value);
                        } else {
                            element.removeClass(value);
                        }
                    }

                    //propagate up if we have a someone listening
                    if (changeListener) {
                        changeListener(oldValue, newValue);
                    }
                },
                BlocksMessages.toggleLabelYes,
                BlocksMessages.toggleLabelNo
            );

            return retVal;
        },

        /**
         * Links the value of the class="" attribute to a slider with a configurable (label/value) number of stops.
         *
         * element: element to change
         * labelText: name to show as label
         * values = array of objects {value: 'a value to change', name: 'name of the value}
         * showTooltip: show the tooltips of the slider?
         * changeListener: optional change listener
         * attribute: set this if you want to change an attribute instead of the class
         * */
        addSliderClass: function (Sidebar, element, labelText, values, showTooltip, changeListener, attribute)
        {
            var id = Commons.generateId();

            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
            }

            //Note that this is an index in the values array
            var initValue = undefined;
            for (var i = 0; i < values.length; i++) {
                var c = values[i];

                //we take the first value as the temp init value, but keep on looking for a better init value
                if (initValue === undefined) {
                    initValue = i;
                }

                if (attribute) {
                    if (element.attr(attribute) == c.value) {
                        initValue = i;
                        //if we have a match, we use the first match
                        break;
                    }
                }
                else {
                    if (element.hasClass(c.value)) {
                        initValue = i;
                        //if we have a match, we use the first match
                        break;
                    }
                }
            }

            var inputGroup = $('<div class="input-group"></div>').appendTo(formGroup);
            var input = $('<input id="' + id + '" type="range" class="form-control" min="0" max="' + (values.length - 1) + '" step="1" value="' + initValue + '">').appendTo(inputGroup);

            //init the bootstrap-slider (see https://github.com/seiyria/bootstrap-slider)
            var sliderWidget = input.slider({
                id: id,
                min: 0,
                max: (values.length - 1),
                value: initValue,
                step: 1,
                tooltip: showTooltip ? 'show' : 'hide',
                formatter: function (value)
                {
                    return values[value].name;
                }
            });

            input.on("change", function (event)
            {
                // Call a method on the slider
                var currentIdx = sliderWidget.slider('getValue');

                //sometimes, we call the val() method on the input directly (externally), followed by a call to .trigger('change') on the range input
                //this check allows us to do just that and expect the same result because the slides seems to set a .value object and the manual trigger() does not
                if (!event.value) {
                    var oldVal = currentIdx;
                    currentIdx = parseInt($(this).val());
                    sliderWidget.slider('setValue', currentIdx);
                    event.value = {
                        oldValue: oldVal,
                        newValue: currentIdx
                    };
                }

                if (attribute) {
                    element.attr(attribute, values[currentIdx].value);
                }
                else {
                    for (var i = 0; i < values.length; i++) {
                        //this is the class linked to index i
                        var val = values[i].value;

                        //we remove all values, except for the new one
                        if (i == currentIdx) {
                            element.addClass(val);
                        }
                        else {
                            element.removeClass(val);
                        }
                    }
                }

                //propagate up if we have a someone listening
                if (changeListener) {
                    var oldValue = values[event.value.oldValue] || undefined;
                    var newValue = values[event.value.newValue] || undefined;
                    changeListener(oldValue ? oldValue.value : undefined, newValue ? newValue.value : undefined);
                }
            });

            //force a manual change if we're initing this slider,
            //since we always have a value set with a slider
            if (changeListener && initValue !== undefined) {
                changeListener(undefined, values[initValue].value);
            }

            return formGroup;
        },

        /**
         * Links the value of a configurable attribute to a dropdown list
         *
         * element: element to change
         * labelText: name to show as label
         * values = array of objects {value: 'a value to change', name: 'name of the value}
         * */
        addUniqueAttributeValue: function (Sidebar, element, labelText, attribute, values, changeListener)
        {
            var hasAttr = element.hasAttribute(attribute);
            //get the value of the attribute on the element
            var attr = hasAttr ? element.attr(attribute) : null;

            var attrFound = false;
            var retVal = this.createCombobox(Sidebar, labelText, values,
                function initCallback(testValue)
                {
                    var retVal = false;

                    //make sure we don't set the attribute to "undefined"
                    if (typeof testValue !== typeof undefined) {
                        //signal the caller to stop as soon as we found the value with the retVal
                        //note that we allow the caller to add an empty ("") value to indicate the selection where no attribute is set (yet)
                        if (!attrFound && (attr == testValue || (testValue === "" && !hasAttr))) {
                            attrFound = true;
                            retVal = true;
                        }
                    }

                    return retVal;
                },
                function changeCallback(oldValue, newValue)
                {
                    if (newValue) {
                        element.attr(attribute, newValue);
                    }
                    else {
                        if (hasAttr) {
                            element.removeAttr(attribute);
                        }
                    }

                    //propagate up if we have a someone listening
                    if (changeListener) {
                        changeListener(oldValue, newValue);
                    }
                });

            return retVal;
        },

        addUniqueAttributeValueAsync: function (Sidebar, element, labelText, attribute, valuesEndpoint, nameProperty, valueProperty, changeListener)
        {
            var retVal = this.addUniqueAttributeValue(Sidebar, element, labelText, attribute,
                [{
                    name: BlocksMessages.comboboxLoadingName,
                    value: undefined //don't make this null cause it needs to be different from the default attr value in the test routines above
                }]
            );

            //this extracts the unique id from the dropdown (to have a unique ID later on, see _termMappings)
            //our dropdown consists of a button with an id, so that's what we're looking for (a button with a non-empty id attr)
            var comboId = retVal.find('button:not([id=""])').attr('id');

            var _this = this;
            $.getJSON(valuesEndpoint)
                .done(function (data)
                {
                    //initialize a private variable that will hold mappings between the data objects from the server
                    // and the keys in the combobox
                    //Note that we need to make this unique by using the combobox ID because we can have multiple comboboxes in one widget
                    if (!_this._termMappings) {
                        _this._termMappings = {};
                    }
                    if (!_this._termMappings[comboId]) {
                        _this._termMappings[comboId] = {};
                    }

                    var comboEntries = [];
                    $.each(data, function (idx, entry)
                    {
                        //note: null values aren't handled very well, force-switch to empty string
                        var value = entry[valueProperty] === null ? '' : entry[valueProperty];

                        comboEntries.push({
                            name: entry[nameProperty],
                            value: value
                        });

                        //save the object in a mapping structure for later
                        _this._termMappings[comboId][value] = entry;
                    });

                    //sort the combobox entries on name
                    comboEntries.sort(function (a, b)
                    {
                        //this makes sure the special 'empty valued' entries appear on top
                        if (!a.value && b.value) {
                            return -1;
                        }
                        else if (!b.value && a.value) {
                            return 1;
                        }
                        //if both are non-empty or empty, just use the label
                        else {
                            var aName = a.name == null ? null : a.name.toLowerCase();
                            var bName = b.name == null ? null : b.name.toLowerCase();

                            return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
                        }
                    });

                    var hasAttr = element.hasAttribute(attribute);
                    //get the value of the attribute on the element
                    var attr = hasAttr ? element.attr(attribute) : null;
                    var attrFound = false;

                    //we externalized this method to be able to load the data lazily when an async json call completed
                    _this.reinitCombobox(retVal, comboEntries,
                        function initCallback(testValue)
                        {
                            var retVal = false;

                            //make sure we don't set the attribute to "undefined"
                            if (typeof testValue !== typeof undefined) {
                                //signal the caller to stop as soon as we found the value with the retVal
                                //note that we allow the caller to add an empty ("") value to indicate the selection where no attribute is set (yet)
                                //Note that this is more or less the same init code as Widget.addUniqueAttributeValue()
                                if (!attrFound && (attr == testValue || (testValue === "" && !hasAttr))) {
                                    attrFound = true;
                                    retVal = true;
                                }
                            }

                            //return true if this element needs to be selected
                            return retVal;
                        },
                        function changeCallback(oldValue, newValue)
                        {
                            var oldValueTerm = _this._termMappings[comboId][oldValue];
                            var newValueTerm = _this._termMappings[comboId][newValue];

                            element.removeAttr(attribute);

                            //if we have a new value and an attribute to set, set it
                            if (attribute && newValue) {
                                element.attr(attribute, newValue);
                            }

                            if (changeListener) {
                                changeListener(oldValueTerm, newValueTerm);
                            }
                        }
                    );
                })
                .fail(function (xhr, textStatus, exception)
                {
                    Notification.error(BlocksMessages.generalServerDataError + (exception ? "; " + exception : ""), xhr);
                });

            return retVal;
        },

        /**
         * Links the presence of a configurable attribute to a dropdown list (only the 'key' of the attribute, the value is left blank)
         *
         * element: element to change
         * labelText: name to show as label
         * values = array of objects {value: 'a value to change', name: 'name of the value}
         * */
        addUniqueAttribute: function (Sidebar, element, labelText, values, changeListener)
        {
            var attrFound = false;

            var retVal = this.createCombobox(Sidebar, labelText, values,
                function initCallback(testValue)
                {
                    var retVal = false;

                    if (element.hasAttribute(testValue)) {
                        if (!attrFound) {
                            attrFound = true;
                            retVal = true;
                        }
                        else {
                            element.removeAttr(testValue)
                        }
                    }

                    return retVal;
                },
                function changeCallback(oldValue, newValue)
                {
                    element.removeAttr(oldValue);
                    if (newValue != "") {
                        element.attr(newValue, "");
                    }

                    //propagate up if we have a someone listening
                    if (changeListener) {
                        changeListener(oldValue, newValue);
                    }
                });

            return retVal;
        },

        /**
         * Links the value of a configurable attribute to an input box (with optional selection methods)
         *
         * element: element to change
         * labelText: name to show as label
         * attribute: name of the attribute the value changes
         * confirm: value only changes when user confirms
         * fileSelect: user can only select file from server
         * pageSelect: user can select a page url from the sitemap
         * */
        addValueAttribute: function (Sidebar, element, labelText, placeholderText, attribute, confirm, fileSelect, pageSelect)
        {
            var selectedFilePath = element.attr(attribute);
            var inputActions = this.buildInputActions(Sidebar, fileSelect, pageSelect, selectedFilePath);

            var content = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    return element.attr(attribute);
                },
                function setterFunction(val)
                {
                    return element.attr(attribute, val);
                },
                labelText, placeholderText, confirm, inputActions
            );

            return content;
        },

        /**
         * Links the inner html of an element to an input box (eg. for iframes)
         *
         * element: element to change
         * labelText: name to show as label
         * placeholderText: string to show as placeholder
         * confirm: value only changes when user clicks apply button
         **/
        addValueHtml: function (Sidebar, element, labelText, placeholderText, confirm)
        {
            return this.createTextInput(Sidebar,
                function getterFunction()
                {
                    return $.trim(element.html());
                },
                function setterFunction(val)
                {
                    return element.html($.trim(val));
                }, labelText, placeholderText, confirm);
        },

        /**
         * Create a vertical list of mutually exclusive radio buttons
         *
         * @param labelText
         * @param attribute
         * * @param values array of objects with a label and value property (if a 'disabled' property is 'true', the radio will be disabled)
         * @param changeListener
         * @returns {*|jQuery|HTMLElement}
         */
        addRadioAttribute: function (labelText, attribute, values, initialValue, changeListener)
        {
            var addRadio = function (formGroup, name, value, label, isDisabled, initCallback, changeCallback)
            {
                //Note: 'radio' is a bootstrap class
                var radioEl = $('<div class="radio"' + (isDisabled ? ' disabled' : '') + '>').appendTo(formGroup);

                var id = Commons.generateId();
                var labelEl = $('<label for="' + id + '">').appendTo(radioEl);

                var isChecked = false;
                if (initCallback) {
                    isChecked = initCallback(value);
                }

                var input = $('<input type="radio" id="' + id + '" name="' + name + '" value="' + value + '"' + (isChecked ? ' checked' : '') + '>').appendTo(labelEl);
                if (label) {
                    labelEl.append(label);
                }

                input.change(function (event)
                {
                    var newValue = $('input[name=\'' + name + '\']:checked').val();

                    if (changeCallback) {
                        changeCallback(newValue);
                    }
                });

                //force a manual change if we're initing this radio,
                //since we're actually changing from nothing to selected
                if (isChecked && changeCallback) {
                    changeCallback(value);
                }

                return radioEl;
            };

            // Generate a common value to group the radios
            var name = Commons.generateId();

            // Create container for radios with label to add to sidebar
            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '" />');
            if (labelText) {
                //TODO the name is never used as Id (only as common name for all radio buttons), maybe add it somewhere?
                var label = ($('<label for="'+name+'">' + labelText + '</label>')).appendTo(formGroup);
            }

            var radioGroup = $('<div class="' + BlocksConstants.RADIO_GROUP_CLASS + '" />').appendTo(formGroup);

            var initStateCallback = function(value)
            {
                var retVal = false;

                if (initialValue && value==initialValue) {
                    retVal = true;
                }

                return retVal;
            };

            for (var i = 0; i < values.length; i++) {
                var c = values[i];
                addRadio(radioGroup, name, c.value, c.label, c.disabled, initStateCallback, changeListener);
            }

            return formGroup;
        },
        // addUniqueClass: function (Sidebar, element, labelText, values, changeListener)
        // {
        //     var id = Commons.generateId();
        //     var content = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '" />');
        //     content.append($('<label for="' + id + '">' + labelText + '</label>'));
        //     var dropdown = $('<div class="dropdown"/>').appendTo(content);
        // }

        //-----PRIVATE METHODS-----
        //These are more or less real private methods, used in the add* functions above
        /**
         * Create a text input box with clear functionality and with optional helper selection methods, implemented as a dropdown arrow.
         *
         * getterFunction: the function to use to get the value we're changing
         * setterFunction: the function to use to set the value we're changing
         * labelText: name to show as label
         * placeholderText: string to show as placeholder
         * confirm: value only changes when user clicks apply button
         **/
        createTextInput: function (Sidebar, getterFunction, setterFunction, labelText, placeholderText, confirm, dropdownActions)
        {
            var id = Commons.generateId();

            var resetBtn = null;
            var selectBtn = null;

            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
            }
            var inputGroup = $('<div class="input-group ' + BlocksConstants.INPUT_WITH_BUTTONS_CLASS + '"></div>').appendTo(formGroup);
            var input = $('<input id="' + id + '" type="text" class="form-control" placeholder="' + placeholderText + '">').appendTo(inputGroup);

            var oldVal = '';
            if (getterFunction) {
                oldVal = getterFunction();
                if (!oldVal) {
                    oldVal = '';
                }
            }
            input.attr(Widget.OLD_VAL_ATTR, oldVal);
            input.val(oldVal);
            input.change();

            var inputActions = $('<div class="input-group-btn"/>').appendTo(inputGroup);

            //check if we need to show the reset button
            if (input.attr(Widget.OLD_VAL_ATTR) !== '') {
                resetBtn = $('<a title="Reset value" class="btn btn-default btn-reset"><i class="fa fa-rotate-left"></a>').appendTo(inputActions);
                resetBtn.click(function (e)
                {
                    input.val(input.attr(Widget.OLD_VAL_ATTR));
                    input.change();
                    input.focus();
                });
                //don't let input lose focus when the button is clicked
                resetBtn.mousedown(function (e)
                {
                    return false;
                });
            }

            //append the clear button
            var clearBtn = $('<a title="Clear value" class="btn btn-default btn-clear"><i class="fa fa-times"></a>').appendTo(inputActions);
            input.on("change keyup focus", function (event)
            {
                inputGroup.addClass('focus');

                if (input.val() == null || input.val() == '') {
                    clearBtn.removeClass("show");
                }
                else {
                    clearBtn.addClass("show");
                }

                if (resetBtn) {
                    if (input.val() === input.attr(Widget.OLD_VAL_ATTR)) {
                        resetBtn.removeClass("show");
                    }
                    else {
                        resetBtn.addClass("show");
                    }
                }

                if (selectBtn) {
                    selectBtn.addClass("show");
                }

                if (event.type !== "focus" && !confirm && setterFunction) {
                    setterFunction(input.val());
                }
            });
            input.on("blur", function (event)
            {
                inputGroup.removeClass('focus');

                if (clearBtn) {
                    clearBtn.removeClass("show");
                }
                if (resetBtn) {
                    resetBtn.removeClass("show");
                }
                if (selectBtn) {
                    selectBtn.removeClass("show");
                }
            });

            //don't let input lose focus when the button is clicked
            clearBtn.mousedown(function (e)
            {
                return false;
            });
            clearBtn.click(function (e)
            {
                input.val('');
                input.change();
                input.focus();
            });

            //check if there are extra actions (next to reset and clear)
            if (dropdownActions) {
                var dropdownOptions = $('<ul class="dropdown-menu dropdown-menu-right"/>');
                var firstLink = null;
                var firstLinkCaption = null;
                $.each(dropdownActions, function (key, value)
                {
                    var option = $('<li />').appendTo(dropdownOptions);
                    var link = $('<a href="javascript:void(0)">' + key + '</a>').appendTo(option);
                    if (value[Widget.TEXT_INPUT_ACTION_OPTION_DISABLE] === true) {
                        option.addClass("disabled");
                    }
                    //don't add the event handler when the link is disabled
                    else {
                        if (value[Widget.TEXT_INPUT_ACTION_OPTION_ONSELECT]) {
                            link.click(function (event)
                            {
                                if (selectBtn) {
                                    //close the dropdown menu
                                    selectBtn.dropdown('toggle');
                                }

                                //let's pass the input field so the function knows where to put the result
                                value[Widget.TEXT_INPUT_ACTION_OPTION_ONSELECT](event, input);
                            });
                            if (!firstLink) {
                                firstLink = link;
                                firstLinkCaption = key;
                            }
                        }
                    }
                });

                if (dropdownOptions.children().length) {
                    //if we only have one link, let the users click it immediately
                    if (dropdownOptions.children().length == 1) {
                        selectBtn = $('<a title="' + firstLinkCaption + '" class="btn btn-default input-btn-actions"><i class="fa fa-search"></a>').appendTo(inputActions);
                        selectBtn.mousedown(function (e)
                        {
                            firstLink.click();
                        });
                    }
                    else {
                        selectBtn = $('<a title="More actions" class="btn btn-default input-btn-actions" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"><i class="fa fa-search"></a>').appendTo(inputActions);
                        //don't let input lose focus when the button is clicked
                        selectBtn.mousedown(function (e)
                        {
                            return false;
                        });

                        inputActions.append(dropdownOptions);
                    }
                }
            }

            if (confirm) {
                var actionsGroup = $('<div class="input-group actions"></div>').appendTo(formGroup);
                var applyBtn = $('<a class="btn btn-sm btn-primary"><i class="fa fa-check"></i> Apply</a>').appendTo(actionsGroup);
                applyBtn.click(function (event)
                {
                    if (setterFunction) {
                        setterFunction(input.val());
                    }
                });
            }
            else {
                //implemented in the input event handler
            }

            return formGroup;
        },

        /**
         * Create the input helper actions for a text input box (select file from server, select page from server).
         *
         * @param Sidebar
         * @param fileSelect boolean to enable file URL selection
         * @param pageSelect boolean to enable page URL selection
         * @param selectedFilePath initial pre-selection value of the media finder
         */
        buildInputActions: function (Sidebar, fileSelect, pageSelect, selectedFilePath)
        {
            var retVal = {};

            if (fileSelect) {
                var fileSelectOptions = {};
                fileSelectOptions[Widget.TEXT_INPUT_ACTION_OPTION_ONSELECT] = function (event, input)
                {
                    // Define variable so we can access it after of or cancel
                    var sidebarWidth = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS).outerWidth();

                    var finderOptions = {};

                    //TODO make this MediaConstants.AJAX_URL
                    if (selectedFilePath && selectedFilePath.indexOf('/webhdfs') === 0) {
                        finderOptions.selectedFile = selectedFilePath;
                    }

                    finderOptions.onSelect = function (selectedFileUrls)
                    {
                        if (selectedFileUrls.length > 0) {
                            var fileUrl = selectedFileUrls[0];
                            if (fileUrl.charAt(0) !== "/") {
                                fileUrl = "/" + fileUrl;
                            }

                            input.val(fileUrl);
                            input.change();
                            input.focus();
                        }
                        Sidebar.unloadFinder();
                        // restore sidebar width
                        Sidebar.animateSidebarWidth(sidebarWidth);
                    };

                    finderOptions.onCancel = function ()
                    {
                        Sidebar.unloadFinder();
                        // restore sidebar width
                        Sidebar.animateSidebarWidth(sidebarWidth);
                    };

                    Sidebar.loadFinder(finderOptions);
                    // save sidebar width
                    sidebarWidth = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS).outerWidth();
                    var windowWidth = $(window).width();

                    if (windowWidth / 2 > sidebarWidth) {
                        Sidebar.animateSidebarWidth(windowWidth / 2);
                    }
                };

                retVal["Select file from server..."] = fileSelectOptions;
            }

            if (pageSelect) {
                var pageSelectOptions = {};
                pageSelectOptions[Widget.TEXT_INPUT_ACTION_OPTION_DISABLE] = true;
                pageSelectOptions[Widget.TEXT_INPUT_ACTION_OPTION_ONSELECT] = function (event, input)
                {
                    alert("Coming soon!");
                };

                retVal["Lookup page address (coming soon)"] = pageSelectOptions;
            }

            return retVal;
        },

        /**
         * Create a combobox with configurable callbacks
         *
         * @param Sidebar
         * @param labelText
         * @param values
         * @param initCallback
         * @param changeCallback
         * @returns {*|jQuery|HTMLElement}
         */
        createCombobox: function (Sidebar, labelText, values, initCallback, changeCallback)
        {
            // Create selectbox to add to sidebar
            var id = Commons.generateId();
            var content = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '" />');
            content.append($('<label for="' + id + '">' + labelText + '</label>'));
            var dropdown = $('<div class="dropdown"/>').appendTo(content);
            var button = $('<button id="' + id + '" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="btn btn-default dropdown-toggle"><span class="text">' + BlocksMessages.comboboxEmptySelection + '</span>&#160;<span class="caret"></span></button>').appendTo(dropdown);

            // Create values inside selectbox and see which one to select
            dropdown.append($('<ul class="dropdown-menu" aria-labelledby="' + id + '"/>'));

            //call it once (can be called again)
            //we externalized this method to be able to load the data lazily when an async json call completed
            this.reinitCombobox(content, values, initCallback, changeCallback);

            return content;
        },

        /**
         * This method initializes the values of the supplied combobox (wiping existing values first).
         * It's meant to be called from the method above, or (again) after the data has arrived.
         *
         * @param combobox the return value of createCombobox()
         * @param values see createCombobox()
         * @param initCallback see createCombobox()
         * @param changeCallback see createCombobox()
         */
        reinitCombobox: function (combobox, values, initCallback, changeCallback)
        {
            //Note: sync this with the classes in createCombobox() above
            var dropdownMenu = combobox.find(".dropdown-menu");
            var dropdownToggle = combobox.find("button.dropdown-toggle");
            var id = dropdownToggle.attr("id");

            //start by clearing existing values in the ul list
            dropdownMenu.empty();

            var activateAfterInit = null;
            var activateAfterInitEl = null;
            for (var i = 0; i < values.length; i++) {
                var c = values[i];
                var li = $('<li />').appendTo(dropdownMenu);
                var a = $('<a data-value="' + c.value + '">' + c.name + '</a>').appendTo(li);

                var clickHandler = function (event, manualElement)
                {
                    //we need to re-create this variable in the closure because the loop overwrites the previous 'a's
                    var linkElement = manualElement || $(this);

                    var combo = linkElement.parents(".dropdown").find('.btn');
                    var text = linkElement.text();
                    var newValue = linkElement.data('value');
                    var oldValue = combo.val();

                    //make bootstrap dropdown behave like a regular <select>
                    combo.find('.text').text(text);
                    combo.val(newValue);
                    //save the selection to the dropdown menu
                    linkElement.parents(".dropdown").find('li').removeClass("active");
                    linkElement.parents("li").addClass("active");

                    if (changeCallback) {
                        //Note that this will probably also fire when old == new, but that's acceptable since this way, the very first value is propagated up as well
                        changeCallback(oldValue, newValue, event);
                    }

                    //close the dropdown on click, apparently this didn't work automatically...
                    var dropDown = $('#' + id);
                    //hope this is _always_ ok
                    if (dropDown.attr('aria-expanded') == "true") {
                        dropDown.dropdown("toggle");
                    }
                };

                a.on("click", clickHandler);

                if (initCallback) {
                    //only save the first match
                    if (activateAfterInit == null && initCallback(c.value)) {
                        activateAfterInit = clickHandler;
                        activateAfterInitEl = a;
                    }
                }
            }

            if (activateAfterInit) {
                activateAfterInit(null, activateAfterInitEl);
            }
            // if we don' have anything to activate, make sure we get rid of the "Loading" entry
            // by reverting back to the default empty label
            else {
                dropdownToggle.find('.text').text(BlocksMessages.comboboxEmptySelection);
            }
        },

        /**
         * Create a custom, mobile friendly styled toggle switch button
         *
         * @param labelText
         * @param initStateCallback
         * @param switchStateCallback
         * @param onLabel
         * @param offLabel
         * @returns {*|jQuery|HTMLElement}
         */
        createToggleButton: function (labelText, initStateCallback, switchStateCallback, onLabel, offLabel)
        {
            var INACTIVE_FA_CLASS = 'fa-square-o';
            var ACTIVE_FA_CLASS = 'fa-check-square-o';

            if (!onLabel) {
                onLabel = BlocksMessages.toggleLabelOn;
            }
            if (!offLabel) {
                offLabel = BlocksMessages.toggleLabelOff;
            }

            // Create selectbox to add to sidebar
            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '" />');

            // Create checkboxes for each value
            var id = Commons.generateId();
            var label = $('<label for="' + id + '">' + labelText + '</label>').appendTo(formGroup);
            var ACTIVE_ATTR = "data-active";
            var input = $('<input id="' + id + '" type="checkbox" data-toggle="toggle" data-size="small" ' + ACTIVE_ATTR + '="false" aria-pressed="false">').appendTo(formGroup);
            //var input = $('<button id="' + id + '" type="button" class="btn btn-default btn-sm btn-toggle pull-right" data-toggle="button" aria-pressed="false" autocomplete="off"><i class="fa fa-fw"></i></button>').appendTo(formGroup);

            var toggleState = function (newState)
            {
                input.attr(ACTIVE_ATTR, '' + newState);

                if (newState) {
                    input.attr("checked", "checked");
                }
                else {
                    input.removeAttr("checked");
                }

                var fa = input.find(".fa");
                if (fa.length > 0) {
                    fa.removeClass(ACTIVE_FA_CLASS);
                    fa.removeClass(INACTIVE_FA_CLASS);

                    //don't set .active or aria-pressed; bootstrap does it already for you
                    if (newState) {
                        fa.addClass(ACTIVE_FA_CLASS);
                    } else {
                        fa.addClass(INACTIVE_FA_CLASS);
                    }
                }
            };

            //init state
            var initState = initStateCallback();
            toggleState(initState);
            // we need to set this once manually
            if (initState) {
                input.attr('aria-pressed', 'true');
                input.addClass('active');
                input.attr(ACTIVE_ATTR, 'true');
            }

            //init the toggle api
            input.bootstrapToggle({
                on: onLabel,
                off: offLabel,
                style: 'pull-right'
            });

            //listener
            input.change(function (e)
            {
                var oldState = input.attr(ACTIVE_ATTR) === 'true';
                toggleState(!oldState);
                switchStateCallback(oldState, !oldState);
            });

            return formGroup;
        },

        createAutocompleteWidget: function (element, contentAttr, formGroupExtraClass, acEndpointOptions, labelText, initialValue, setterFunction)
        {
            var id = Commons.generateId();
            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            if (formGroupExtraClass) {
                formGroup.addClass(formGroupExtraClass);
            }
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
                        url: acEndpointOptions[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT],
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
                limit: parseInt(acEndpointOptions[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS]) - 1,
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
                $.getJSON(acEndpointOptions[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT] + encodeURIComponent(suggestion.value))
                    .done(function (data)
                    {
                        setterFunction(element, initialValue, data);
                    })
                    .fail(function (xhr, textStatus, exception)
                    {
                        Notification.error(BlocksMessages.generalServerDataError + (exception ? "; " + exception : ""), xhr);
                    });
            });

            ////init and attach the change listener

            var firstValue = element.attr(contentAttr);

            //if the html widget is uninitialized or empty (because we empty it while resetting), try to set it to a default value
            if (typeof firstValue === typeof undefined || firstValue === '') {
                //initial value may be 0 or '', so check of type
                if (typeof initialValue !== typeof undefined) {
                    //signal the setter function to reset the tag
                    setterFunction(element, initialValue, null);
                }
            }
            else {
                //this gives us a chance to skip this if it would be needed
                if (typeof firstValue !== typeof undefined) {
                    //if we have a real value, contact the resource endpoint to load the official name (not the more human friendly label) into the autocomplete box
                    if (firstValue != initialValue) {
                        $.getJSON(acEndpointOptions[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT] + encodeURIComponent(firstValue))
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
    });
}]);
/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This is the abstract superclass that all widgets need to extend
 *
 * Created by bram on 8/13/15.
 */
base.plugin("blocks.imports.Widget", ["constants.blocks.core", "messages.blocks.core", "constants.blocks.media.core", "constants.blocks.media.commons", "base.core.Class", "base.core.Commons", "blocks.core.Notification", "blocks.core.Undo", function (BlocksConstants, BlocksMessages, MediaConstants, MediaCommonsConstants, Class, Commons, Notification, Undo)
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
                    throw Logger.error("Could not instantiate widget because the 'tags' option (an array containing the tags you want this widget to be registered to) was missing or has the wrong type.", selectors);
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

        //-----PUBLIC -----
        //this is the default weight (other than undefined) that is used to pull up
        // an internal setting to the top of the sidebar list
        CONFIG_WEIGHT_DEFAULT: 10,

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
         * Sets the setting belonging to key on the config element to the specified value
         */
        setSetting: function (configElement, key, value)
        {

            var config = configElement.data(BlocksConstants.SIDEBAR_CONFIG_KEY);
            if (!config) {
                config = {};
            }
            config[key] = value;

            configElement.data(BlocksConstants.SIDEBAR_CONFIG_KEY, config);

            return configElement;
        },
        /**
         * Returns the setting belonging to key on the config element or null if it's not there.
         */
        getSetting: function (configElement, key)
        {
            var config = configElement.data(BlocksConstants.SIDEBAR_CONFIG_KEY);
            return config ? config[key] : null;
        },
        /**
         * This method can be used as a shortcut to wrap the config element with a setting so it ends up in
         * the advanced section.
         *
         * For example: retVal.push(this.setAdvancedSetting(this.addUniqueClass(Sidebar, block.element, ...)));
         */
        setAdvancedSetting: function (configElement, weight)
        {

            this.setSetting(configElement, BlocksConstants.SIDEBAR_CONFIG_ADVANCED_KEY, true);

            if (typeof weight != 'undefined') {
                this.setSetting(configElement, BlocksConstants.SIDEBAR_CONFIG_WEIGHT_KEY, weight);
            }

            return configElement;
        },
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

            var initValue = null;
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

                    //save the init value so we can skip it in the change callback
                    if (retVal && initValue == null) {
                        initValue = testValue;
                    }

                    return retVal;
                },
                function changeCallback(oldValue, newValue)
                {
                    var undoAttr = 'class';
                    var oldUndoVal = element.attr(undoAttr);

                    //this will reset the classes even if newValue is ""
                    for (var i = 0; i < values.length; i++) {
                        element.removeClass(values[i].value);
                    }

                    if (newValue) {
                        element.addClass(newValue);
                    }

                    var initialChange = !oldValue && newValue == initValue;
                    if (!initialChange) {
                        Undo.recordAttributeChange(element, undoAttr, oldUndoVal, retVal.find('button.dropdown-toggle'), oldValue, newValue);
                    }

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
                    var retVal = false;

                    if (attribute) {
                        retVal = element.hasAttribute(attribute);
                    }
                    else {
                        retVal = element.hasClass(value);
                    }

                    return retVal;
                },

                function switchStateCallback(oldValue, newValue)
                {
                    var undoAttr = attribute ? attribute : 'class';
                    var oldUndoVal = element.attr(undoAttr);

                    if (attribute) {
                        if (newValue) {
                            //Note: having a value seems to be necessary
                            element.attr(attribute, 'true')
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

                    //Note: the callback doesn't seem to be called on init, so it's safe to just log all changes
                    Undo.recordAttributeChange(element, undoAttr, oldUndoVal, retVal.find('input[type="checkbox"]'), oldValue, newValue);

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

                //sync this with switch below
                var undoAttr = attribute ? attribute : 'class';
                var oldUndoVal = element.attr(undoAttr);

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

                var oldIdx = event.value.oldValue;
                var newIdx = event.value.newValue;

                Undo.recordAttributeChange(element, undoAttr, oldUndoVal, formGroup.find('input[type="range"]'), oldIdx, newIdx);

                //propagate up if we have a someone listening
                if (changeListener) {
                    var oldValue = values[oldIdx] || undefined;
                    var newValue = values[newIdx] || undefined;
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
            var initValue = null;
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

                    //save the init value so we can skip it in the change callback
                    if (retVal && initValue == null) {
                        initValue = testValue;
                    }

                    return retVal;
                },
                function changeCallback(oldValue, newValue)
                {
                    var oldVal = element.attr(attribute);

                    if (newValue) {
                        element.attr(attribute, newValue);
                    }
                    else {
                        if (hasAttr) {
                            element.removeAttr(attribute);
                        }
                    }

                    var initialChange = !oldValue && newValue == initValue;
                    if (!initialChange) {
                        Undo.recordAttributeChange(element, attribute, oldVal, retVal.find('button.dropdown-toggle'));
                    }

                    //propagate up if we have a someone listening
                    if (changeListener) {
                        changeListener(oldValue, newValue);
                    }
                });

            return retVal;
        },

        addUniqueAttributeValueAsync: function (Sidebar, element, labelText, attribute, valuesEndpoint, nameProperty, valueProperty, changeListener, addEmptyEntry)
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

                    if (addEmptyEntry) {
                        comboEntries.push({
                            name: BlocksMessages.comboboxEmptySelection,
                            value: ''
                        });
                    }

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

                    _this.sortComboEntries(comboEntries);

                    var hasAttr = element.hasAttribute(attribute);
                    //get the value of the attribute on the element
                    var attr = hasAttr ? element.attr(attribute) : null;
                    var attrFound = false;

                    //we externalized this method to be able to load the data lazily when an async json call completed
                    var initValue = null;
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

                            //save the init value so we can skip it in the change callback
                            if (retVal && initValue == null) {
                                initValue = testValue;
                            }

                            //return true if this element needs to be selected
                            return retVal;
                        },
                        function changeCallback(oldValue, newValue)
                        {
                            var oldValueTerm = _this._termMappings[comboId][oldValue];
                            var newValueTerm = _this._termMappings[comboId][newValue];

                            var oldVal = element.attr(attribute);

                            element.removeAttr(attribute);

                            //if we have a new value and an attribute to set, set it
                            if (attribute && newValue) {
                                element.attr(attribute, newValue);
                            }

                            if (changeListener) {
                                changeListener(oldValueTerm, newValueTerm);
                            }

                            //On first load, this change callback is called when the value is updated
                            //from undefined/empty to the selected value. We don't want this to record
                            //an undo event, because the real initial state is after this value has been set.
                            var initialChange = !oldValue && newValue == initValue;
                            if (!initialChange) {
                                Undo.recordAttributeChange(element, attribute, oldVal, retVal.find('button.dropdown-toggle'));
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
            //TODO no undo/redo functionality implemented because no use of this method at the time of adding it
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
         */
        addValueAttribute: function (Sidebar, element, labelText, placeholderText, attribute, confirm, fileSelect, pageSelect)
        {
            var selectedFilePath = element.attr(attribute);

            if (Commons.isUriAttribute(attribute)) {
                selectedFilePath = Commons.defingerprint(selectedFilePath);
            }

            var inputActions = this.buildInputActions(Sidebar, fileSelect, pageSelect, selectedFilePath);

            var content = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    var retVal = element.attr(attribute);

                    if (Commons.isUriAttribute(attribute)) {
                        retVal = Commons.defingerprint(retVal);
                    }

                    return retVal;
                },
                function setterFunction(val)
                {
                    var oldVal = element.attr(attribute);
                    var newVal = $.trim(val);
                    var retVal = element.attr(attribute, newVal);

                    if (oldVal != newVal) {
                        Undo.recordAttributeChange(element, attribute, oldVal, content.find('input'));
                    }

                    return retVal;
                },
                labelText, placeholderText, confirm, inputActions
            );

            return content;
        },

        /**
         * Links the value of a configurable attribute to a clickable link with a selection method
         *
         * element: element to change
         * labelText: name to show as label
         * attribute: name of the attribute the value changes
         * confirm: value only changes when user confirms
         * fileSelect: user can only select file from server
         * pageSelect: user can select a page url from the sitemap
         */
        addValueAttributeSelection: function (Sidebar, element, labelText, placeholderText, attribute, confirm, fileSelect, pageSelect)
        {
            var selectedFilePath = element.attr(attribute);

            if (Commons.isUriAttribute(attribute)) {
                selectedFilePath = Commons.defingerprint(selectedFilePath);
            }

            var inputActions = this.buildInputActions(Sidebar, fileSelect, pageSelect, selectedFilePath);
            //for now, we'll take the first one
            var inputAction = null;
            $.each(inputActions, function (key, value)
            {
                inputAction = value;
                return false;
            });

            var content = this.createLinkInput(Sidebar,
                function getterFunction()
                {
                    var retVal = element.attr(attribute);

                    if (Commons.isUriAttribute(attribute)) {
                        retVal = Commons.defingerprint(retVal);
                    }

                    return retVal;
                },
                function setterFunction(val)
                {
                    var oldVal = element.attr(attribute);
                    var newVal = $.trim(val);
                    var retVal = element.attr(attribute, newVal);

                    if (oldVal != newVal) {
                        //note: this input selector will find the hidden input field (with a change listener)
                        Undo.recordAttributeChange(element, attribute, oldVal, content.find('input'));
                    }

                    return retVal;
                },
                labelText, placeholderText, confirm, inputAction
            );

            return content;
        },

        /**
         * Links the inner html of an element to an input box or textarea (eg. for iframes)
         *
         * element: element to change
         * labelText: name to show as label
         * placeholderText: string to show as placeholder
         * confirm: value only changes when user clicks apply button
         * textarea: set to true if you want to render a <textarea> element instead of an <input> element
         **/
        addValueHtml: function (Sidebar, element, labelText, placeholderText, confirm, textarea)
        {
            var inputEl = null;

            var getterFunction = function ()
            {
                return $.trim(element.html());
            };
            var setterFunction = function (val)
            {
                var oldVal = element.html();
                var newVal = $.trim(val);
                var retVal = element.html(newVal);

                if (oldVal != newVal) {
                    Undo.recordHtmlChange(element, oldVal, inputEl.find('input'));
                }

                return retVal;
            };

            if (textarea) {
                inputEl = this.createTextareaInput(Sidebar, getterFunction, setterFunction, labelText, placeholderText);
            }
            else {
                inputEl = this.createTextInput(Sidebar, getterFunction, setterFunction, labelText, placeholderText, confirm);
            }

            return inputEl;
        },

        /**
         * Create a vertical list of mutually exclusive radio buttons
         *
         * @param labelText
         * @param attribute
         * @param values array of objects with a label and value property (if a 'disabled' property is 'true', the radio will be disabled)
         * @param initialValue
         * @param changeListener
         * @returns {*|jQuery|HTMLElement}
         */
        addRadioAttribute: function (labelText, attribute, values, initialValue, changeListener)
        {
            //TODO no undo/redo functionality implemented because no use of this method at the time of adding it
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
                var label = ($('<label for="' + name + '">' + labelText + '</label>')).appendTo(formGroup);
            }

            var radioGroup = $('<div class="' + BlocksConstants.RADIO_GROUP_CLASS + '" />').appendTo(formGroup);

            var initStateCallback = function (value)
            {
                var retVal = false;

                if (initialValue && value == initialValue) {
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

        /**
         * Links the value of a css style to the values in a controlled list
         *
         * element: element to change
         * labelText: name to show as label
         * cssProperty: name of the css property the value changes
         * values = array of objects {value: 'a value to change', name: 'name of the value, (optional) default: true}
         */
        addUniqueStyle: function (Sidebar, element, labelText, cssProperty, values, changeListener)
        {
            //TODO no undo/redo functionality implemented because no use of this method at the time of adding it
            var _this = this;

            //note: we parse the raw style attribue instead of using $.css() to control the html-attribute better
            var initStyles = this.splitStyles(element.attr('style'));

            var defaultObj = null;
            for (var i = 0; i < values.length; i++) {
                var val = values[i];
                if (val.default) {
                    defaultObj = val;
                    break;
                }
            }

            var initValue = initStyles[cssProperty];
            if (!initValue && defaultObj) {
                initValue = defaultObj.value;
            }

            var content = this.createCombobox(Sidebar, labelText, values,
                function initCallback(testValue)
                {
                    var retVal = false;

                    //make sure we don't set the attribute to "undefined"
                    if (typeof testValue !== typeof undefined) {
                        if (testValue == initValue) {
                            retVal = true;
                        }
                    }

                    return retVal;
                },
                function changeCallback(oldValue, newValue)
                {
                    var oldStyle = element.attr('style');
                    var styles = _this.splitStyles(oldStyle);

                    delete styles[cssProperty];
                    if (newValue != "") {
                        if (defaultObj && defaultObj.value == newValue) {
                            //NOOP: instead of adding the default value explicitly when it's chosen,
                            //      we delete it and let the browser decide the default
                        }
                        else {
                            styles[cssProperty] = newValue;
                        }
                    }

                    var styleStr = _this.joinStyles(styles);
                    if (styleStr && styleStr.length > 0) {
                        element.attr('style', styleStr);
                    }
                    else {
                        element.removeAttr('style');
                    }

                    //Note: untested code
                    if (oldValue != newValue) {
                        Undo.recordAttributeChange(element, 'style', oldStyle, content.find('button.dropdown-toggle'), oldValue, newValue);
                    }

                    //propagate up if we have a someone listening
                    if (changeListener) {
                        changeListener(oldValue, newValue);
                    }
                });

            return content;
        },

        /**
         * Links the value of a css style to an input box (with optional selection methods)
         *
         * element: element to change
         * labelText: name to show as label
         * cssProperty: name of the css property the value changes
         * confirm: value only changes when user confirms
         * fileSelect: user can only select file from server
         * pageSelect: user can select a page url from the sitemap
         * */
        addValueStyle: function (Sidebar, element, labelText, placeholderText, cssProperty, confirm, fileSelect, pageSelect)
        {
            var _this = this;

            //note: we parse the raw style attribue instead of using $.css() to control the html-attribute better
            var styles = this.splitStyles(element.attr('style'));

            var content = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    return styles[cssProperty];
                },
                function setterFunction(val)
                {
                    var oldStyle = element.attr('style');
                    var styles = _this.splitStyles(oldStyle);
                    var oldVal = styles[cssProperty];

                    delete styles[cssProperty];
                    if (val != "") {
                        styles[cssProperty] = val;
                    }

                    var styleStr = _this.joinStyles(styles);
                    if (styleStr && styleStr.length > 0) {
                        element.attr('style', styleStr);
                    }
                    else {
                        element.removeAttr('style');
                    }

                    if (oldVal != val) {
                        Undo.recordAttributeChange(element, 'style', oldStyle, content.find('input'), oldVal, val);
                    }
                },
                labelText, placeholderText, confirm,

                //we omitted the last param (preselection) because the input is not always predictable
                this.buildInputActions(Sidebar, fileSelect, pageSelect)
            );

            return content;
        },

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
                resetBtn = $('<a title="' + BlocksMessages.inputActionResetValueTitle + '" class="btn btn-default btn-reset"><i class="fa fa-rotate-left"></a>').appendTo(inputActions);
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
            var clearBtn = $('<a title="' + BlocksMessages.inputActionClearValueTitle + '" class="btn btn-default btn-clear"><i class="fa fa-times"></a>').appendTo(inputActions);
            input.on("change keyup focus", function (event)
            {
                inputGroup.addClass('focus');
                var val = input.val();

                if (val == null || val == '') {
                    clearBtn.removeClass("show");
                }
                else {
                    clearBtn.addClass("show");
                }

                if (resetBtn) {
                    if (val === input.attr(Widget.OLD_VAL_ATTR)) {
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
                    setterFunction(val);
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
                var dropdownOptions = $('<ul class="dropdown-menu dropdown-menu-right" role="menu"/>');
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
         * Create a link with clear functionality and with a helper selection method that is called on click.
         *
         * getterFunction: the function to use to get the value we're changing
         * setterFunction: the function to use to set the value we're changing
         * labelText: name to show as label
         * placeholderText: string to show as placeholder
         * confirm: value only changes when user clicks apply button
         **/
        createLinkInput: function (Sidebar, getterFunction, setterFunction, labelText, placeholderText, confirm, dropdownAction)
        {
            var id = Commons.generateId();

            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
            }

            //note that this setup (with the link, the html, the title and the hidden input) is expected in eg. admin.js of
            //the blocks-video and blocks-image imports. If you would ever change it, make sure to update them too.
            var inputGroup = $('<div class="' + BlocksConstants.LINK_GROUP_CLASS + '"></div>').appendTo(formGroup);
            var link = $('<a id="' + id + '" href="javascript:void(0)">' + placeholderText + '</a>').appendTo(inputGroup);
            var hidden = $('<input type="hidden">').appendTo(inputGroup);
            hidden.on("change", function (event)
            {
                var val = hidden.val();

                if (event.type !== "focus" && !confirm && setterFunction) {
                    setterFunction(val);
                    link.html(val);
                    link.attr('title', val);
                }
            });

            var oldVal = '';
            if (getterFunction) {
                oldVal = getterFunction();
                if (!oldVal) {
                    oldVal = '';
                }
            }
            link.attr(Widget.OLD_VAL_ATTR, oldVal);
            link.html(oldVal);
            link.attr('title', oldVal);

            //check if there are extra action
            if (dropdownAction) {

                if (dropdownAction[Widget.TEXT_INPUT_ACTION_OPTION_DISABLE] === true) {
                    link.addClass("disabled");
                }
                //don't add the event handler when the link is disabled
                else {
                    if (dropdownAction[Widget.TEXT_INPUT_ACTION_OPTION_ONSELECT]) {
                        link.click(function (event)
                        {
                            //let's pass the hidden field as the place where to put the result
                            dropdownAction[Widget.TEXT_INPUT_ACTION_OPTION_ONSELECT](event, hidden);
                        });
                    }
                }
            }

            if (confirm) {
                var actionsGroup = $('<div class="input-group actions"></div>').appendTo(formGroup);
                var applyBtn = $('<a class="btn btn-sm btn-primary"><i class="fa fa-check"></i> Apply</a>').appendTo(actionsGroup);
                applyBtn.click(function (event)
                {
                    if (setterFunction) {
                        setterFunction(hidden.val());
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

                    if (selectedFilePath && selectedFilePath.indexOf(MediaCommonsConstants.HDFS_URL_BASE) === 0) {

                        var mediaFilePath = decodeURI(selectedFilePath);

                        //decode it as the reverse of the encode below
                        finderOptions[MediaConstants.FINDER_OPTIONS_SELECTED_FILE] = mediaFilePath;
                    }

                    finderOptions.onSelect = function (selectedFileUrls)
                    {
                        if (selectedFileUrls.length > 0) {
                            //we can only select the first one
                            var fileUrl = selectedFileUrls[0];
                            if (fileUrl.charAt(0) !== "/") {
                                fileUrl = "/" + fileUrl;
                            }

                            //make sure special chars (like spaces) are parsed into valid URLs (eg. very important for RDFa parsing)
                            //Note: don't use encodeURIComponent() or all slashes will be encoded too...
                            fileUrl = encodeURI(fileUrl);

                            input.val(fileUrl);
                            input.change();
                            input.focus();
                        }
                        Sidebar.unloadFinder();
                        // restore sidebar width
                        Sidebar.setWidth(sidebarWidth);
                    };

                    finderOptions.onCancel = function ()
                    {
                        Sidebar.unloadFinder();
                        // restore sidebar width
                        Sidebar.setWidth(sidebarWidth);
                    };

                    // save sidebar width
                    sidebarWidth = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS).outerWidth();
                    var windowWidth = $(window).width();

                    //first move the sidebar, then load it, because we don't have a watcher that updates the components-size
                    // (was problematic with the statusbar elements)
                    if (windowWidth / 2 > sidebarWidth) {
                        Sidebar.setWidth(windowWidth / 2, function ()
                        {
                            Sidebar.loadFinder(finderOptions);
                        });
                    }
                    else {
                        Sidebar.loadFinder(finderOptions);
                    }
                };

                retVal[BlocksMessages.inputActionSelectFileTitle] = fileSelectOptions;
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

            //this will allow us to call button.val('value').change() on this button element
            //to manually update the combobox
            button.on('change', function (e)
            {
                dropdown.find('a[data-value="' + button.val() + '"]').first().click();
            });

            // Create values inside selectbox and see which one to select
            dropdown.append($('<ul class="dropdown-menu" role="menu" aria-labelledby="' + id + '"/>'));

            //add keyboard prefix search to standard bootstrap dropdown
            //for inspiration, see https://stackoverflow.com/questions/21474213/selecting-bootstrap-dropdown-value-by-key-press
            dropdown.bind('keydown', function (event)
            {
                var dropdown = $(this);
                var keyChar = String.fromCharCode(event.keyCode).toLowerCase();

                //this part adds functionality for longer prefixes when they're typed fast enough (<0.5sec)
                //note: for IE8 timestamp support, see https://stackoverflow.com/questions/221294/how-do-you-get-a-timestamp-in-javascript
                var timeStampInMs = window.performance && window.performance.now && window.performance.timing && window.performance.timing.navigationStart ? window.performance.now() + window.performance.timing.navigationStart : Date.now();
                var lastTimeStampInMs = dropdown.data('keydown-stamp');
                if (lastTimeStampInMs && (timeStampInMs - lastTimeStampInMs) < 500) {
                    var lastPrefix = dropdown.data('keydown-prefix');
                    if (lastPrefix) {
                        keyChar = lastPrefix + keyChar;
                    }
                }
                dropdown.data('keydown-stamp', timeStampInMs);
                dropdown.data('keydown-prefix', keyChar);

                var selectedItems = $(this).find('a').filter(function ()
                {
                    return $(this).text().toLowerCase().indexOf(keyChar) === 0;
                });

                //this part adds functionality to cycle through the
                var f = $(selectedItems).is(':focus');
                if (f) {
                    selectedItems = $('a:focus').parent().nextAll().find('a').filter(function ()
                    {
                        return $(this).text().toLowerCase().indexOf(keyChar) === 0;
                    }).first();

                    if (selectedItems.length == 0) {
                        selectedItems = $(this).find('a').filter(function ()
                        {
                            return $(this).text().toLowerCase().indexOf(keyChar) === 0;
                        });
                    }
                }

                selectedItems.first().focus();
            });

            //this will make sure the selected item is in the middle of the dropdown every time the menu is re-opened,
            //even if the menu has been scrolled to another item
            dropdown.on('shown.bs.dropdown', function ()
            {
                $(this).find('.active').first().find('a').focus();
            });

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
                var title = c.name;
                //note: this can be activated if you want subtitles
                var subtitle = null/*c.value*/;
                //Note: the href enables keyboard navigation
                var a = $('<a href="javascript:void(0)" data-value="' + c.value + '">' + title + (subtitle ? '<small>' + subtitle + '</small>' : '') + '</a>').appendTo(li);

                var clickHandler = function (event, manualElement)
                {
                    //we need to re-create this variable in the closure because the loop overwrites the previous 'a's
                    var linkElement = manualElement || $(this);

                    var combo = linkElement.parents(".dropdown").find('.btn');
                    //remove all sub elements before converting to text
                    //var text = linkElement.text();
                    var text = linkElement.clone().find(' *').remove().end().text();
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
                    //hope this is _always_ ok
                    if (dropdownToggle.attr('aria-expanded') == "true") {
                        dropdownToggle.dropdown("toggle");
                    }
                };

                //install the main change listener
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
            // if we don't have anything to activate, make sure we get rid of the "Loading" entry
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
         * @param enableDisabled This option was added to introduce an 'empty' or 'unset' state: only when the control is clicked once, the 'off' state is triggered
         *                       Note that to activate the disabled-state on creation, the initStateCallback() should return 'undefined'
         * @returns {*|jQuery|HTMLElement}
         */
        createToggleButton: function (labelText, initStateCallback, switchStateCallback, onLabel, offLabel, enableDisabled)
        {
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
            var checkboxGroup = $('<div class="' + BlocksConstants.TOGGLE_GROUP_CLASS + '" />').appendTo(formGroup);
            var input = $('<input id="' + id + '" type="checkbox">').appendTo(checkboxGroup);

            //init the toggle api
            input.bootstrapToggle({
                on: onLabel,
                off: offLabel,
                style: 'pull-right'
            });

            //init state
            var initState = initStateCallback();
            var startDisabled = false;
            if (enableDisabled && typeof initState === typeof undefined) {
                startDisabled = true;
                //let's always default to off in disabled state
                input.bootstrapToggle('off');
            }
            else if (initState) {
                input.bootstrapToggle('on');
            }
            else {
                input.bootstrapToggle('off');
            }

            //listener
            input.change(function (e)
            {
                if (switchStateCallback) {
                    var newState = $(this).prop('checked');
                    switchStateCallback(!newState, newState);
                }
            });

            //start disabled and enable on click
            if (enableDisabled) {
                //This is a bit of a hack: we find the wrapping toggle container (the one that actually receives and handles the events)
                // and we attach a click listener to enable the toggle widget on first click (and swallow the event)
                //Note that we can't use the built-in 'disable' features because disabled elements don't fire events,
                // so we simulate that disabledness
                var toggleBtn = input.closest('.toggle');

                var activate = function (e)
                {
                    checkboxGroup.removeClass('disabled');
                    //send out the current state to the change listener
                    if (switchStateCallback) {
                        switchStateCallback(null, input.prop('checked'));
                    }

                    //needs to come last
                    e.stopPropagation();
                };

                if (startDisabled) {
                    //simulation, see css
                    checkboxGroup.addClass('disabled');
                    toggleBtn.one('click', activate);
                }

                //if we enable disabled, we must offer the user a way to go back
                var reset = $('<a href="javascript:void(0);" class="btn btn-link btn-xs btn-reset"><i class="fa fa-trash-o"></a>').appendTo(checkboxGroup);
                reset.click(function (e)
                {
                    checkboxGroup.addClass('disabled');

                    var oldState = input.prop('checked');
                    //we always default to off in disabled state
                    input.bootstrapToggle('off');

                    //send out the current state to the change listener
                    if (switchStateCallback) {
                        switchStateCallback(oldState, null);
                    }

                    //reactivate the event listener
                    toggleBtn.one('click', activate);
                });
            }

            return formGroup;
        },

        /**
         * Create an autocomplete input box that communicates with a configurable backend endpoint for supplying values.
         *
         * @param element
         * @param contentAttr
         * @param formGroupExtraClass
         * @param acEndpointOptions
         * @param labelText
         * @param initialValue
         * @param setterFunction
         * @returns {*|jQuery|HTMLElement}
         */
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
            var input = $('<input id="' + id + '" type="text" class="form-control typeahead" placeholder="' + BlocksMessages.autocompleteInputPlaceholder + '">').appendTo(inputGroup);

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
                    empty: '<div class="tt-suggestion "' + BlocksConstants.INPUT_TYPE_RES_SUG_EMPTY_CLASS + '><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_TITLE_CLASS + '">' + BlocksMessages.autocompleteResultsEmpty + '</p></div>',
                    //we add title (hover) tags as well because the css will probably chop it off (ellipsis overflow)
                    suggestion: Handlebars.compile('<div title="{{title}} - {{subTitle}}"><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_TITLE_CLASS + '">{{title}}</p><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_SUBTITLE_CLASS + '">{{subTitle}}</p></div>')
                }
            };
            input.typeahead(options, dataSet);

            //gets called when a real selection is done
            input.bind('typeahead:select', function (ev, suggestion)
            {
                $.getJSON(acEndpointOptions[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT] + encodeURIComponent(suggestion.value))
                    .done(function (data)
                    {
                        setterFunction(element, data);
                    })
                    .fail(function (xhr, textStatus, exception)
                    {
                        Notification.error(BlocksMessages.generalServerDataError + (exception ? "; " + exception : ""), xhr);
                    });
            });
            input.on("change keyup", function (event)
            {
                //if the input is cleared, we wipe the resource
                if (!input.val()) {
                    //signal the setter function to reset the tag (with data==null)
                    setterFunction(element, null);
                }
            });

            //show/hide the spinner when busy
            var inputActions = $('<div class="input-group-addon invisible"><i class="fa fa-spinner fa-pulse fa-fw"></i></div>').appendTo(inputGroup);
            input.on('typeahead:asyncrequest', function ()
            {
                inputActions.removeClass('invisible');
            });
            input.on('typeahead:asynccancel typeahead:asyncreceive', function ()
            {
                inputActions.addClass('invisible');
            });

            ////init and attach the change listener

            var firstValue = undefined;
            if (typeof initialValue !== typeof undefined && initialValue !== null) {
                firstValue = initialValue;
            }
            else {
                if (contentAttr) {
                    firstValue = element.attr(contentAttr);
                }
            }

            //this gives us a chance to skip this if it would be needed
            if (firstValue) {
                //if we have a real value, contact the resource endpoint to load the official name (not the more human friendly label) into the autocomplete box
                $.getJSON(acEndpointOptions[BlocksConstants.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT] + encodeURIComponent(firstValue))
                    .done(function (data)
                    {
                        //init autocomplete input box
                        input.typeahead('val', data.name);

                        //don't think we need to re-set the html here, just init the autocomplete box
                        //Update: this used to be commented out, but our initialValue argument allows us to supply a scripted initial value (eg. not a property-attr saved initial value),
                        //        so it's possible we want to fire up the setter function (which can do whatever it wants) with the data coming back from the server,
                        //        so we re-activated it.
                        setterFunction(element, data);
                    })
                    .fail(function (xhr, textStatus, exception)
                    {
                        Notification.error(BlocksMessages.generalServerDataError + (exception ? "; " + exception : ""), xhr);
                    });
            }
            else {
                //signal the setter function to reset the tag (with data==null)
                setterFunction(element, null);
            }

            return formGroup;
        },

        /**
         * Create a text area input box
         *
         * getterFunction: the function to use to get the value we're changing
         * setterFunction: the function to use to set the value we're changing
         * labelText: name to show as label
         * placeholderText: string to show as placeholder
         * numRows: the number of rows to show (eg. the height of the textarea)
         **/
        createTextareaInput: function (Sidebar, getterFunction, setterFunction, labelText, placeholderText, numRows)
        {
            var id = Commons.generateId();

            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
            }
            var inputGroup = $('<div class="input-group"></div>').appendTo(formGroup);
            var input = $('<textarea id="' + id + '" rows="' + (numRows ? numRows : 5) + '" class="form-control" placeholder="' + placeholderText + '">').appendTo(inputGroup);

            var oldVal = '';
            if (getterFunction) {
                oldVal = getterFunction();
                if (!oldVal) {
                    oldVal = '';
                }
            }
            input.val(oldVal);
            input.change();

            var inputActions = $('<div class="input-group actions"/>').appendTo(formGroup);

            var applyBtn = $('<a class="btn btn-sm btn-primary"><i class="fa fa-check"/> ' + BlocksMessages.textareaApplyBtnTitle + '</a>').appendTo(inputActions);
            applyBtn.click(function (e)
            {
                if (setterFunction) {
                    setterFunction(input.val());
                }
            });
            inputActions.append('&#160;');

            //append the clear button
            var clearBtn = $('<a class="btn btn-sm btn-default btn-clear"><i class="fa fa-times"/> ' + BlocksMessages.textareaResetBtnTitle + '</a>').appendTo(inputActions);
            clearBtn.click(function (e)
            {
                input.val('');
                input.change();
                input.focus();

                if (setterFunction) {
                    setterFunction(input.val());
                }
            });
            inputActions.append('&#160;');

            return formGroup;
        },
        /**
         * Create a color input widget
         *
         * getterFunction: the function to use to get the value we're changing
         * setterFunction: the function to use to set the value we're changing
         * labelText: name to show as label
         *
         * TODO: we might take into consideration to make this a general 'input-typed' function (with inline or not switch)
         **/
        createColorInput: function (Sidebar, getterFunction, setterFunction, labelText)
        {
            //note: html5 spec says the color cannot be empty and defaults to black
            //see https://www.w3schools.com/jsreF/prop_color_value.asp
            var DEFAULT_VALUE = '#000000';

            var id = Commons.generateId();

            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"></div>');
            if (labelText) {
                var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
            }

            var colorGroup = $('<div class="' + BlocksConstants.COLOR_GROUP_CLASS + '" />').appendTo(formGroup);
            var input = $('<input id="' + id + '" type="color" class="form-control">');

            var oldVal = DEFAULT_VALUE;
            if (getterFunction) {
                oldVal = getterFunction();
                if (!oldVal) {
                    oldVal = DEFAULT_VALUE;
                }
                //when dragging blocks around, the css properties sometimes
                //get transformed (especially from hex to rgb). Also, the html5 color picker
                //doesn't support alpha values
                else if (oldVal.substring(0, 3) == 'rgb') {
                    //this is a basic rgb2hex function
                    //Note: match() returns An array containing the entire match result and any parentheses-captured matched results;
                    // null if there were no matches. So the first ()-capture is at [1]
                    var rgb = oldVal.match(/^rgba?[\s]*\([\s]*(\d+)[\s]*,[\s]*(\d+)[\s]*,[\s]*(\d+)[\s]*/i);
                    oldVal = (rgb && rgb.length === 4) ? "#" +
                        ("0" + parseInt(rgb[1], 10).toString(16)).slice(-2) +
                        ("0" + parseInt(rgb[2], 10).toString(16)).slice(-2) +
                        ("0" + parseInt(rgb[3], 10).toString(16)).slice(-2) : DEFAULT_VALUE;
                }
            }

            //Note: change callback doesn't happen automatically
            input.val(oldVal).change();

            //this will save the old value every time the color picker is launched,
            //so we can pass it along in the setterFunction
            input.on('click', function (e)
            {
                oldVal = input.val();
            });

            //listener
            //note: the 'change' event happens when the color picker is dismissed
            //      the 'input' event happens every time the color in the picker changes
            input.on('change', function (e)
            {
                if (setterFunction) {
                    //dependency code requires the 'reset-value' to be the empty string
                    setterFunction(oldVal == DEFAULT_VALUE ? '' : oldVal, input.val() == DEFAULT_VALUE ? '' : input.val());
                }
            });

            var reset = $('<a href="javascript:void(0);" class="btn btn-link btn-xs btn-reset"><i class="fa fa-trash-o"></a>').appendTo(colorGroup);
            reset.click(function (e)
            {
                oldVal = input.val();
                input.val(DEFAULT_VALUE).change();
            });

            input.appendTo(colorGroup);

            return formGroup;
        },
        sortComboEntries: function (comboEntries)
        {
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
        },
        createListGroup: function (labelText, sortable, reorderListener)
        {
            var formGroup = $('<div class="' + BlocksConstants.INPUT_TYPE_WRAPPER_CLASS + '"/>');
            var id = Commons.generateId();
            if (labelText) {
                var label = $('<label for="' + id + '">' + labelText + '</label>').appendTo(formGroup);
            }
            var listGroup = $('<div id="' + id + '" class="list-group"/>').appendTo(formGroup);

            if (sortable && Sortable) {
                var opts = {
                    //works nicer when the whole line is draggable, but works
                    //handle: ".handle",
                    animation: 250,
                    //this will create an empty space where we will drop
                    ghostClass: 'invisible',
                    draggable: '.list-group-item',
                    //don't let the perfect scrollbar items be sorted
                    filter: '.ps-scrollbar-x-rail, ps-scrollbar-y-rail',
                };
                if (reorderListener) {
                    opts.onUpdate = $.proxy(reorderListener, this);
                }

                Sortable.create(listGroup.get(0), opts);
            }

            if (jQuery().perfectScrollbar) {
                listGroup.perfectScrollbar();
            }

            return formGroup;
        },
        splitStyles: function (rawStyleValue)
        {
            var retVal = {};

            if (rawStyleValue) {
                var styleTuples = rawStyleValue.split(';');
                for (var i = 0; i < styleTuples.length; i++) {
                    var style = styleTuples[i];
                    if (style && style.length > 0) {
                        //Note: we can't just split on colon, because eg. an URL can contain a colon too
                        var idx = style.indexOf(':');
                        if (idx >= 0) {
                            var key = $.trim(style.substring(0, idx));
                            var val = $.trim(style.substring(idx + 1));
                            if (val.substring(0, 'url'.length) == 'url') {
                                val = val.substring('url'.length);
                                //trim all remaining braces, spaces and quotes from start and end
                                //See https://stackoverflow.com/questions/26156292/trim-specific-character-from-a-string
                                val = val.replace(/^[() "']+|[() "']+$/g, '');
                                //TODO also defingerprint if a URI?
                            }
                            retVal[key] = val;
                        }
                        else {
                            Logger.error('Encountered style tuple without a colon, this should not happen; ' + style);
                        }
                    }
                }
            }

            return retVal;
        },
        joinStyles: function (styleTuples)
        {
            var retVal = '';

            var _this = this;
            $.each(styleTuples, function (key, value)
            {
                retVal += key + ': ' + _this.parseStyleValue(value) + ';';
            });

            return retVal;
        },
        parseStyleValue: function (val)
        {
            //cleanup and makes sure the calculations below are correct in case of padding
            var retVal = $.trim(val);

            //URL test: not really 100% correct (eg. relative paths), but close enough for our use
            //TODO we should probably curate a list of css properties instead
            if (retVal.substring(0, '/'.length) == '/' || retVal.substring(0, 'http'.length) == 'http') {
                retVal = 'url(' + retVal + ')';
            }

            return retVal;
        },
    });
}]);
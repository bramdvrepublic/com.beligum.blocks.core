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

/**
 * Central plugin that holds references to a number of elements and centralizes keyboard actions.
 */
base.plugin("blocks.core.UI", ["base.core.Commons", "blocks.core.Broadcaster", "constants.blocks.core", function (Commons, Broadcaster, BlocksConstants)
{
    var UI = this;

    //-----CONSTANTS-----
    this.KEYCODE = {

        //in separate subobject so we can iterate them
        MODIFIER: {
            SHIFT: 16,
            CTRL: 17,
            ALT: 18,
            //special modifier, see keyhandler in manager why we invented it
            NONE: 0,
        },

        //special keys
        TAB: 9,
        DELETE: 46,
        SPACE: 32,
        BACKSPACE: 8,
        ESC: 27,

        //arrow keys
        LEFT: 37,
        UP: 38,
        RIGHT: 39,
        DOWN: 40,

        //normal keys
        S: 83,
        Y: 89,
        Z: 90,
    };

    //data key to set on events to let them pierce through (see manager for details)
    this.FORCE_CLICK_DATA = 'force';

    //-----VARIABLES-----
    // flag to enable/disable creation of new blocks
    this.allowCreate = true;
    // flag to enable/disable layout functionality (resize columns and move blocks around)
    this.allowLayout = true;
    // flag to enable/disable editing of existing blocks (giving them focus to start editing)
    this.allowEdit = true;
    // flag to enable/disable deleting of blocks
    this.allowDelete = true;

    this.window = $(window);
    this.html = $("html");
    this.body = $("body");

    this.startButton = undefined;
    this.sidebar = undefined;
    this.pageContent = undefined;
    this.containers = undefined;
    this.overlayWrapper = undefined;
    this.surfaceWrapper = undefined;
    this.resizerWrapper = undefined;
    this.dropspotWrapper = undefined;

    this.pageSurface = undefined;
    this.focusedSurface = undefined;

    //array of currently pressed keys
    this.keysPressed = {};
    //mapping between keystrokes and button/link selectors
    this.registeredKeystrokes = {};

    //-----PUBLIC METHODS-----
    /**
     * Returns the (registered) modifier key that's currently pressed.
     * If multiple modifiers are pressed, the firstly registered one is returned;
     *
     * @returns {number}
     */
    this.getActiveModifierKey = function ()
    {
        var activeModifier = UI.KEYCODE.MODIFIER.NONE;

        //iterate all known modifiers
        $.each(UI.KEYCODE.MODIFIER, function (key, value)
        {
            var retVal = true;

            if (UI.isKeyPressed(value)) {
                activeModifier = value;
                //this means the first modifier will win...
                retVal = false;
            }

            return retVal;
        });

        return activeModifier;
    };

    this.isValidModifier = function (keycode)
    {
        var valid = false;

        $.each(UI.KEYCODE.MODIFIER, function (key, value)
        {
            var retVal = true;

            if (keycode === value) {
                valid = true;
                retVal = false;
            }

            return retVal;
        });

        return valid;
    };

    /**
     * Returns true if the keyboard key with the supplied code is currently pressed.
     *
     * @param code
     * @returns {boolean}
     */
    this.isKeyPressed = function (code)
    {
        return this.keysPressed[code] === true;
    };

    /**
     * Register a callback that will be executed when the specified keycode is pressed
     */
    this.registerKeystrokeAction = function (keycode, modifier, action)
    {
        _registerKeystroke(keycode, modifier, function (data)
        {
            data.actions = data.actions || [];
            data.actions.push(action);
        });
    };

    /**
     * Register a new btn/link/... selector that will trigger() when the specified keycode is pressed,
     * but only of that element is visible
     */
    this.registerKeystrokeSelector = function (keycode, modifier, selector)
    {
        _registerKeystroke(keycode, modifier, function (data)
        {
            data.selectors = data.selectors || [];

            // make sure we don't register the same selector twice
            if (data.selectors.indexOf(selector) < 0) {
                data.selectors.push(selector);
            }
        });
    };

    /**
     * Fire up all things attached to this pressed key, taking the currently
     * pressed-down modifiers into account too.
     */
    this.fireKeystroke = function (keyEvent)
    {
        var retVal = false;

        var modifierMap = this.registeredKeystrokes[this.getActiveModifierKey()];
        if (modifierMap) {
            var data = modifierMap[keyEvent.keyCode];

            if (data) {
                retVal |= _fireKeystrokeActions(keyEvent, data.actions);
                retVal |= _fireKeystrokeSelectors(keyEvent, data.selectors);
            }

            if (retVal) {
                //override the default event handler if something happened
                keyEvent.preventDefault();
                keyEvent.stopPropagation();
            }
        }

        return retVal;
    };

    this.resetKeystrokes = function ()
    {
        this.keysPressed = {};
        this.registeredKeystrokes = {};
    };

    /**
     * Enables pierce-through mode on the event (see manager for details)
     *
     * @param event
     */
    this.setPierceThrough = function (event)
    {
        //Note: we need to set it in the originalEvent or it won't end up in the Menu's generic document.on() listener
        event.originalEvent.data = event.originalEvent.data || {};
        event.originalEvent.data[this.FORCE_CLICK_DATA] = true;

        //for chaining
        return event;
    };

    /**
     * This is a wrapper around the lower-level event broadcast to offer other modules
     * a more uniform and code-friendly way of requesting a refresh of the page admin UI.
     */
    this.refresh = function()
    {
        //note: no parent event or data, we want to keep it simple
        Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH);
    };

    /**
     * Debouncing function to make eventing more performant
     * by amortizing quick successions together.
     * See http://unscriptable.com/index.php/2009/03/20/debouncing-javascript-methods/
     * @param func
     * @param threshold
     * @param execAsap
     * @returns {debounced}
     */
    this.debounce = function (func, threshold, execAsap)
    {
        var timeout;

        return function debounced()
        {
            var obj = this, args = arguments;

            function delayed()
            {
                if (!execAsap) {
                    func.apply(obj, args);
                }
                timeout = null;
            }

            if (timeout) {
                clearTimeout(timeout);
            }
            else if (execAsap) {
                func.apply(obj, args);
            }

            timeout = setTimeout(delayed, threshold || 50);
        };
    };

    //-----GENERAL JQUERY FUNCTIONS-----
    /**
     * JQuery event listener for more performant window resizing.
     * Use like this: $(window).smartresize(function (event) {});
     */
    $.fn["smartresize"] = function (fn)
    {
        return fn ? this.on('resize', UI.debounce(fn)) : this.trigger("smartresize");
    };

    /**
     * JQuery event listener for more performant mouse move.
     * Use like this: $(document).on("smartmousemove", function (event) {});
     */
    $.fn["smartmousemove"] = function (fn)
    {
        return fn ? this.on('mousemove', UI.debounce(fn)) : this.trigger("smartmousemove");
    };

    //-----PRIVATE METHODS-----
    /**
     * Fire the registered keystroke actions for the keys pressed in the current event
     */
    var _fireKeystrokeActions = function (keyEvent, actions)
    {
        var retVal = false;

        if (actions) {
            for (var i = 0; i < actions.length; i++) {

                var action = actions[i];
                if (action) {
                    action(keyEvent);
                    retVal = true;
                }
            }
        }

        return retVal;
    };
    /**
     * Fire the registered keystroke selectors for the keys pressed in the current event
     */
    var _fireKeystrokeSelectors = function (keyEvent, selectors)
    {
        var retVal = false;

        if (selectors) {
            for (var i = 0; i < selectors.length; i++) {

                var selector = selectors[i];
                var el = $(selector);

                //note: this is a special filter
                // the element must:
                // 1) exist
                // 2) be unique
                // 3) be visible
                if (el && el.length === 1 && el.is(":visible")) {
                    //'click' the action element
                    el.trigger('click');

                    //signal the caller something was actually fired
                    retVal = true;
                }
            }
        }

        return retVal;
    };
    var _registerKeystroke = function (keycode, modifier, callback)
    {
        if (Commons.isUnset(modifier)) {
            //this way we can uniformly handle modifiers and no modifiers
            modifier = UI.KEYCODE.MODIFIER.NONE;
        }

        //check if the modifier is supported by our mapping
        if (UI.isValidModifier(modifier)) {
            //init structure if needed
            UI.registeredKeystrokes[modifier] = UI.registeredKeystrokes[modifier] || {};
            UI.registeredKeystrokes[modifier][keycode] = UI.registeredKeystrokes[modifier][keycode] || {};

            //note: this (currently) means we don't support multiple modifiers!
            callback(UI.registeredKeystrokes[modifier][keycode]);
        }
        else {
            Logger.error("Unable to register keystroke selector because the modifier isn't supported; " + modifier);
        }
    };

}]);
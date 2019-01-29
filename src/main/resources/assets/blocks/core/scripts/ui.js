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

base.plugin("blocks.core.UI", ["base.core.Commons", "constants.blocks.core", function (Commons, BlocksConstants)
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
    };

    //data key to set on events to let them pierce through (see manager for details)
    this.PIERCE_THROUGH_DATA = 'pierce-through';

    //-----VARIABLES-----
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
        $.each(UI.KEYCODE.MODIFIER, function(key, value) {
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

    this.isValidModifier = function(keycode)
    {
        var valid = false;

        $.each(UI.KEYCODE.MODIFIER, function(key, value) {
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
     * Register a new btn/link/... selector that will trigger() when the specified keycode is pressed,
     * but only of that element is visible
     * @param keycode
     * @param modifier
     * @param actionSelector
     */
    this.registerKeystrokeSelector = function(keycode, modifier, actionSelector)
    {
        if (Commons.isUnset(modifier)) {
            //this way we can uniformly handle modifiers and no modifiers
            modifier = UI.KEYCODE.MODIFIER.NONE;
        }

        //check if the modifier is supported by our mapping
        if (this.isValidModifier(modifier)) {
            this.registeredKeystrokes[modifier] = this.registeredKeystrokes[modifier] || {};
            this.registeredKeystrokes[modifier][keycode] = this.registeredKeystrokes[modifier][keycode] || [];
            //note: this (currently) means we don't support multiple modifiers!
            this.registeredKeystrokes[modifier][keycode].push(actionSelector);
        }
        else {
            Logger.error("Unable to register keystroke selector because the modifier isn't supported; "+modifier);
        }
    };

    /**
     * Fire the registered keystroke selectors for the keys pressed in the current event
     * @param keyEvent
     * @returns {boolean}
     */
    this.fireKeystrokeSelectors = function(keyEvent)
    {
        var retVal = false;

        var modifierMap = this.registeredKeystrokes[this.getActiveModifierKey()];
        if (modifierMap) {
            var selectors = modifierMap[keyEvent.keyCode];
            if (selectors) {
                for (var i=0;i<selectors.length;i++) {

                    var selector = selectors[i];
                    var el = $(selector);

                    //note: this is a special filter
                    // the element must:
                    // 1) exist
                    // 2) be unique
                    // 3) be visible
                    if (el && el.length === 1 && el.is(":visible")) {
                        //override the default event handler
                        keyEvent.preventDefault();
                        keyEvent.stopPropagation();

                        //'click' the action element
                        el.trigger('click');

                        //signal the caller something was actually fired
                        retVal = true;
                    }

                    //it makes sense to stop processing after the first hit
                    if (retVal) {
                        break;
                    }
                }
            }
        }

        return retVal;
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
        event.originalEvent.data[this.PIERCE_THROUGH_DATA] = true;

        //for chaining
        return event;
    };

    //-----PRIVATE METHODS-----

}]);
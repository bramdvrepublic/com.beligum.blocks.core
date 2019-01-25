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

base.plugin("blocks.core.UI", ["constants.blocks.core", function (BlocksConstants)
{
    var UI = this;

    //-----CONSTANTS-----
    this.KEYCODE = {
        SHIFT: 16,
        CTRL: 17,
        TAB: 9,
        DELETE: 46,
        ALT: 18,
        SPACE: 32,
        BACKSPACE: 8,
        ESC: 27,
        LEFT: 37,
        UP: 38,
        RIGHT: 39,
        DOWN: 40,

        S: 83,
    };
    this.PIERCE_THROUGH_DATA = 'pierce-through';

    //-----VARIABLES-----
    this.window = $(window);
    this.html = $("html");
    this.body = $("body");
    this.startButton = undefined;
    this.sidebar = undefined;
    this.pageContent = undefined;
    this.overlayWrapper = undefined;
    this.surfaceWrapper = undefined;
    this.resizerWrapper = undefined;
    this.dropspotWrapper = undefined;

    this.pageSurface = undefined;
    this.focusedSurface = undefined;

    this.keysPressed = [];

    //-----PUBLIC METHODS-----
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
     * Enables pierce-through mode on the event (see manager for details)
     *
     * @param event
     */
    this.setPierceThrough = function(event)
    {
        //Note: we need to set it in the originalEvent or it won't end up in the Menu's generic document.on() listener
        event.originalEvent.data = event.originalEvent.data || {};
        event.originalEvent.data[this.PIERCE_THROUGH_DATA] = true;

        //for chaining
        return event;
    };

    //-----PRIVATE METHODS-----

}]);
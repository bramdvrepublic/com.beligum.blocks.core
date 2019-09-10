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
 * Plugin to broadcast messages across the system, built using jQuery's trigger() method
 * and created to solve the cyclic dependency problem.
 */
base.plugin("blocks.core.Broadcaster", [function ()
{
    var Broadcaster = this;

    //-----CONSTANTS-----
    this.EVENTS = {

        BLOCKS: {

            /**
             * sent out when the blocks system needs to boot
             * data: {}
             */
            START: "BLOCKS_START",

            /**
             * sent out when the blocks system has finished booting
             * data: {}
             */
            STARTED: "BLOCKS_STARTED",

            /**
             * sent out when the blocks system needs to be shut down
             * data: {}
             */
            STOP: "BLOCKS_STOP",

            /**
             * sent out when the blocks system has finished shutting down
             * data: {}
             */
            STOPPED: "BLOCKS_STOPPED",

            /**
             * sent out when the blocks editor system needs to be paused temporarily (used during saving, dialogs, resizing, etc)
             * data: {}
             */
            PAUSE: "BLOCKS_PAUSE",

            /**
             * sent out when the blocks editor system was paused
             * data: {}
             */
            PAUSED: "BLOCKS_PAUSED",

            /**
             * sent out when the blocks editor system needs to be un-pauzed
             * data: {}
             */
            RESUME: "BLOCKS_RESUME",

            /**
             * sent out when the blocks editor system was resumed
             * data: {}
             */
            RESUMED: "BLOCKS_RESUMED",

        },

        MOUSE: {

            /**
             * custom general click event that happens when the user clicked,
             * but may have slipped the mouse a few pixels between mouse down and up
             * data: {
             *     surface: the surface we clicked on
             *     element: the low-level DOM element we clicked on
             *     originalEvent: the original mouse down event
             * }
             */
            CLICK: "MOUSE_CLICK",

            DRAG: {

                /**
                 * the user exceeded the minimum threshold and started dragging the mouse
                 * data: {
                 *     surface: this is the surface we're dragging around
                 *     element: this is the DOM element we started our drag on
                 *     event: this is the original mousedown event that started the drag
                 * }
                 */
                START: "MOUSE_DRAG_START",

                /**
                 * the user moved the mouse inside a dragging session
                 * data: {
                 *     surface: this is the surface we started the drag on
                 *     element: this is the DOM element we started our drag on
                 *     originalEvent: this is the original mousedown event that started the drag
                 *     prevHoveredSurface: this is the surface we previously hovered on
                 *     hoveredSurface: this is the surface we're currently hovering on
                 *     dragVector: the current dragvector (x1,y1,x2,y2)
                 *     dragStats: the statistics of the dragvector (variance, direction, speed)
                 * }
                 */
                MOVE: "MOUSE_DRAG_MOVE",

                /**
                 * the user released the mouse after a dragging session
                 * data: {
                 *     surface: this is the surface we dragged around
                 *     element: the low-level DOM element we ended our drag on
                 *     originalEvent: the original mouse down event
                 *     hoveredSurface: the last surface we were hovering on
                 * }
                 */
                STOP: "MOUSE_DRAG_STOP",

                /**
                 * an active dragging session needs to be cancelled
                 * data: {
                 *     surface: this is the surface we dragged around (possibly undefined)
                 *     element: the low-level DOM element we ended our drag on
                 *     originalEvent: the original mouse down event
                 *     hoveredSurface: the last surface we were hovering on
                 * }
                 */
                ABORT: "MOUSE_DRAG_ABORT",
            }
        },

        UNDO: {

            /**
             * Sent out when an undo stack frame is recorded
             * data: {}
             */
            RECORDED: "UNDO_RECORDED",

            /**
             * Sent out when an undo action has been executed
             * data: {}
             */
            PERFORMED: "UNDO_PERFORMED",

            /**
             * Sent out when an redo action has been executed
             * data: {}
             */
            REDO: "UNDO_REDO",
        },

        PAGE: {

            /**
             * the current page model needs to be refreshed
             * data: {
             *     force: defaults to true, but can be set to false
             * }
             */
            REFRESH: "PAGE_REFRESH",

            /**
             * change the speed at which to refresh the page continuously
             * data: {
             *     speed: new value in millis for the timeout
             * }
             */
            REFRESH_SPEED: "PAGE_REFRESH_SPEED",

            /**
             * the current page model and overlays needs to be torn down and rebuilt entirely
             * data: {}
             */
            RELOAD: "PAGE_RELOAD",

            /**
             * the current page needs to be saved
             * data: {}
             */
            SAVE: "PAGE_SAVE",

            /**
             * the current page needs to be deleted
             * data: {}
             */
            DELETE: "PAGE_DELETE",

            CHANGED: {

                /**
                 * sent out by the manager when the html of the page changed because of a:
                 * - block was added
                 * - block was moved
                 * - block was removed
                 * - column was resized
                 *
                 * data: {
                 *     surface: the page-surface
                 *     oldValue: the old inner html of the page, before the change
                 * }
                 */
                HTML: "PAGE_CHANGED_HTML",

                /**
                 * sent out by blocks-imports-page when the type of the page changed or was initialized.
                 *
                 * data: {
                 *     oldValue: a data structure containing the old metadata about the page type (see server side RdfClass interface for a list of properties, notably 'curie')
                 *     newValue: a data structure containing the new metadata about the page type (see server side RdfClass interface for a list of properties, notably 'curie')
                 * }
                 */
                TYPE: "PAGE_CHANGED_TYPE",

                /**
                 * sent out by the manager when the cached data about the state of the page changed.
                 *
                 * the accompanying data object is the "pageCachedData" variable of the manager
                 */
                CACHED_DATA: "PAGE_CHANGED_CACHED_DATA",
            },

        },

        BLOCK: {
            /**
             * a block needs to receive focus
             * data: {
             *     surface: the block-surface that needs to receive focus or null if we want the page to get it (and blur any focused blocks)
             *     element: this is the specific 'deep' html element at this mouse position that was clicked
             * }
             */
            FOCUS: "BLOCK_FOCUS",

            /**
             * a block needs to be deleted
             * data: {
             *     surface: the block-surface that needs to be deleted
             * }
             */
            DELETE: "BLOCK_DELETE",

            /**
             * a new block needs to be auto-added to the page
             * data: {
             *     name: the name of the block type that needs to be created
             * }
             */
            CREATE: "BLOCK_CREATE",

            /**
             * a new block was added to the page
             * data: {
             *     surface: the block-surface that was created
             * }
             */
            CREATED: "BLOCK_CREATED",

            /**
             * a block was moved to a new location on the page
             * data: {
             *     surface: the block-surface that was moved
             * }
             */
            MOVED: "BLOCK_MOVED",

            /**
             * a block was deleted from the page
             * data: {
             *     surface: the block-surface that was deleted
             * }
             */
            DELETED: "BLOCK_DELETED",


            // group of events that are sent out when this block was changed
            CHANGED: {

                /**
                 * the inner html of this block changed
                 * data: {
                 *     surface: the block-surface that was changed
                 *     element: the jQuery element on which inner html was changed
                 *     oldValue: the old value of the inner html
                 *     configElement: the jQuery sidebar widget to call .val().change() on if we undo/redo this action
                 *     configOldValue: a value, a callback function or null (to use the oldValue) that will be supplied to the config's .val() callback on undo
                 *     configNewValue: a value, a callback function or null (to use the newValue) that will be supplied to the config's .val() callback on redo
                 *     listener: the listener callback that is called on each undo and redo
                 * }
                 */
                HTML: "BLOCK_CHANGED_HTML",

                /**
                 * an attribute of this block changed
                 * data: {
                 *     surface: the block-surface that was changed
                 *     element: the jQuery element on which the attribute was changed
                 *     attribute: the name of the attribute
                 *     oldValue: the old value of the attribute or null if it didn't exist before this call
                 *     configElement: the jQuery sidebar widget to call .val().change() on if we undo/redo this action
                 *     configOldValue: a value, a callback function or null (to use the oldValue) that will be supplied to the config's .val() callback on undo
                 *     configNewValue: a value, a callback function or null (to use the newValue) that will be supplied to the config's .val() callback on redo
                 *     listener: the listener callback that is called on each undo and redo
                 * }
                 */
                ATTRIBUTE: "BLOCK_CHANGED_ATTRIBUTE",
            },
        },

        FINDER: {
            /**
             * sent out when we want to load the finder in a container-element,
             * passed to the event using the 'container' option.
             * When done, the 'callback' option is called (if specified)
             * data: {
             *     container: the container element in which to put the loaded finder html
             *     options: the options that will passed as-is to the finder JS
             *     callback: function(success) that will be called when all is loaded successfully or not
             * }
             */
            LOAD: "FINDER_LOAD",
        },
    };

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /*
     * This function sends an event and automatically creates a custom event with parameters.
     * It allows us to solve the cyclic-dependency problem between the modules.
     * The events are triggered on the document
     */
    this.send = function (eventName, originalEvent, data)
    {
        if (eventName) {

            var newEvent;

            // we allow the user to reUse the original even that triggered this event (eg. to re-use the mouse coordinates)
            // but we'll manually re-set the type so it's a new event
            if (originalEvent) {

                newEvent = $.Event(originalEvent, {
                    type: eventName
                });
            }
            else {
                newEvent = $.Event(eventName);
            }

            // send the event with jquery
            $(document).triggerHandler(newEvent, data);
        }
        else {
            Logger.error('Trying to broadcast unknown event, ignoring');
        }
    };

}]);
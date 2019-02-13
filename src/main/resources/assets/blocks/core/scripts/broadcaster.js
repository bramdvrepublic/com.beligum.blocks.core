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
            //sent out when the blocks system needs to boot
            START: "BLOCKS_START",
            //sent out when the blocks system has finished booting
            STARTED: "BLOCKS_STARTED",
            //sent out when the blocks system needs to be shut down
            STOP: "BLOCKS_STOP",
            //sent out when the blocks system has finished shutting down
            STOPPED: "BLOCKS_STOPPED",
            //sent out when the blocks editor system needs to be paused temporarily (used during saving, dialogs, resizing, etc)
            PAUSE: "BLOCKS_PAUSE",
            //sent out when the blocks editor system was paused
            PAUSED: "BLOCKS_PAUSED",
            //sent out when the blocks editor system needs to be un-pauzed
            RESUME: "BLOCKS_RESUME",
            //sent out when the blocks editor system was resumed
            RESUMED: "BLOCKS_RESUMED",
        },

        MOUSE: {
            // custom general click event that happens when the user clicked,
            // but may have slipped the mouse a few pixels between mouse down and up
            CLICK: "MOUSE_CLICK",
            DRAG: {
                // the user exceeded the minimum threshold and started dragging the mouse
                START: "MOUSE_DRAG_START",
                // the user moved the mouse inside a dragging session
                MOVE: "MOUSE_DRAG_MOVE",
                // the user released the mouse after a dragging session
                STOP: "MOUSE_DRAG_STOP",
                // an active dragging session needs to be cancelled
                ABORT: "MOUSE_DRAG_ABORT",
            }
        },

        UNDO: {
            // Sent out when an undo stack frame is recorded
            RECORDED: "UNDO_RECORDED",
            // Sent out when an undo action has been executed
            PERFORMED: "UNDO_PERFORMED",
            // Sent out when an redo action has been executed
            REDO: "UNDO_REDO",
        },

        PAGE: {
            //the current page model needs to be refreshed
            REFRESH: "PAGE_REFRESH",
            //the speed at which to refresh the page continuously
            REFRESH_SPEED: "PAGE_REFRESH_SPEED",
            //the current page model and overlays needs to be rebuilt entirely
            RELOAD: "PAGE_RELOAD",
            //sent out by the manager when the layout of the page was changed
            // because of a:
            // - block was added
            // - block was moved
            // - blocks was removed
            // - column was resized
            CHANGED: "PAGE_CHANGED",
            //the current page needs to be saved
            SAVE: "PAGE_SAVE",
            //the current page needs to be deleted
            DELETE: "PAGE_DELETE",
        },

        BLOCK: {
            //a block needs to receive focus
            FOCUS: "BLOCK_FOCUS",
            //a block needs to be deleted
            DELETE: "BLOCK_DELETE",
        },

        FINDER: {
            // sent out when we want to load the finder in a container-element,
            // passed to the event using the 'container' option.
            // When done, the 'callback' option is called (if specified)
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
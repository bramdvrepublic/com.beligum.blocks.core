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
 *
 * Plugin to broadcast messages
 *
 * Register to receive a message for an event by name, and the callback to be called when a message is received.
 * Different callbacks can be coupled to the same event. So when you register twice for the same event,
 * both callbacks will be called when a message is send.
 * e.g.: Broadcaster.on("doLookup", lookup)
 *
 * You can use namespaces by writing eventname.namespace.
 * Namespaces can be used to only unregister some registered
 * events with the same name, or to only send messages to events in the same namespace
 *
 * e.g.: Broadcaster.on("doLookup.mynamespace", lookup)
 *
 * Unregister with off. Once unregistered you will no longer receive messages.
 * When unregistering without a namespace, all registered callbacks for an event will be removed.
 * otherwise, only the callbacks for that namespace and event will be removed.
 * e.g.: Broadcaster.off("doLookup", lookup)
 *
 * You can send an event. If needed you can add 1 parameter:
 * Broadcaster.send("lookup", parameter);
 *
 * When using a namespace when sending an event, only events for that namespace will be called.
 * If needed you can add a timeout value when sending an event. Then all events will be called
 * with that timeout value. Default is 0, so all registered callbacks will be called without waiting
 * for each other (async). When you give a timeout value < 0 then the next registered callback will
 * be called when the previous callback finished synchronously.
 *
 * Using Broadcaster.send("lookup") has the same effect as Broadcaster.send("lookup", null, -1);
 */
base.plugin("blocks.core.Broadcaster", [function ()
{
    var Broadcaster = this;

    //-----CONSTANTS-----
    this.EVENTS = {

        BLOCKS: {
            //sent out when the edit page button was clicked and the sidebar has completed opening
            START: "BLOCKS_START",
            //sent out when the sidebar was closed and has completed shutdown
            STOP: "BLOCKS_STOP",
            //sent out when the blocks editor system needs to be paused temporarily (used during saving, dialogs, resizing, etc)
            PAUSE: "BLOCKS_PAUSE",
            //sent out when the blocks editor system needs to be un-pauzed
            RESUME: "BLOCKS_RESUME",
        },

        MOUSE: {
            // custom general click event that happens when the user clicked,
            // but may have slipped the mouse a few pixels between mouse down and up
            CLICK: "MOUSE_CLICK",
            // the user exceeded the minimum threshold and started dragging the mouse
            DRAG_START: "MOUSE_DRAG_START",
            // the user moved the mouse inside a dragging session
            DRAG_MOVE: "MOUSE_DRAG_MOVE",
            // the user released the mouse after a dragging session
            DRAG_STOP: "MOUSE_DRAG_STOP",
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
            //the current page needs to be saved
            SAVE: "PAGE_SAVE",
            //the current page needs to be deleted
            DELETE: "PAGE_DELETE",
        },

        BLOCK: {
            //a block needs to be deleted
            DELETE: "BLOCK_DELETE",
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
            var e;

            // we allow the user to reUse the original even that triggered this event (eg. to re-use the mouse coordinates)
            // but we'll manually re-set the type so it's a new event
            if (originalEvent) {

                e = $.Event(originalEvent, {
                    type: eventName
                });
            }
            else {
                e = $.Event(eventName);
            }

            // send the event with jquery
            $(document).triggerHandler(e, data);
        }
        else {
            Logger.error('Trying to broadcast unknown event, ignoring');
        }
    };

}]);
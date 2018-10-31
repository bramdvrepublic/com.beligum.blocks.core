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

/*plugin to broadcast messages
 *
 * register to receive a message for an event by name, and the callback to be called when a message is received.
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
 *You can send an event. If needed you can add 1 parameter:
 * Broadcaster.send("lookup", parameter);
 *
 * When using a namespace when sending an event, only events for that namespace will be called.
 * If needed you can add a timeout value when sending an event. Then all events will be called
 * with that timeout value. Default is 0, so all registered callbacks will be called without waiting
 * for each other (async). When you give a timeout value < 0 then the next registered callback will
 * be called when the previous callback finished. (synnchronous).
 * Using Broadcaster.send("lookup") has the same effect as Broadcaster.send("lookup", null, -1);
 *
 * */

base.plugin("blocks.core.Broadcaster", ["constants.base.core.internal", function (Constants)
{
    var Broadcaster = this;

    this.active = false;

    /*
     * This function sends an event and automatically creates a blockevent with all current parameters
     * Gives us the location on the page for the mouse, the smallest block we hover over (and the previous one),
     * the property we hover over (and the previous one)
     * custom is the paramter used with the send function. The event is triggered on the document
     */
    this.send = function (eventName, originalEvent, data)
    {
        if (eventName == Broadcaster.EVENTS.START_BLOCKS || eventName == Broadcaster.EVENTS.PRE_START_BLOCKS) {
            Broadcaster.active = true;
        }

        if (Broadcaster.active) {
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

        if (eventName == Broadcaster.EVENTS.STOP_BLOCKS) {
            Broadcaster.active = false;
        }
    };

    this.EVENTS = {

        //sent out when the edit page button was clicked, but the sidebar hasn't completed opening
        PRE_START_BLOCKS: "PRE_START_BLOCKS",
        //sent out when the edit page button was clicked, and the sidebar has completed opening
        START_BLOCKS: "START_BLOCKS",
        //sent out when the sidebar was closed, but hasn't completed shutdown
        PRE_STOP_BLOCKS: "PRE_STOP_BLOCKS",
        //sent out when the sidebar was closed, and has completed shutdown
        STOP_BLOCKS: "STOP_BLOCKS",

        // Events with blockEvent as argument
        START_DRAG: "START_DRAG",
        END_DRAG: "END_DRAG",
        ABORT_DRAG: "ABORT_DRAG",
        DRAG_OVER_BLOCK: "DRAG_OVER_BLOCK",
        FOCUS_BLOCK: "FOCUS_BLOCK",

        // Called when the mouse pointer enters/leaves a visible LayoutElement
        HOVER_ENTER_OVERLAY: "HOVER_ENTER_OVERLAY",
        HOVER_LEAVE_OVERLAY: "HOVER_LEAVE_OVERLAY",

        ENABLE_DND: "ALLOW_DRAG",
        DISABLE_DND: "DISABLE_DND",

        // This (de)activates the mouse cf pauzes the templates layouter (used during dialogs, resizing, etc)
        ACTIVATE_MOUSE: "ACTIVATE_MOUSE",
        DEACTIVATE_MOUSE: "DEACTIVATE_MOUSE",

        // Events send when the the templates DOM tree in memory will/is/did rebuild
        DO_REFRESH_LAYOUT: "DO_REFRESH_LAYOUT",
        WILL_REFRESH_LAYOUT: "WILL_REFRESH_LAYOUT",
        DID_REFRESH_LAYOUT: "DID_REFRESH_LAYOUT",

        // Announce that we changed someting to the dom (add/remove/resize elements)
        // this will automatically trigger DO_REFRESH_LAYOUT
        DOM_CHANGED: "DOM_CHANGED",

        // Sent out when an undo stack frame is recorded
        UNDO_RECORDED: "UNDO_RECORDED",
        // Sent out when an undo action has been executed
        UNDO_PERFORMED: "UNDO_PERFORMED",
        // Sent out when an redo action has been executed
        REDO_PERFORMED: "REDO_PERFORMED",
    };

}]);
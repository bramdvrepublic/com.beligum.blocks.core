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

base.plugin("blocks.core.Broadcaster", ["base.core.Constants", "blocks.core.DomManipulation", function (Constants, DOM)
{
    var Broadcaster = this;

    this.active = false;
    var fields = {current: null, previous: null};
    var hoveredBlocks = {current: null, previous: null};
    var properties = {current: null, previous: null};

    var layoutTree = null;

    this.block = function ()
    {
        return hoveredBlocks;
    };

    this.property = function ()
    {
        return properties;
    };

    /*
     * Resets all parameters for the hovering.
     * This means that the next mousemove will trigger a HOVER_ENTER_EVENT
     * */
    this.resetHover = function ()
    {
        fields = {current: null, previous: null};
        hoveredBlocks = {current: null, previous: null};
        properties = {current: null, previous: null};

        //now moved to mouse.js, needed there?
        //lastMoveEvent.block = hoveredBlocks;
        //lastMoveEvent.property = properties;
    };

    this.setHoveredBlock = function (layoutElement)
    {
        hoveredBlocks.previous = hoveredBlocks.current;
        hoveredBlocks.current = layoutElement;
    };
    this.getHoveredBlock = function ()
    {
        return hoveredBlocks;
    };

    /*
     * sets the current hovered block based on the mouse coordinates
     * */
    this.getHoveredBlockForPosition = function (x, y)
    {
        var currentField = fields.current;
        fields.current = null;

        var currentBlock = hoveredBlocks.current;
        hoveredBlocks.current = null;

        var currentProperty = properties.current;
        properties.current = null;

        // First search for active element
        // If an element is active, we have a big chance the next event is in the same element, so we start our search here
        if (currentField != null) {
            var bb = currentField.findActiveElement(x, y);
            if (bb instanceof blocks.elements.Block || bb instanceof blocks.elements.Property) {
                //Logger.debug("staying in same block");
                fields.current = bb;
            }
        }

        // Our shortcut failed so search the full page
        // we loop the trees of elements to find the smallest active element
        if (fields.current == null) {
            if (Broadcaster.getContainer() != null) {
                var bb = Broadcaster.getContainer().findActiveElement(x, y);
                if (bb != null && (bb instanceof blocks.elements.Block || bb instanceof blocks.elements.Property)) {
                    fields.current = bb;
                } else if (bb != null) {
                    // loop back until we find null, block or field
                    while (bb != null && !(bb instanceof blocks.elements.Block || bb instanceof blocks.elements.Property)) {
                        bb = bb.parent;
                    }
                    fields.current = bb;
                }
            }
        }

        if (fields.current != currentField) {
            fields.previous = currentField;
        }

        if (fields.current != null) {
            if (fields.current instanceof blocks.elements.Property) {
                properties.current = fields.current;
                hoveredBlocks.current = fields.current;
                while (hoveredBlocks.current != null && !(hoveredBlocks.current instanceof blocks.elements.Block)) {
                    hoveredBlocks.current = hoveredBlocks.current.parent;
                }
            } else {
                hoveredBlocks.current = fields.current;
                properties.current = null;
            }
        }

        if (hoveredBlocks.current != currentBlock) {
            hoveredBlocks.previous = currentBlock;
        }

        if (properties.current != currentProperty) {
            properties.previous = currentProperty;
        }

        return hoveredBlocks;
    };

    /*
     * This function sends an event and automatically creates a blockevent with all current parameters
     * Gives us the location on the page for the mouse, the smallest block we hover over (and the previous one),
     * the property we hover over (and the previous one)
     * custom is the paramter used with the send function. The event is triggered on the document
     */
    this.send = function (eventName, originalEvent, data)
    {
        if (eventName == Broadcaster.EVENTS.START_BLOCKS) {
            Broadcaster.active = true;
        }

        if (Broadcaster.active) {
            var e;
            // we allow the user to reUse the original even that triggered this event (eg. to re-use the mouse coordinates)
            // but we'll manually re-set the type so it's a new event
            if (originalEvent) {
                e = $.Event(originalEvent);
                e.type = eventName;
            }
            else {
                e = $.Event(eventName);
            }

            //all of this is moved to mouse.js, needed?
            //e.target = lastMoveEvent.target;
            //e.pageX = lastMoveEvent.pageX;
            //e.pageY = lastMoveEvent.pageY;
            //e.clientX = lastMoveEvent.clientX;
            //e.clientY = lastMoveEvent.clientY;
            //e.direction = lastMoveEvent.direction;

            if (data) {
                e.data = data;
            }

            //still needed?
            e.block = hoveredBlocks;
            e.property = properties;

            // send the event with jquery
            $(document).triggerHandler(e);
        }

        if (eventName == Broadcaster.EVENTS.STOP_BLOCKS) {
            Broadcaster.active = false;
        }
    };

    /*
     * Container is the block IN which we are dragging.
     * If we set this to null then then the top level block(s) are the container
     * */
    this.setContainer = function (value)
    {
        layoutTree = value
    };
    this.getContainer = function ()
    {
        return layoutTree;
    };

    /*
     We create some sort of a heat map. We define boxes for all draggable templates
     we can add left and right from each column
     and left and right from container if container has more than 1 row
     select each row and add bottom
     if row has +1 colunms, we can add also to bottom of columns
     except if column has +1 rows
     */

    var oldLayoutTree = null;
    var oldContainerParent = null;

    this.buildLayoutTree = function ()
    {
        oldLayoutTree = null;
        oldContainerParent = null;
        layoutTree = new blocks.elements.Page();
        Broadcaster.resetHover();

        //this is moved to mouse.js
        //lastMoveEvent.block = Broadcaster.getHoveredBlockForPosition(lastMoveEvent.pageX, lastMoveEvent.pageY);
        //lastMoveEvent.block = Broadcaster.getHoveredBlock();
    };

    this.EVENTS = {};
    // Events with callback

    // This enables/disables the drag functionality. But all mouse ecvents are still send
    // This way you can implement your own drag without diabling the whole templates event system
    this.EVENTS.ENABLE_BLOCK_DRAG = "ENABLE_DRAG";
    this.EVENTS.DISABLE_BLOCK_DRAG = "DISABLE_DRAG";

    // Events with blockEvent as argument
    this.EVENTS.START_DRAG = "START_DRAG";
    this.EVENTS.END_DRAG = "END_DRAG";
    this.EVENTS.ABORT_DRAG = "ABORT_DRAG";
    this.EVENTS.DRAG_LEAVE_BLOCK = "DRAG_LEAVE_BLOCK";
    this.EVENTS.DRAG_ENTER_BLOCK = "DRAG_ENTER_BLOCK";
    this.EVENTS.DRAG_OVER_BLOCK = "DRAG_OVER_BLOCK";

    this.EVENTS.BLOCKS_CLICK = "BLOCKS_CLICK";

    // Send when clicked on a property
    // is like focus/blur on a text input
    // Allows plugins to hook on
    this.EVENTS.START_EDIT_FIELD = "START_EDIT_FIELD";
    this.EVENTS.END_EDIT_FIELD = "END_EDIT_FIELDS";

    this.EVENTS.DISABLE_SELECTION = "DISABLE_SELECTION";

    // Notifications

    // Called when the mouse pointer enters/leaves a visible LayoutElement
    this.EVENTS.HOVER_ENTER_OVERLAY = "HOVER_ENTER_OVERLAY";
    this.EVENTS.HOVER_LEAVE_OVERLAY = "HOVER_LEAVE_OVERLAY";

    this.EVENTS.ENABLE_DND = "ALLOW_DRAG";
    this.EVENTS.DISABLE_DND = "DISABLE_DND";
    // This (de)activates the mouse cf pauzes the templates layouter (used during dialogs, resizing, etc)
    this.EVENTS.ACTIVATE_MOUSE = "ACTIVATE_MOUSE";
    this.EVENTS.DEACTIVATE_MOUSE = "DEACTIVATE_MOUSE";
    // Events send when the the templates DOM tree in memory will/is/did rebuild
    this.EVENTS.DO_REFRESH_LAYOUT = "DO_REFRESH_LAYOUT";
    this.EVENTS.WILL_REFRESH_LAYOUT = "WILL_REFRESH_LAYOUT";
    this.EVENTS.DID_REFRESH_LAYOUT = "DID_REFRESH_LAYOUT";

    // Announce that we changed someting to the dom (add/remove/resize elements)
    // this will automatically trigger DO_REFRESH_LAYOUT
    this.EVENTS.DOM_CHANGED = "DOM_CHANGED";

    // on/off
    this.EVENTS.STOP_BLOCKS = "STOP_BLOCKS";
    this.EVENTS.START_BLOCKS = "START_BLOCKS";
    this.EVENTS.WILL_SAVE = "WILL_SAVE";
    this.EVENTS.DID_SAVE = "DID_SAVE";

}]);
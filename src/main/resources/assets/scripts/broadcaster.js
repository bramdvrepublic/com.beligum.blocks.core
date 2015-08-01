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
    var directionVector = {x1: 0, y1: 0, x2: 0, y2: 0};

    var layoutTree = null;
    var lastMoveEvent = $.Event("mousemove", {pageX: 0, pageY: 0});

    this.getLastMove = function ()
    {
        return lastMoveEvent;
    }


    this.registerMouseMove = function ()
    {
        $(document).on("mousemove.blocks_broadcaster", function (event)
        {
            var direction = calculateDirection(event);
            lastMoveEvent = event;
            lastMoveEvent.block = Broadcaster.getHoveredBlockForPosition(lastMoveEvent.pageX, lastMoveEvent.pageY);
            lastMoveEvent.direction = direction;
        });
    };

    this.unregisterMouseMove = function ()
    {
        $(document).off("mousemove.blocks_broadcaster");
    };


    this.block = function ()
    {
        return hoveredBlocks;
    };

    this.property = function ()
    {
        return properties;
    };

    // http://stackoverflow.com/questions/9043805/test-if-two-lines-intersect-javascript-function
    function intersects(a, b, c, d, p, q, r, s)
    {
        var det, gamma, lambda;
        det = (c - a) * (s - q) - (r - p) * (d - b);
        if (det === 0) {
            return false;
        } else {
            lambda = ((s - q) * (r - a) + (p - r) * (s - b)) / det;
            gamma = ((b - d) * (r - a) + (c - a) * (s - b)) / det;
            return (0 < lambda && lambda < 1) && (0 < gamma && gamma < 1);
        }
    };
    // https://gist.github.com/Joncom/e8e8d18ebe7fe55c3894
    function line_intersects(p0_x, p0_y, p1_x, p1_y, p2_x, p2_y, p3_x, p3_y) {

        var s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;
        s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;
        s2_y = p3_y - p2_y;

        var s, t;
        s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
        {
            // Collision detected
            return 1;
        }

        return 0; // No collision
    };

    this.mouseDirectionForBlock = function (block)
    {
        if (line_intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.top, block.right, block.top)) {
            return Constants.DIRECTION.UP;
        } else if (line_intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.bottom, block.right, block.bottom)) {
            return Constants.DIRECTION.DOWN;
        } else if (line_intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.top, block.left, block.bottom)) {
            return Constants.DIRECTION.LEFT;
        } else if (line_intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.right, block.top, block.right, block.bottom)) {
            return Constants.DIRECTION.RIGHT;
        } else {
            return Constants.DIRECTION.NONE;
        }
    }

    // returns the current mouse direction
    var old_direction = {x: false, y: false};
    var prevX = -1;
    var prevY = -1;
    var distance = 0;
    var direction = 0;
    var sins = [];
    var coss = [];
    var lengths = [];
    var times = [];
    var index = 0;
    var limit = 20;
    var variance = 0;
    var prevTime = new Date().getTime();

    function updateDistanceAndDirection(curX, curY)
    {
        var angle = Math.atan2(prevY - curY, prevX - curX);
        sins[index] = Math.sin(angle);
        coss[index] = Math.cos(angle);
        lengths[index] = Math.sqrt((curX - prevX) * (curX - prevX) + (curY - prevY) * (curY - prevY));
        var time = new Date().getTime();
        times[index] = time - prevTime;

        variance = 1.0 - Math.sqrt(sum(coss) * sum(coss) + sum(sins) * sum(sins)) / sins.length;

        direction = Math.atan2(1 / sins.length * sum(sins), 1 / coss.length * sum(coss));
        var speed = sum(lengths) / (sum(times) / 200);
        distance = Math.min(Math.max(40, speed), 100);
        prevTime = time;
        index = (index + 1) % limit;
        prevX = curX;
        prevY = curY;
    }

    var sum = function (array)
    {
        var s = 0.0;
        for (var i = 0; i < array.length; i++) {
            s += array[i];
        }
        return s;
    };

    /*
     * Gives the current direction of the mouse in degrees
     * */
    var calculateDirection = function (event)
    {
        updateDistanceAndDirection(event.pageX, event.pageY);
        directionVector.x1 = event.pageX;
        directionVector.y1 = event.pageY;
        if (variance < 0.2) {

            var cos = (Math.cos(direction) * 10000);
            var sin = (Math.sin(direction) * 10000);
            directionVector.x2 = directionVector.x1 - cos;
            directionVector.y2 = directionVector.y1 - sin;
        }
//        Logger.debug(directionVector.x1 + ", "+directionVector.y1+ " - " +directionVector.x2 +", "+ directionVector.y2);
        var angle = direction * (180 / Math.PI);
//        Logger.debug("Hoek: " + angle + " - variance: " + variance);
        return direction;
    };

    /*
     * Rests all parameters for the hovering.
     * This means that the next mousemove will trigger a HOVER_ENTER_EVENT
     * */
    this.resetHover = function ()
    {
        fields = {current: null, previous: null};
        hoveredBlocks = {current: null, previous: null};
        properties = {current: null, previous: null};
        lastMoveEvent.block = hoveredBlocks;
        lastMoveEvent.property = properties;

    };

    /*
     * sets the current hovered block based on teh mouse coordinates
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


    this.createEvent = function(eventName, custom) {
        //Logger.debug(eventName);
        var e = $.Event(eventName);
        e.target = lastMoveEvent.target;
        e.pageX = lastMoveEvent.pageX;
        e.pageY = lastMoveEvent.pageY;
        e.clientX = lastMoveEvent.clientX;
        e.clientY = lastMoveEvent.clientY;
        e.direction = lastMoveEvent.direction;
        e.block = hoveredBlocks;
        e.property = properties;
        e.custom = custom;
        return e;
    }

    /*
     * This function sends an event and automatically creates a blockevent with all current paramaters
     * GIves us the location on the page for the mouse, the smallest block we hover over (and the previous one),
     * the property we hover over (and the previous one)
     * custom is the paramter used with the send function. The event is triggered on the document
     * */
    this.send = function (eventName, custom)
    {
        if (eventName == Broadcaster.EVENTS.START_BLOCKS) {
            Broadcaster.active = true;
        }

        if (Broadcaster.active) {
            var e = Broadcaster.createEvent(eventName, custom);
            // send the event with jquery
            $(document).triggerHandler(e);
        }
        if (eventName == Broadcaster.EVENTS.STOP_BLOCKS) {
            Broadcaster.active = false;
        }
    };

    this.sendToElement = function (element, eventName, custom)
    {
        if (Broadcaster.active) {
            var e = $.Event(eventName);
            e.pageX = lastMoveEvent.pageX;
            e.pageY = lastMoveEvent.pageY;
            e.direction = lastMoveEvent.direction;
            e.block = hoveredBlocks;
            e.property = properties;
            e.custom = custom;
            // send the event with jquery
            element.triggerHandler(e);
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
        lastMoveEvent.block = Broadcaster.getHoveredBlockForPosition(lastMoveEvent.pageX, lastMoveEvent.pageY);
    };

    this.EVENTS = {};
    // EVents with callback

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
    this.EVENTS.HOVER_LEAVE_BLOCK = "HOVER_LEAVE_BLOCK";
    this.EVENTS.HOVER_ENTER_BLOCK = "HOVER_ENTER_BLOCK";
    this.EVENTS.HOVER_OVER_BLOCK = "HOVER_OVER_BLOCK";

    this.EVENTS.HOVER_LEAVE_PROPERTY = "HOVER_LEAVE_PROPERTY";
    this.EVENTS.HOVER_ENTER_PROPERTY = "HOVER_ENTER_PROPERTY";
    this.EVENTS.HOVER_OVER_PROPERTY = "HOVER_OVER_PROPERTY";

    this.EVENTS.END_HOVER = "END_HOVER";
    this.EVENTS.BLOCKS_CLICK = "BLOCKS_CLICK";

    // Send when clicked on a property
    // is like focus/blur on a text input
    // Allows plugins to hook on
    this.EVENTS.START_EDIT_FIELD = "START_EDIT_FIELD";
    this.EVENTS.END_EDIT_FIELD = "END_EDIT_FIELDS";

    this.EVENTS.DISABLE_SELECTION = "DISABLE_SELECTION";

    // Notifications
    this.EVENTS.DO_ALLOW_DRAG = "ALLOW_DRAG";
    this.EVENTS.DO_NOT_ALLOW_DRAG = "DO_NOT_ALLOW_DRAG";
    // Thsi (de)activates the mouse cf pauzes the templates layouter (used during dialogs)
    this.EVENTS.ACTIVATE_MOUSE = "ACTIVATE_MOUSE";
    this.EVENTS.DEACTIVATE_MOUSE = "DEACTIVATE_MOUSE";
    // Events send when the the templates DOM tree in memory will/is/did rebuild
    this.EVENTS.DO_REFRESH_LAYOUT = "DO_REFRESH_LAYOUT";
    this.EVENTS.WILL_REFRESH_LAYOUT = "WILL_REFRESH_LAYOUT";
    this.EVENTS.DID_REFRESH_LAYOUT = "DID_REFRESH_LAYOUT";
    // Announce that we changed someting to the dom
    // this will automatically trigger DO_REFRESH_LAYOUT
    this.EVENTS.DOM_WILL_CHANGE = "DOM_WILL_CHANGE";
    this.EVENTS.DOM_DID_CHANGE = "DOM_DID_CHANGE";
    // on/off
    this.EVENTS.STOP_BLOCKS = "STOP_BLOCKS";
    this.EVENTS.START_BLOCKS = "START_BLOCKS";
    this.EVENTS.WILL_SAVE = "WILL_SAVE";
    this.EVENTS.DID_SAVE = "DID_SAVE";


}]);
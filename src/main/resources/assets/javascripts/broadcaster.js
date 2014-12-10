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
 * Using Broadcaster.sendNoTimeout("lookup") has the same effect as Broadcaster.send("lookup", null, -1);
 *
 * */
blocks.plugin("blocks.core.Broadcaster", ["blocks.core.Constants", "blocks.core.Elements", "blocks.core.DomManipulation", function (Constants, Elements, DOM) {
    var Broadcaster = this;
    var blocks = {current: null, previous: null};
    var mediumPoints = {x: 0, y:0, sum: {x: 0, y:0}, total:0, points: []};
    var layoutTree = null;
    var layoutParentElement = null;
    var lastMoveEvent = $.Event("mousemove", {pageX:0, pageY:0});

    $(document).on("mousemove.blocks_broadcaster", function (event) {
        lastMoveEvent = event;
        lastMoveEvent.block = Broadcaster.getHooveredBlockForPosition(lastMoveEvent.pageX, lastMoveEvent.pageY);
        lastMoveEvent.direction = calculateDirection(event);
    });

    this.block = function() {
        return blocks;
    }

    // returns the current mouse direction
    var calculateDirection = function(event) {
        var REMEMBER_NR_OF_POINTS = 60;
        var newPoint = {x: event.pageX, y: event.pageY};
        if (mediumPoints.total == 0) {
            mediumPoints.x = newPoint.x;
            mediumPoints.y = newPoint.y;
            mediumPoints.points.push(newPoint);
            mediumPoints.total = 1;
        }
        var deltaX = newPoint.x - mediumPoints.x;
        var deltaY = newPoint.y - mediumPoints.y;
        var retVal;
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (deltaX < 0) {
                retVal =  Constants.DIRECTION.LEFT;
            } else {
                retVal = Constants.DIRECTION.RIGHT;
            }
        } else if (Math.abs(deltaX) < Math.abs(deltaY)) {
            if (deltaY < 0) {
                retVal = Constants.DIRECTION.UP;
            } else {
                retVal = Constants.DIRECTION.DOWN;
            }
        } else {
            retVal = Constants.DIRECTION.NONE;
        }

        if (mediumPoints.total > REMEMBER_NR_OF_POINTS) {
            var p = mediumPoints.points.shift();
            mediumPoints.sum.x -= p.x;
            mediumPoints.sum.y -= p.y;
            mediumPoints.total -= 1;
        }
        mediumPoints.points.push(newPoint);
        mediumPoints.sum.x += newPoint.x;
        mediumPoints.sum.y += newPoint.y;
        mediumPoints.total += 1;

//        mediumPoints.x = (mediumPoints.sum.x / mediumPoints.total);
//        mediumPoints.y = (mediumPoints.sum.y / mediumPoints.total);
        mediumPoints.x = (mediumPoints.x + newPoint.x) / 2;
        mediumPoints.y = (mediumPoints.y + newPoint.y) / 2;

        return retVal;
    };

    // sets the current active block
    this.getHooveredBlockForPosition = function (x, y) {
        var currentBlock = blocks.current;
        blocks.current = null;
        // First search for active element
        // If an element is active, we have a big chance the next event is in the same element, so we start our search here
        if (currentBlock != null) {
            blocks.current = currentBlock.findActiveElement(x, y);
        }
        // Our shortcut failed so search the full page
        // we loop the trees of elements to find the smallest active element
        if (blocks.current == null) {
            var i = 0;
            while (i < Broadcaster.getLayoutTree().length && blocks.current == null) {
                blocks.current = Broadcaster.getLayoutTree()[i].findActiveElement(x, y);
                i++;
            }
        }

        if (blocks.current != currentBlock) {
            blocks.previous = currentBlock;
        }
        return blocks;
    };

    this.send = function (eventName, custom) {
        setTimeout(function() {Broadcaster.sendNoTimeout(eventName, custom)}, 0);
    };

    this.sendNoTimeout = function(eventName, custom) {
        var e = $.Event(eventName);
        e.pageX = lastMoveEvent.pageX;
        e.pageY = lastMoveEvent.pageY;
        e.direction = lastMoveEvent.direction;
        e.block = blocks;
        e.custom = custom;
        $(document).triggerHandler(e);
    };


    this.EVENTS = {};
    // EVents with callback

    // Owner is a string that identifies the owner of the request
    this.EVENTS.ENABLE_BLOCK_DRAG = "ENABLE_DRAG";
    this.EVENTS.DISABLE_BLOCK_DRAG = "DISABLE_DRAG";

    // Events with blockEvent as argument
    this.EVENTS.START_DRAG = "START_DRAG";
    this.EVENTS.END_DRAG = "END_DRAG";
    this.EVENTS.ABORT_DRAG = "ABORT_DRAG";
    this.EVENTS.DRAG_LEAVE_BLOCK = "DRAG_LEAVE_BLOCK";
    this.EVENTS.DRAG_ENTER_BLOCK = "DRAG_ENTER_BLOCK";
    this.EVENTS.DRAG_OVER_BLOCK = "DRAG_OVER_BLOCK";
    this.EVENTS.HOOVER_LEAVE_BLOCK = "HOOVER_LEAVE_BLOCK";
    this.EVENTS.HOOVER_ENTER_BLOCK = "HOOVER_ENTER_BLOCK";
    this.EVENTS.HOOVER_OVER_BLOCK = "HOOVER_OVER_BLOCK";
    this.EVENTS.END_HOOVER = "END_HOOVER";
    this.EVENTS.DOUBLE_CLICK_BLOCK = "DOUBLE_CLICK_BLOCK";

    // Notifications
    this.EVENTS.DO_ALLOW_DRAG = "ALLOW_DRAG";
    this.EVENTS.DO_NOT_ALLOW_DRAG = "DO_NOT_ALLOW_DRAG";
    this.EVENTS.ACTIVATE_MOUSE = "ACTIVATE_MOUSE";
    this.EVENTS.DEACTIVATE_MOUSE = "DEACTIVATE_MOUSE";
    this.EVENTS.DO_REFRESH_LAYOUT = "DO_REFRESH_LAYOUT";
    this.EVENTS.DID_REFRESH_LAYOUT = "DID_REFRESH_LAYOUT";
    this.EVENTS.DOM_WILL_CHANGE = "DOM_WILL_CHANGE";
    this.EVENTS.DOM_DID_CHANGE = "DOM_DID_CHANGE";




    // The parent element where the tree is build
    // if null this is automatically set to the container

    this.getLayoutTree = function() {
        if (layoutTree == null) {
            buildLayoutTree();
        }
        return layoutTree;
    };

    this.setLayoutParent = function(element) {
        layoutParentElement = element;
        buildLayoutTree();
    };

    /*
     We create some sort of a heat map. We define boxes for all draggable blocks
     we can add left and right from each column
     and left and right from container if container has more than 1 row
     select each row and add bottom
     if row has +1 colunms, we can add also to bottom of columns
     except if column has +1 rows
     */
    var buildLayoutTree = function () {
        blocks.previous = null;
        blocks.current = null;
        layoutTree = [];
        //_this.cleanLayout();
        if (layoutParentElement == null) {
            layoutParentElement = $("body").find(".can-layout");
        }

        var findContainersInParent = function(parent) {

            if (DOM.canLayout(parent) || DOM.canEdit(parent)) {
                var container = new Elements.Container(parent);
                Logger.debug(container);
                layoutTree.push(container);
            } else {
                var children = parent.children();
                for (var i = 0; i < children.length; i++) {
                    findContainersInParent($(children[i]));
                }
            }
//            if (parent.next() != null) {
//                findContainersInParent(parent.next());
//            }
        };
        findContainersInParent(layoutParentElement);
        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DID_REFRESH_LAYOUT);
    };

    $(document).ready(function() {
        buildLayoutTree();
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function() {
        buildLayoutTree();
    })

    $(document).on(Broadcaster.EVENTS.DOM_DID_CHANGE, function() {
        Broadcaster.sendNoTimeout(Broadcaster.EVENTS.DO_REFRESH_LAYOUT);
    });



    // On Boot
    $(window).on("resize.blocks_broadcaster", function () {
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT);
    });

}])
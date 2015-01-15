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

blocks.plugin("blocks.core.Broadcaster", ["blocks.core.Constants",  "blocks.core.DomManipulation", function (Constants, DOM) {
    var Broadcaster = this;
    var active = false;
    var hoveredBlocks = {current: null, previous: null};
    var properties = {current: null, previous: null};
    var directionVector = {x1: 0, y1: 0, x2: 0, y2: 0};
    var lastPoints = [];
    var resetDirectionHandler = null;
    var layoutTree = null;
    var layoutParentElement = null;
    var lastMoveEvent = $.Event("mousemove", {pageX:0, pageY:0});


    var registerMouseMove = function() {
        $(document).on("mousemove.blocks_broadcaster", function (event) {
            var direction = calculateDirection(event);
            lastMoveEvent = event;
            lastMoveEvent.block = Broadcaster.getHooveredBlockForPosition(lastMoveEvent.pageX, lastMoveEvent.pageY);
            lastMoveEvent.direction = direction;
        });
    }

    var unregisterMouseMove = function() {
        $(document).off("mousemove.blocks_broadcaster");
    }



    this.block = function() {
        return hoveredBlocks;
    };

    this.property = function() {
        return properties;
    };

    // http://stackoverflow.com/questions/9043805/test-if-two-lines-intersect-javascript-function
    function intersects(a,b,c,d,p,q,r,s) {
        var det, gamma, lambda;
        det = (c - a) * (s - q) - (r - p) * (d - b);
        if (det === 0) {
            return false;
        } else {
            lambda = ((s - q) * (r - a) + (p - r) * (s - b)) / det;
            gamma = ((b - d) * (r - a) + (c - a) * (s - b)) / det;
            return (0 < lambda && lambda < 1) && (0 < gamma && gamma < 1);
        }
    }



    this.mouseDirectionForBlock = function(block) {
        if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.top, block.right, block.top)) {
            return Constants.DIRECTION.UP;
        } else if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.bottom, block.right, block.bottom)) {
            return Constants.DIRECTION.DOWN;
        } else if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.top, block.left, block.bottom)) {
            return Constants.DIRECTION.LEFT;
        } else if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.right, block.top, block.right, block.bottom)) {
            return Constants.DIRECTION.RIGHT;
        } else {
            return Constants.DIRECTION.NONE;
        }
    }

    // returns the current mouse direction
    var old_direction = {x: false, y:false};
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
    function updateDistanceAndDirection(curX, curY){
        var angle = Math.atan2(prevY - curY, prevX - curX);
        sins[index] = Math.sin(angle);
        coss[index] = Math.cos(angle);
        lengths[index] = Math.sqrt((curX-prevX)*(curX-prevX) + (curY-prevY)*(curY-prevY));
        var time = new Date().getTime();
        times[index] = time - prevTime;

        variance = 1.0 - Math.sqrt(sum(coss)*sum(coss)+sum(sins)*sum(sins))/sins.length;

        direction = Math.atan2(1/sins.length*sum(sins),1/coss.length*sum(coss));
        var speed = sum(lengths)/(sum(times)/200);
        distance = Math.min(Math.max(40, speed), 100);
        prevTime = time;
        index = (index+1)%limit;
        prevX = curX;
        prevY = curY;
    }

    var sum = function(array){
        var s = 0.0;
        for(var i=0; i<array.length; i++){
            s += array[i];
        }
        return s;
    }

    var calculateDirection = function(event) {
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
        var angle = direction * (180/Math.PI);
//        Logger.debug("Hoek: " + angle + " - variance: " + variance);
        return direction;
    };

    // sets the current active block
    this.getHooveredBlockForPosition = function (x, y) {
        var currentBlock = hoveredBlocks.current;
        hoveredBlocks.current = null;
        // First search for active element
        // If an element is active, we have a big chance the next event is in the same element, so we start our search here
        if (currentBlock != null) {
            var bb = currentBlock.findActiveElement(x, y);
            if (bb instanceof blocks.elements.Block) {
                hoveredBlocks.current = bb;
            }
        }
        // Our shortcut failed so search the full page
        // we loop the trees of elements to find the smallest active element
        if (hoveredBlocks.current == null) {
            var i = 0;
            while (i < Broadcaster.getLayoutTree().length && hoveredBlocks.current == null) {
                var bb = Broadcaster.getLayoutTree()[i].findActiveElement(x, y);
                if (bb instanceof blocks.elements.Block) {
                    hoveredBlocks.current = bb;
                }
                i++;
            }
        }

        if (hoveredBlocks.current != currentBlock) {
            hoveredBlocks.previous = currentBlock;
        }

        // Set Property
        var currentProperty = properties.current
        properties.current = null;
        if (hoveredBlocks.current != null) {
//            properties.current = hoveredBlocks.current.getProperty(x, y);
        }
        if (properties.current != currentProperty) {
            properties.previous = currentProperty;
        }

        return hoveredBlocks;
    };


    this.send = function(eventName, custom) {
        if (active || eventName == Broadcaster.EVENTS.START_BLOCKS) {
//        Logger.debug(eventName);
            var e = $.Event(eventName);
            e.pageX = lastMoveEvent.pageX;
            e.pageY = lastMoveEvent.pageY;
            e.direction = lastMoveEvent.direction;
            e.block = hoveredBlocks;
            e.property = properties;
            e.custom = custom;
            // send the event with jquery
            $(document).triggerHandler(e);
        }
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

    this.EVENTS.HOOVER_LEAVE_PROPERTY = "HOOVER_LEAVE_PROPERTY";
    this.EVENTS.HOOVER_ENTER_PROPERTY = "HOOVER_ENTER_PROPERTY";
    this.EVENTS.HOOVER_OVER_PROPERTY = "HOOVER_OVER_PROPERTY";

    this.EVENTS.END_HOOVER = "END_HOOVER";
    this.EVENTS.DOUBLE_CLICK_BLOCK = "DOUBLE_CLICK_BLOCK";
    this.EVENTS.CLICK_BLOCK = "CLICK_BLOCK";

    // Notifications
    this.EVENTS.DO_ALLOW_DRAG = "ALLOW_DRAG";
    this.EVENTS.DO_NOT_ALLOW_DRAG = "DO_NOT_ALLOW_DRAG";
    this.EVENTS.ACTIVATE_MOUSE = "ACTIVATE_MOUSE";
    this.EVENTS.DEACTIVATE_MOUSE = "DEACTIVATE_MOUSE";
    this.EVENTS.DO_REFRESH_LAYOUT = "DO_REFRESH_LAYOUT";
    this.EVENTS.WILL_REFRESH_LAYOUT = "WILL_REFRESH_LAYOUT";
    this.EVENTS.DID_REFRESH_LAYOUT = "DID_REFRESH_LAYOUT";
    this.EVENTS.DOM_WILL_CHANGE = "DOM_WILL_CHANGE";
    this.EVENTS.DOM_DID_CHANGE = "DOM_DID_CHANGE";
    this.EVENTS.STOP_BLOCKS = "STOP_BLOCKS";
    this.EVENTS.START_BLOCKS = "START_BLOCKS";
    this.EVENTS.WILL_SAVE = "WILL_SAVE";
    this.EVENTS.DID_SAVE = "DID_SAVE";


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

    var isContainer = function(element) {
        return DOM.isProperty(element) || DOM.canLayout(element) || DOM.canEdit(element);
    }

    var findContainers = function(element) {
        var retVal = [];
        if (isContainer(element)) {
            retVal.push(element);
        } else {
            var children = element.children();
            for(var i=0; i < children.length; i++) {
                var block = $(children[i]);
                if (isContainer(block)) {
                    retVal.push(block)
                } else {
                    retval.push.apply(retval, findContainers(block));
                }
            }
        }
        return retVal;
    };


    var buildLayoutTree = function () {
        hoveredBlocks.previous = null;
        hoveredBlocks.current = null;
        layoutTree = [];
        //_this.cleanLayout();
        if (layoutParentElement == null) {
            layoutParentElement = $("body");
        }

        var findContainersInParent = function(parent) {

            if (parent != null && parent.length > 0 && isContainer(parent)) {
                var container = new blocks.elements.Container(parent);
                container.createAllDropspots();
                Logger.debug(container);
                layoutTree.push(container);
            } else {
                var children = parent.children();
                for (var i = 0; i < children.length; i++) {
                    findContainersInParent($(children[i]));
                }
            }

        };

        findContainersInParent(layoutParentElement);


        Broadcaster.send(Broadcaster.EVENTS.DID_REFRESH_LAYOUT);
    };


    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function() {
        Broadcaster.send(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT);
        buildLayoutTree();
    })

    $(document).on(Broadcaster.EVENTS.DOM_DID_CHANGE, function() {
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT);
    });

    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function() {
        active = true;
        layoutTree = null;
        buildLayoutTree();
        registerMouseMove();
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function() {
        active = false;
        unregisterMouseMove();
        layoutTree = [];
    });

    // On Boot
    var resizeTimeout = null
    $(window).on("resize.blocks_broadcaster", function () {

        if (resizeTimeout != null) {
            clearTimeout(resizeTimeout);
            resizeTimeout = null;
            Logger.debug("timeout cleared")
        } else {
            Logger.debug("timeout not cleared")
        }
        resizeTimeout = setTimeout(function(){ Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT);}, 200);

    });

}])
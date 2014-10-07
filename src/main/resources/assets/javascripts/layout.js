var Blocks = Blocks || {}


//(function() {
    Blocks.Core = my.Class({

        STATIC: {
            //Classes
            ROW: "row",
            COLUMN: "column"
        },

        RegisteredEvent: my.Class({
            constructor: function(name, namespace, callback) {
                this.name = name;
                this.namespace = namespace;
                this.callback = function() {};
                if (callback != null) {
                    this.callback = callback;
                }
            }
        }),

        constructor: function() {
            this.active = true;
            this.events = {};
            this.mouseEvents = new Blocks.MouseEvent(this);
            this.ruler = $(".ruler");

            blocks= this;
        },

        layoutChanged: function() {
            this.layoutTreeFactory.generateHotspots();
        },

        activate: function() {
            this.active = true;
        },

        deactivate: function() {
            this.active = false;
        },

        on: function(eventName, callback) {
            var splitName = eventName.split(".");
            var name = splitName[0];
            var namespace = "";
            if (splitName.length > 1) {
                namespace = splitName[1];
            }

            if (this.events[name] == null) {
                this.events[name] = [];
            }
            var event = new this.RegisteredEvent(name, namespace, callback);
            this.events.push(event);
        },

        off: function(eventName) {
            var splitName = eventName.split(".");
            var name = splitName[0];
            var namespace = "";
            if (splitName.length > 1) {
                namespace = splitName[1];
            }
            if (this.events[name] != null) {
                var okEvents = [];
                var removeEvents = [];

                for (var i = 0; i < this.events[name].length; i++) {
                    var e = this.events[name][i];
                    if (e.name == name && e.namespace == namespace) {
                        removeEvents.push(e);
                    } else {
                        okEvents.push(e);
                    }
                }
                this.events[name] = okEvents;
            }

        },

        send: function(eventName) {
            var splitName = eventName.split(".");
            var name = splitName[0];
            if (this.events[name] != null) {
                for (var i = 0; i < this.events[name].length; i++) {
                    this.events[name][i].callback();
                }
            }
        }


        /* OnClick
If click, save start coos:
if threshold passed -> start dragging
if start drag -> calculate object with co'n hotspots
While dragging check hotspots


*/
    });

Blocks.MouseEvent = my.Class({
    STATIC: {
        DRAGGING: {
            WAITING: 1,
            YES: 2,
            NO: 3,
            TEXT_SELECTION: 4
        },
        // threshold in px to start dragging of blocks
        DRAGGING_THRESHOLD: 10
    },

    constructor: function() {
        this.active = true;
        this.draggingStatus = Blocks.MouseEvent.DRAGGING.NO;
        this.layoutTreeFactory = new Blocks.LayoutTreeFactory();
        this.drag = {startEvent: null, surface: null};
        this.event = null;
        this.dropSpot = {current: null, previous: null};
        this.resizeHandle = {current: null, previous: null};
        this.layoutElement = {current: null, previous: null};
        this.registerMouseEvents();
    },

    getActiveElements: function() {
        this.dropSpot.previous = this.dropSpot.current;
        this.resizeHandle.previous = this.resizeHandle.current;
        this.layoutElement.previous = this.layoutElement.current;

        // First search for active element
        // If an element is active, we have a big chance the next event is in the same element, so we start our search here
        this.layoutElement.current = null;
        this.dropSpot.current = null;
        this.resizeHandle.current = null;

        if (this.layoutElement.previous != null) {
            this.layoutElement.current = this.layoutElement.previous.findActiveElement(event);
        }
        // Our shortcut failed so search the full page
        // we loop the trees of elements to find the smallest active element
        if (this.layoutElement.current == null) {
            var i = 0;
            while (i < this.layoutTreeFactory.layoutTrees.length && this.layoutElement.current == null) {
                this.layoutElement.current = this.layoutTreeFactory.layoutTrees[i].findActiveElement(event);
                i++;
            }
        }
//        this.currentActiveElement = activeElement;

        if (this.layoutElement.current != null) {
            this.dropSpot.current = this.layoutElement.current.findTriggeredTrigger(event);
        } else {
            this.dropSpot.current = null;
        }
    },

    createEvent: function() {

    },

    mouseDown: function(event) {
        if (this.active) {
            // send down event: resizermousedown, blockmousedown, dropspotmousedown
            // Blockevent = name, jquery Event, Surface
            this.getActiveElements();
            this.draggingStatus = Blocks.MouseEvent.DRAGGING.WAITING;
            this.drag.startEvent = event;

        } else {
            this.draggingStatus = Blocks.MouseEvent.DRAGGING.NO;
            this.drag.startEvent = null;
        }
    },

    mouseUp: function(event) {
        this.draggingStatus = Blocks.MouseEvent.DRAGGING.NO;
        this.drag.startEvent = null;

        // if dragging send surfacedragend
        // else surfaceclick

    },

    enableDragAfterTreshold: function(event) {
        Logger.debug("Wait for drag: " + Math.abs(this.dragging.start_x - event.pageX) + " " + Math.abs(this.dragging.start_y - event.pageY));
        if (Math.abs(this.dragging.start_x - event.pageX) > Blocks.Core.DRAGGING_THRESHOLD || Math.abs(this.dragging.start_y - event.pageY) > Blocks.Core.DRAGGING_THRESHOLD) {
            Logger.debug("Start dragging.");
            this.draggingStatus = Blocks.MouseEvent.DRAGGING.YES;
            // send surfacedragstart
            // Blocks.mouseevent
        }
    },

    mouseMove: function(event) {
        this.event = event;
        if (this.active) {
            if (this.draggingStatus == Blocks.MouseEvent.DRAGGING.NO) {
                // Send Hooverevent
            } else if (this.draggingStatus == Blocks.MouseEvent.DRAGGING.WAITING) {
                this.enableDragAfterTreshold(event)
            }


            if (this.draggingStatus == Blocks.MouseEvent.DRAGGING.YES) {
                // check hotspots
                var newTrigger = this.layoutTreeFactory.getTriggeredTrigger(event)
                if (newTrigger != this.currentTrigger) {
                    if (newTrigger != null) {
                        this.ruler.css("left", newTrigger.left + "px");
                        this.ruler.css("top", newTrigger.top + "px");
                        this.ruler.css("width", newTrigger.right - newTrigger.left + "px");
                        this.ruler.css("height", newTrigger.bottom - newTrigger.top + "px");
                    } else {
                        this.ruler.css("left", "0px");
                        this.ruler.css("top", "0px");
                        this.ruler.css("width", "0px");
                        this.ruler.css("height", "0px");

                    }
                    this.currentTrigger = newTrigger;
                }
            }
        }
    },

    registerMouseEvents: function() {
        var _this = this;
        $(document).on("mousedown.blocks_core", function (event) {_this.mouseDown(event)});
        $(document).on("mouseup.blocks_core", function (event) {_this.mouseUp(event)});
        $(document).on("mousemove.blocks_core", function (event) {_this.mouseMove(event)});
    },

    unregisterMouseEvent: function() {
        $(document).on("mousedown.blocks_core");
        $(document).on("mouseup.blocks_core");
        $(document).off("mousemove.blocks_core");
    }
})

    $(document).ready(function() {
        bb = new Blocks.Core();
    });
//})();

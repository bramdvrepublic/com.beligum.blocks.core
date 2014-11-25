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
blocks.plugin("blocks.core.Broadcaster", ["blocks.core.Class", function (Class) {
    this.active = true;
    var events = [];

    var RegisteredEvent = Class.create({
        constructor: function (name, callback, namespace) {
            this.name = name;
            this.namespace = namespace;
            this.callback = callback != null ? callback : function () {};
        },

        run: function(event, timeout) {
            var timeOut = (timeOut == null || timeOut == undefined) ? 0 : timeout ;

            if (this.namespace == event.namespace || event.namespace == null) {
                if (timeOut < 0) {
                    this.callback(event);
                } else {
                    setTimeout(this.callback(event), timeOut);
                }
            }
        }


    });

    var activate = function () {
        this.active = true;
    };

    var deactivate = function () {
        this.active = false;
    };

    this.on = function (event, namespace, callback) {
        if (typeof event === 'function' && event.prototype.NAME != null) {
            var name = event.prototype.NAME;

            if (events[name] == null) {
                events[name] = [];
            }
            var event = new RegisteredEvent(name, callback, namespace);
            events[name].push(event);
        } else {

        }
    };

    this.off = function (event, namespace) {
        if (typeof event === 'function' && event.prototype.NAME != null) {
            var name = event.prototype.NAME;

            if (events[name] != null) {
                var okEvents = [];
                var removeEvents = [];

                for (var i = 0; i < events[name].length; i++) {
                    var e = events[name][i];
                    if (e.name == name && (e.namespace == namespace || namespace == null)) {
                        removeEvents.push(e);
                    } else {
                        okEvents.push(e);
                    }
                }
                events[name] = okEvents;
            }
        }
    };

    // send events async with timeout
    // if timeout < 0 send events sync (or use sendNoTimeout)
    this.send = function (event, timeout) {

        var name = event.NAME;
        //Logger.debug("event send: " + event.NAME);
        //Logger.debug(event);

        if (events[name] != null) {
            for (var i = 0; i < events[name].length; i++) {
                var registeredEvent = events[name][i];
                registeredEvent.run(event, timeout);
            }
        }
    };

    this.sendNoTimeout = function (eventName, param) {
        this.send(eventName, param, -1);
    };

    var BasicEvent = Class.create({

        NAME: "UNKNOWN_EVENT",
        namespace: null
    });
    this.BASIC_EVENT = BasicEvent;

    this.EVENTS = {};
    // EVents with callback

    // Owner is a string that identifies the owner of the request
    this.EVENTS.ENABLE_DRAG = Class.create(BasicEvent, {

        constructor: function(priority, owner, callback) {
            this.priority = priority;
            this.owner = owner;
            if (this.priority == null) this.priority = 0;
            this.cb = callback;
        },

        callback: function(enabled) {
            if (this.cb != null) this.cb(enabled);
        },
        NAME: "ENABLE_DRAG"
    });
    this.EVENTS.DISABLE_DRAG = Class.create(BasicEvent, {
        constructor: function(priority, owner) {
            this.priority = priority;
            this.owner = owner;
            if (this.priority == null) this.priority = 0;
        },

        NAME: "DISABLE_DRAG"
    });

    this.EVENTS.DRAG_DISABLED = Class.create(BasicEvent, {
        constructor: function(priority, owner) {
            this.priority = priority;
            this.owner = owner;
            if (this.priority == null) this.priority = 0;
        },

        NAME: "DRAG_DISABLED"
    });

    // Events with blockEvent as argument
    this.EVENTS.START_DRAG = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "START_DRAG"
    });
    this.EVENTS.END_DRAG = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "END_DRAG"
    });
    this.EVENTS.ABORT_DRAG = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "ABORT_DRAG"
    });
    this.EVENTS.DRAG_LEAVE_BLOCK = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "DRAG_LEAVE_BLOCK"
    });
    this.EVENTS.DRAG_ENTER_BLOCK = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "DRAG_ENTER_BLOCK"
    });
    this.EVENTS.DRAG_OVER_BLOCK = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "DRAG_OVER_BLOCK"
    });
    this.EVENTS.HOOVER_LEAVE_BLOCK = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "HOOVER_LEAVE_BLOCK"
    });
    this.EVENTS.HOOVER_ENTER_BLOCK = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "HOOVER_ENTER_BLOCK"
    });
    this.EVENTS.HOOVER_OVER_BLOCK = Class.create(BasicEvent, {

        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "HOOVER_OVER_BLOCK"
    });
    this.EVENTS.END_HOOVER = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "END_HOOVER"
    });

    this.EVENTS.DOUBLE_CLICK_BLOCK = Class.create(BasicEvent, {
        constructor: function(blockEvent) {
            this.blockEvent = blockEvent;
        },
        NAME: "DOUBLE_CLICK_BLOCK"
    });

    // Notifications
    this.EVENTS.DO_ALLOW_DRAG = Class.create(BasicEvent, {
        NAME: "ALLOW_DRAG"
    });
    this.EVENTS.DO_NOT_ALLOW_DRAG = Class.create(BasicEvent, {
        NAME: "DO_NOT_ALLOW_DRAG"
    });
    this.EVENTS.ACTIVATE_MOUSE = Class.create(BasicEvent, {
        NAME: "ACTIVATE_MOUSE"
    });
    this.EVENTS.DEACTIVATE_MOUSE = Class.create(BasicEvent, {
        NAME: "DEACTIVATE_MOUSE"
    });

    this.EVENTS.DO_REFRESH_LAYOUT = Class.create(BasicEvent, {
        NAME: "DO_REFRESH_LAYOUT"
    });
    this.EVENTS.DID_REFRESH_LAYOUT = Class.create(BasicEvent, {
        NAME: "DID_REFRESH_LAYOUT"
    });
    this.EVENTS.DOM_WILL_CHANGE = Class.create(BasicEvent, {
        NAME: "DOM_WILL_CHANGE"
    });
    this.EVENTS.DOM_DID_CHANGE = Class.create(BasicEvent, {
        NAME: "DOM_WILL_CHANGE"
    });

    this.EVENTS.REGISTER_EDITABLE_BLOCK = Class.create(BasicEvent, {
        constructor: function(blockClassName, adminUrl) {
            this.blockClassName = blockClassName;
            this.adminUrl = adminUrl;
        },
        NAME: "REGISTER_BLOCK"
    });

    this.EVENTS.PREPARE_EDIT_BLOCK = Class.create(BasicEvent, {
        NAME: "PREPARE_EDIT_BLOCK"
    });

    this.EVENTS.START_EDIT_BLOCK = Class.create(BasicEvent, {
        NAME: "START_EDIT_BLOCK"
    });

    this.EVENTS.END_EDIT_BLOCK = Class.create(BasicEvent, {
        NAME: "END_EDIT_BLOCK"
    });




}])
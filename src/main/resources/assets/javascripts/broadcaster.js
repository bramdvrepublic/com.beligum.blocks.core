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
blocks.plugin("blocks.core.Broadcaster", ["blocks.core.RegisteredEvent", function (RegisteredEvent) {
    this.active = true;
    var events = [];

    var activate = function () {
        this.active = true;
    }

    var deactivate = function () {
        this.active = false;
    }

    this.on = function (eventName, callback) {
        var splitName = eventName.split(".");
        var name = splitName[0];
        var namespace = null;
        if (splitName.length > 1) {
            namespace = splitName[1];
        }

        if (events[name] == null) {
            events[name] = [];
        }
        var event = new RegisteredEvent.create(name, namespace, callback);
        events[name].push(event);
    }

    this.off = function (eventName) {
        var splitName = eventName.split(".");
        var name = splitName[0];
        var namespace = null;
        if (splitName.length > 1) {
            namespace = splitName[1];
        }
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
    };

    // send events async with timeout
    // if timeout < 0 send events sync (or use sendNoTimeout)
    this.send = function (eventName, param, timeout) {
        var timeOut = timeout;
        if (timeOut == null || timeOut == undefined) {
            timeOut = 0;
        }
        var splitName = eventName.split(".");
        var name = splitName[0];
        var namespace = null;
        if (splitName.length > 1) {
            namespace = splitName[1];
        }

        if (events[name] != null) {
            for (var i = 0; i < events[name].length; i++) {
                var currentEvent = events[name][i];
                if (currentEvent.namespace == namespace || namespace == null) {
                    if (timeOut < 0) {
                        events[name][i].run(param);
                    } else {
                        setTimeout(currentEvent.run(param), timeOut);
                    }
                }
            }
        }
    };

    this.sendNoTimeout = function (eventName, param) {
        this.send(eventName, param, -1);
    }

}])
    .plugin("blocks.core.RegisteredEvent", [function () {
        this.create = function (name_t, namespace_t, callback_t) {
            var name = name_t;
            var namespace = namespace_t;
            var callback = function () {
            };
            if (callback_t != null) {
                callback = callback_t;
            }

            this.run = function (parameter) {
                callback(parameter);
            }
        }
    }]);




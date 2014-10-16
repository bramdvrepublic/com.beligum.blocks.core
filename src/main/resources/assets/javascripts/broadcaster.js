
blocks.module("blocks.broadcaster", [])
    .service("Broadcaster", ["RegisteredEvent", function(RegisteredEvent) {
        this.active = true;
        var events = [];


        var activate = function() {
            this.active = true;
        }

        var deactivate = function() {
            this.active = false;
        }

        this.on = function(eventName, callback) {
            var splitName = eventName.split(".");
            var name = splitName[0];
            var namespace = "";
            if (splitName.length > 1) {
                namespace = splitName[1];
            }

            if (events[name] == null) {
                events[name] = [];
            }
            var event = new RegisteredEvent(name, namespace, callback);
            events[name].push(event);
        }

        this.off = function(eventName) {
            var splitName = eventName.split(".");
            var name = splitName[0];
            var namespace = "";
            if (splitName.length > 1) {
                namespace = splitName[1];
            }
            if (events[name] != null) {
                var okEvents = [];
                var removeEvents = [];

                for (var i = 0; i < events[name].length; i++) {
                    var e = events[name][i];
                    if (e.name == name && e.namespace == namespace) {
                        removeEvents.push(e);
                    } else {
                        okEvents.push(e);
                    }
                }
                events[name] = okEvents;
            }

        }

        this.send = function(eventName, param) {
            Logger.debug("Send event: " + eventName);
            var splitName = eventName.split(".");
            var name = splitName[0];
            if (events[name] != null) {
                for (var i = 0; i < events[name].length; i++) {
                    var currentEvent = events[name][i];
                    setTimeout(events[name][i].run(param), 0);
                }
            }
        }
    }])
    .factory("RegisteredEvent", [function() {
            return function(name_t, namespace_t, callback_t) {
                var name = name_t;
                var namespace = namespace_t;
                var callback = function() {};
                if (callback_t != null) {
                    callback = callback_t;
                }

                this.run = function(parameter) {
                callback(parameter);
            }
        }
    }]);




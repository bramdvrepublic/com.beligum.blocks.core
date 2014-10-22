function blocksLoader(window) {
    /*Simple helper functions copied from angular*/

    /*Check if property of object exists and create with new (instantiated)value
    * if not */
    function ensure(obj, name, factory) {
        return obj[name] || (obj[name] = factory());
    }

    // true if value is function
    function isFunction(value) {
        return typeof value === 'function';
    }

    // true is value is array
    var isArray = Array.isArray;

    // true if value is object
    function isObject(value) {
        // http://jsperf.com/isobject4
        return value !== null && typeof value === 'object';
    }

    function isString(value) {
        return typeof value === 'string';
    }

    // define blocks in window
    var blocks = ensure(window, 'blocks', Object);

    /*Dependency injection for blocks
    * ---------------------------------
    * use:
    * e.g.:
    * blocks.plugin("test", function() {
    *              this.doSomething = function() {
    *                  // do something
    *              }
    *        })
    *
    * use a plugin as dependency for another plugin. The dependency will be automatically injected
    *
    * e.g.:
    * blocks.plugin("test2", ["test", function(test) {
    *                                    test.doSomething()
    *                                 }])
    * or:
    * blocks.plugin("test2", "test", function(test) {}]);
    * blocks.plugin("test2", ["test"], function(test) {}])
    * blocks.plugin("test2", ["test", "othertest"], function(test, ot) {}])
    *
    * All plugins added to blocks will be instantiated after document.ready() with the command blocks.run()
    *
    * You can add some config variables for each plugin with:
    * blocks.config("name of plugin", {test: "test"});
    *
    * Each config must be an object. The config will be added to the instance of the plugin as a config property.
    * You can redefine the keys of an existing config by redefining the config for a plugin. Only changed values
    * will be overwritten. A nez config will overwrite an existing config for a plugin in the order they are loaded.
    *
    * blocks.config("test", {y: "test", x: 3});
    * blocks.config("test", {x: "4"});
    *
    * blocks.plugin("test2", ["test", function(test) {
    *                                    test.config.y -> test
    *                                    test.config.x -> 4
    *                         }])
    *
    *
    *        */
    ensure(blocks, "loader", function () {
        var _serviceDefinitions = {};
        var _serviceInstances = {};
        var _configDefinitions = {};
        var INSTANTIATING = '---INSTANTIATING---';


        /*Instantiate all loaded services (called by run)*/
        var instantiateAllServices = function () {

            for (service in _serviceDefinitions) {
                // do not reinstantiate already instantiated service
                if (_serviceDefinitions.hasOwnProperty(service) && _serviceInstances[service] == null) {
                    instantiateService(_serviceDefinitions[service]);
                }
            }
        };


        /*Instantiate a service:
        * Fisrt check all dependencies by name and look them up. If a dependency
        * if not yet instantiated, instantiate it. Add all instantiated dependencies to an argument list.
        * Then create constructor, add service.fn (function to instantiate) to prototype
         * instantiate service and apply with self.*/
        var instantiateService = function (service) {
            _serviceInstances[service.name] = INSTANTIATING;
            var instantiatedDependencies = [];

            // instantiate all dependencies (if necessary)
            for (var i = 0; i < service.dependencies.length; i++) {
                // required service already instantiated
                if (_serviceInstances[service.dependencies[i]] != null) {
                    if (_serviceInstances[service.dependencies[i]] == INSTANTIATING) {
                        throw Logger.error("Could not instantiate service: Circular dependency found");
                    } else {
                        instantiatedDependencies.push(_serviceInstances[service.dependencies[i]]);
                    }
                } else {
                    // if required service not yet instantiated
                    // and if serviceDefinition exists -> instantiate
                    if (_serviceDefinitions[service.dependencies[i]] != null) {
                        _serviceInstances[service.dependencies[i]] = instantiateService(_serviceDefinitions[service.dependencies[i]])
                        instantiatedDependencies.push(_serviceInstances[service.dependencies[i]]);
                    } else {
                        throw Logger.error("Could not instantiate service: Service not found");
                    }
                }
            }

            var Constructor = function () {};
            Constructor.prototype = service.fn.prototype;
            var config = _configDefinitions[service.name];
            service.fn.prototype.config = config;
            var instance = new Constructor();
            service.fn.apply(instance, instantiatedDependencies);
            return _serviceInstances[service.name] = instance;
        };

        var run = function () {
            instantiateAllServices();
        };

        var loadConfig = function (name, value) {
            if (isString(name) && isObject(value)) {
                if (_configDefinitions[name] == null) {
                    _configDefinitions[name] = {};
                }
                var config = _configDefinitions[name];

                for (key in value) {
                    if (value.hasOwnProperty(key)) {
                        config[key] = value[key];
                    }
                }

            } else {
                throw Logger.debug("Config name is not a string or value is not an object");
            }
        };

        var loadService = function () {
            var service = {
                name: null,
                dependencies: [],
                fn: null
            };

            if (arguments.length > 1 && isString(arguments[0]) && arguments[0].length > 0) {
                service.name = arguments[0];
            } else {
                throw Logger.debug("Service needs a name.")
            }
            loadConfig(service.name, {});

            // service("serviceName", function(){})
            // or service("serviceName", ["dependency", function(dependency) {})
            if (arguments.length == 2) {
                if (isArray(arguments[1])) {
                    var fn = arguments[1].pop();
                    if (isFunction(fn)) {
                        service.fn = fn;
                        // check if all strings
                        service.dependencies = arguments[1];
                    } else {
                        throw Logger.debug("Service is not a function");
                    }
                } else if (isFunction(arguments[1])) {
                    service.fn = arguments[1];
                } else {
                    throw Logger.error("Invalid service: no valid dependencies or function added");
                }

            } else if (arguments.length == 3) {
                if (isString(arguments[1])) {
                    service.dependencies.push(arguments[1])
                } else if (isArray(arguments[1])) {
                    service.dependencies = arguments[1];
                } else {
                    throw Logger.error("Invalid service: no valid dependencies added");
                }

                if (isFunction(arguments[2])) {
                    service.fn = arguments[2];
                } else {
                    throw Logger.error("Invalid service: no valid function");
                }

            } else {
                throw Logger.error("Invalid service: wrong number of arguments");
            }
            _serviceDefinitions[service.name] = service;
            return blocks;

        }


        return {
            loadService: loadService,
            loadConfig: loadConfig,
            run: run
        }

    });
    /*short notation to add plugins and configs*/
    blocks.plugin = blocks.loader.loadService;
    blocks.config = blocks.loader.loadConfig;
    blocks.run = blocks.loader.run;
}

blocksLoader(window);

// instantiate all plugins
$(document).ready(function () {
    blocks.run();
});



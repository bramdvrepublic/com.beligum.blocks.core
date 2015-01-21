/**
 * Created by wouter on 13/10/14.
 */

// All constants

blocks.plugin("blocks.core.Constants", function() {
    this.DIRECTION = {
        UP: 1,
        DOWN: 2,
        LEFT: 3,
        RIGHT: 4,
        NONE: 0
    };

    this.OPPOSITE_DIRECTION = {
        1: 2,
        2: 1,
        4: 3,
        3: 4,
        0: 0
    };

    this.DRAGGING = {
        WAITING: 1,
        YES: 2,
        NO: 3,
        NOT_ALLOWED: 5,
        TEXT_SELECTION: 6
    };

    this.SIDE = {
        TOP: 1,
        BOTTOM: 2,
        LEFT: 3,
        RIGHT: 4,
        NONE: 0
    };

    this.OPPOSITE_SIDE = {
        1: 2,
        2: 1,
        3: 4,
        4: 3,
        0: 0
    };

    this.CONTAINER_CLASS = "container"
    this.ROW_CLASS = "row";
    this.BLOCK_CLASS = "block";
    this.FAKE_BLOCK = "fake-block";

    // Must be ordered from small to big
    this.COLUMN_WIDTH_CLASS = [
        {name: "col-xs-", min: 0, max: 767},
        {name: "col-sm-", min: 768, max: 991},
        {name: "col-md-", min: 992, max: 1199},
        {name: "col-lg-", min: 120, max: 10000}
    ];
    this.CAN_LAYOUT_ROW_CLASS = "can-layout"; // can layout row and add and delete blocks
    this.CAN_EDIT_BLOCK_CLASS = "can-edit"; // specifies edit and delete
    this.CAN_CAST_BLOCK_CLASS = "can-cast-block";
    this.IS_ENTITY = "typeof";
    this.IS_PROPERTY = "property";

    this.MAX_COLUMNS = 12;


    this.OVERLAY_CLASS = "blocks-overlay";
    this.OVERLAY_BACKGROUND_CLASS = "block-hover";
    this.COLUMN_RESIZER_CLASS = "column-resize-handle";
    this.BLOCK_CLASS = "block-hover";
    this.PROPERTY_CLASS = "property-hover";
    this.OVERLAY_BACKGROUND_CLASS = "block-overlay-background";
    this.ADMIN_ANIMATION_FRONT = "admin-animation-front";
    this.ADMIN_ANIMATION_BACK = "admin-animation-back";
    this.ADMIN_ANIMATION_MASTER = "admin-animation-master";


    this.BLOCK_OVERLAY_INDEX = 2000;
    this.SELECTED_BLOCK_INDEX = 2050;
    this.SELECTED_BLOCK_BACKGROUND_INDEX = 2040;
    this.DIALOG_INDEX = 4000;
    this.DIALOG_OVERLAY_INDEX = 3000;
    this.RESIZEHANDLE_INDEX = 2500;


});

blocks.plugin("blocks.core.Class", [function () {

    //============================================================================
    // @method my.extendClass
    // @params Class:function, extension:Object, ?override:boolean=true
    var extendClass = function (Class, extension, override) {
        if (extension.STATIC) {
            extend(Class, extension.STATIC, override);
            delete extension.STATIC;
        }
        extend(Class.prototype, extension, override);
    };
    //============================================================================
    var extend = function (obj, extension, override) {
        var prop;
        if (override === false) {
            for (prop in extension)
                if (!(prop in obj))
                    obj[prop] = extension[prop];
        } else {
            for (prop in extension)
                obj[prop] = extension[prop];
            if (extension.toString !== Object.prototype.toString)
                obj.toString = extension.toString;
        }
    };

    this.create = function () {
        var len = arguments.length;
        var body = arguments[len - 1];
        var SuperClass = len > 1 ? arguments[0] : null;
        var hasImplementClasses = len > 2;
        var Class, SuperClassEmpty;

        if (body.constructor === Object) {
            Class = function () {
            };
        } else {
            Class = body.constructor;
            delete body.constructor;
        }

        if (SuperClass) {
            SuperClassEmpty = function () {
            };
            SuperClassEmpty.prototype = SuperClass.prototype;
            Class.prototype = new SuperClassEmpty();
            Class.prototype.constructor = Class;
            Class.Super = SuperClass;

            extend(Class, SuperClass, false);
        }

        if (hasImplementClasses)
            for (var i = 1; i < len - 1; i++)
                extend(Class.prototype, arguments[i].prototype, false);

        extendClass(Class, body);

        return Class;
    }
}]);

blocks.plugin("blocks.core.Logger", ["blocks.core.Class", function(Class) {
    Application = Class.create({

        //-----CONSTANTS-----
        STATIC: {
            LEVEL_DEBUG: 1,
            LEVEL_INFO: 2,
            LEVEL_WARN: 3,
            LEVEL_ERROR: 4,
            LEVEL_ASSERT: 5
        },

        //-----VARIABLES-----

        //-----CONSTRUCTOR-----
        constructor: function () {
            this.APP_NAME = "Youthr";
            this.LOG_LEVEL = this.LEVEL_DEBUG;
        }

    });

    //make it 'static'
    Application = new Application();


    Logger = Class.create({

        //-----CONSTANTS-----
        STATIC: {
        },

        //-----VARIABLES-----
        currentLevel: Application.LOG_LEVEL,

        //-----CONSTRUCTOR-----
        constructor: function () {
        },

        //-----PUBLIC METHODS-----
        log: function (msg, level) {
            if (level && level >= this.currentLevel) {
                this._doLog(msg, level);
            }
            else {
                this._doLog(msg);
            }
        },
        debug: function (msg, objs) {
            this.log(msg, Application.LEVEL_DEBUG);
        },
        info: function (msg) {
            this.log(msg, Application.LEVEL_INFO);
        },
        warn: function (msg) {
            this.log(msg, Application.LEVEL_WARN);
        },
        error: function (msg) {
            this.log(msg, Application.LEVEL_ERROR);
        },
        assert: function (msg) {
            this.log(msg, Application.LEVEL_ASSERT);
        },
        dir: function (obj) {
            if (!typeof(console) == 'undefined') {
                console.dir(obj);
            }
        },
        trace: function () {
            if (!typeof(console) == 'undefined') {
                console.trace();
            }
        },
        setLevel: function (level) {
            this.currentLevel = level;
        },

        //-----PRIVATE METHODS-----
        _doLog: function (msg, level) {
            var prefix = "";

            var placeholderChar = "s";
            if (msg instanceof Object) {
                placeholderChar = "o";
            }

            var haveConsole = typeof(console) != 'undefined';

            switch (level) {
                case Application.LEVEL_DEBUG:
                    if (haveConsole && console.debug) {
                        console.debug('DEBUG: %' + placeholderChar, msg);
                    }
                    break;
                case Application.LEVEL_INFO:
                    if (haveConsole && console.info) {
                        console.info('INFO: %' + placeholderChar, msg);
                    }
                    break;
                case Application.LEVEL_WARN:
                    if (haveConsole && console.warn) {
                        console.warn('WARN: %' + placeholderChar, msg);
                    }
                    break;
                case Application.LEVEL_ERROR:
                    if (haveConsole && console.error) {
                        console.error('ERROR: %' + placeholderChar, msg);
                    }
                    else {
                        alert('ERROR: ' + msg);
                    }
                    break;
                case Application.LEVEL_ASSERT:
                    if (haveConsole && console.assert) {
                        console.assert('ASSERT: %' + placeholderChar, msg);
                    }
                    break;
                default:
                    if (haveConsole && console.log) {
                        console.log(msg);
                    }
                    break;
            }
        }
    });

    //make it 'static'
    Logger = new Logger();


}]);


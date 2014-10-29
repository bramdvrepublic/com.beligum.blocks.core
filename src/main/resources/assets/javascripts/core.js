/**
 * Created by wouter on 13/10/14.
 */

blocks.plugin("blocks.core.Class", [function () {

    //============================================================================
    // @method my.extendClass
    // @params Class:function, extension:Object, ?override:boolean=true
    var extendClass = my.extendClass = function (Class, extension, override) {
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

blocks.plugin("blocks.core.Constants", function() {
    this.DIRECTION = {
        UP: 1,
        DOWN: 2,
        LEFT: 3,
        RIGHT: 4,
        NONE: 0
    };

    this.DRAGGING = {
        WAITING: 1,
        YES: 2,
        CAN_START_DRAG: 3,
        CAN_NOT_START_DRAG: 4,
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

    this.BLOCK_DRAG_CORNER = {
        top: 0,
        left: 0,
        width: 50,
        height: 50
    };


    this.COLUMN_CLASS = "column";
    this.ROW_CLASS = "row";
    this.BLOCK_CLASS = "block";
    this.COLUMN_WIDTH_CLASS = "col-md-";
    this.CAN_LAYOUT_CLASS = "can-layout";
    this.MAX_COLUMNS = 12;
});

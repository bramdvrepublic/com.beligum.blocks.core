/**
 * Created by bram on 8/13/15.
 */
/*
 * This is the abstract superclass that all widgets need to extend
 */
base.plugin("blocks.imports.Widget", ["constants.blocks.core", "base.core.Class", "base.core.Commons", "blocks.core.SidebarUtils",  function (BlocksConstants, Class, Commons, SidebarUtils)
{
    var Widget = this;

    this.Class = Class.create({

        //-----STATICS-----
        STATIC: {
            SELECTOR_INDEX: {},
            OBJ_REFS: {},

            /**
             * Register a new widget class for the supplied tags
             * @param selectors the array of jquery selectors you want to register this widget class to
             */
            register: function(selectors)
            {
                //there should always be a tags option specified
                if (selectors && selectors.length && selectors.length>0) {
                    for (var i=0;i<selectors.length;i++) {
                        var selector = selectors[i];
                        // note that this will happen when we extend eg. blocks-spacer in a subclass;
                        // the superclass will be registered first, and later overwritten by it's subclass
                        if (Widget.Class.SELECTOR_INDEX[selector]) {
                            Logger.warn("Encountered a double Widget registration for '"+selector+"', overwriting.", this);
                        }

                        Widget.Class.SELECTOR_INDEX[selector] = this;
                    }
                }
                else {
                    throw Logger.error("Could not instantiate widget because the 'tags' option (an array containing the tags you want this widget to be registered to) was missing or wrong has the type.", selectors);
                }
            },

            /**
             * Factory method: create a new widget instance for the supplied element tag
             * @param element the html element you want to construct a widget for
             * @returns the instance or null if no such tag is registered
             */
            create: function (element)
            {
                var retVal = null;

                if (element != null) {
                    var clazz = null;

                    //search for the first selector that matches
                    $.each(Widget.Class.SELECTOR_INDEX, function(selector, widget) {
                        if (element.is(selector)) {
                            clazz = widget;
                            return false; // == break
                        }
                    });

                    //if we found a class, instantiate it
                    if (clazz) {
                        retVal = new clazz();
                    }
                }

                return retVal;
            }
        },

        //-----PUBLIC VARIABLES-----
        id: null,
        creationStamp: null,

        //-----PRIVATE VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            this.id = Commons.generateId();
            this.creationStamp = new Date().getTime();

            //note: this.constructor returns the class
            if (!Widget.Class.OBJ_REFS[this.constructor]) {
                Widget.Class.OBJ_REFS[this.constructor] = {};
                this.init();
            }
            //add a reference to this object
            Widget.Class.OBJ_REFS[this.constructor][this.id] = this;
        },

        //-----'ABSTRACT' METHODS-----
        /**
         * This class gets called only once for each subclass: when the first object of this subclass is instantiated
         */
        init: function ()
        {
        },

        /**
         * @param block the block that should get focus (not null)
         * @param element one of these:
         *                - the first property element on the way up of the element that got clicked (inside the block)
         *                - the template element (then element==block.element) that got clicked
         *                - the page element
         * @param hotspot the (possibly changed) mouse coordinates that function as the 'hotspot' for this event (object with top and left like offset())
         * @param event the original event that triggered this all
         */
        focus: function (block, element, hotspot, event)
        {
        },

        /**
         * @param same parameters as in focus()
         */
        blur: function (block, element)
        {
        },

        /**
         * @param first two parameters as in focus()
         * @return an array containing config UI, created with the SidebarUtils factory classes (eg. SidebarUtils.addValueAttribute())
         */
        getConfigs: function (block, element)
        {
            return [];
        },

        /**
         * @return the name of the sidebar window for this widget if it's a block
         */
        getWindowName: function ()
        {
            return null;
        },

        //-----PUBLIC METHODS-----


        //-----PRIVATE METHODS-----

    });
}]);
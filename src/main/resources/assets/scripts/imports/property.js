/**
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.imports.Property", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Layouter", "base.core.Commons", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar, Layouter, Commons)
{
    var Property = this;

    (this.Class = Class.create(Widget.Class, {

        STATIC: {},

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Property.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            Property.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            Property.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = [];

            return retVal;
        },
        getWindowName: function ()
        {
            return Property.Class.Super.prototype.getWindowName.call(this);
        },

        //-----PRIVATE METHODS-----
        _getCreateLinkConfig: function (block, element)
        {
            var retVal = $('<div class="' + BlocksConstants.CREATE_LINK_WRAPPER_CLASS + '"></div>');

            var elementProperty = element.attr('property');
            if (!elementProperty) {
                elementProperty = element.attr('data-property');
            }
            if (!elementProperty) {
                throw Logger.error("Trying to wrap a Property element with a link, but I can't seem to find the property attribute?", element);
            }
            var LINK_PROP_ATTR = 'data-property';
            var LINK_SELECTOR = 'a[' + LINK_PROP_ATTR + '=' + elementProperty + ']';
            var wrapLink = $('<a ' + LINK_PROP_ATTR + '="' + elementProperty + '"></a>');

            //TODO those two are a little bit ugly, but it works...
            var _this = this;
            var addInputForm = function ()
            {
                var link = element.parent(LINK_SELECTOR);
                var linkInputAction = _this.addValueAttribute(Sidebar, link, null, "Paste or type a link", "href", false, true, true);
                retVal.append(linkInputAction);
                retVal.addClass('active');
            };
            var removeInputForm = function ()
            {
                var linkInputAction = retVal.find('input[type=text]').parents('.form-group').first();
                linkInputAction.remove();
                retVal.removeClass('active');
            };

            var startState = false;
            var toggleButton = this.createToggleButton("Create link?",
                function initStateCallback()
                {
                    var retVal = element.parent(LINK_SELECTOR).length > 0;

                    if (retVal) {
                        startState = true;
                    }

                    return retVal;
                },

                function switchStateCallback(oldState, newState)
                {
                    if (newState) {
                        element.wrap(wrapLink);
                        addInputForm();
                    } else {
                        //this parent-child iteration ensures we have a <a> parent
                        element.parent(LINK_SELECTOR).children().unwrap();
                        removeInputForm();
                    }
                },
                BlocksMessages.toggleLabelYes,
                BlocksMessages.toggleLabelNo
            );

            retVal.append(toggleButton);

            if (startState) {
                addInputForm();
            }

            return retVal;
        },
    }));
}]);
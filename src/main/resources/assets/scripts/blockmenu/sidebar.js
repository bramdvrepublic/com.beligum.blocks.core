/**
 * Created by wouter on 15/06/15.
 */


base.plugin("blocks.core.Sidebar", ["blocks.core.Broadcaster", "constants.blocks.common", "blocks.core.DomManipulation",  function (Broadcaster, Constants, DOM)
{
    var SideBar = this;

    var currentProperty = null;
    var configPanels = {};

    /*
    * When clicking a property disable drag drop
    * */
    var setPropertyState = function(property) {
        var sidebarElement = $("." + Constants.PAGE_SIDEBAR_CLASS);
        currentProperty = property;
        currentProperty.element.parents().siblings().addClass("opacity");
        currentProperty.element.siblings().addClass("opacity");
        sidebarElement.removeClass("opacity");
        Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);

        $(window).on("mousedown.sidebar", function(e) {
            if (!currentProperty.element.is(e.target) // if the target of the click isn't the container...
                && currentProperty.element.has(e.target).length === 0 // ... nor a descendant of the container
                && !sidebarElement.is(e.target) // ... nor the sidebar
                && sidebarElement.has(e.target).length == 0
                && (currentProperty.editFunction == null || currentProperty.editFunction.canBlur == null
                        || currentProperty.editFunction.canBlur(e))) //...
            {
                $(window).off("mousedown.sidebar");
                $(".opacity").removeClass("opacity");
                currentProperty.editFunction.blur(currentProperty);
                currentProperty = null;
                Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
                SideBar.update();
            }
        });
    };

    /*
    * Drill down and add functionality for each block
    * */
    this.update = function() {
        // property: add div
        var blockEvent = Broadcaster.createEvent();
        var property = null;

        var property = blockEvent.property.current;
        if (property != null && property.editFunction != null) {
            setPropertyState(property);
        } else if (property == null) {
            property = Broadcaster.block.current;
        }

        if (property != null) {
            if (property.editFunction != null && property.editFunction.focus != null) {
                property.editFunction.focus(property, blockEvent);
            }
            while (property.parent != null) {
                property = property.parent;
                if (property instanceof blocks.elements.Block) {
                    if (property.editFunction != null && property.editFunction.focus != null) {
                        property.editFunction.focus(property, blockEvent);
                    }
                }
            }
        }

    };

    this.updateBlock = function(element) {

    };


    this.addUIForProperty = function(style, property, html) {
        var config = createConfigForElement(style, property.element, null);
        var content = config.children(".panel-body");
        content.append(html);
    };

    this.addUniqueClass = function(property, element, name, values) {
        var classFound = null;
        var select = $("<select />");
        var selected = null;
        for (var i=0; i < array.length; i++) {
            var c = array[i];
            var option = $("<option />").attr("value", c).html(c);
            if (element.hasClass(c) && !classFound) {
                option.attr(selected, "true");
            } else if (element.hasClass(c) && !classFound) {
                element.removeClass(c);
            }
            select.append(option);
        }

        select.change(function(e) {
            if (selected != null) {
                element.removeClass(selected);
            }
            var option = selected.children("[selected]");
            selected = option.attr("value");
            element.addClass(selected);
        })
    };

    this.addOptionalClass = function(type, property, element, label, values) {

    };

    this.addOptionalAttribute = function(type, property, element, label, name, values) {

    };

    this.addUniqueAttribute = function(type, property, element, label, name, values) {

    };

    this.addValueAttribute = function(type, property, element, label, name) {

    };


    var createConfigForElement = function(type, element, title)
    {
        if (configPanels[type] == null) configPanels[type] = {};
        var panels = configPanels[type];

        if (panels[element] == null) {
            if (title == null) {
                title = element.attr("property");
                if (title == null) {
                    title = "Block: " + element[0].tagName;
                } else {
                    title = "Property: " + title;
                }
            }

            var div = $("<div class='panel panel-default'/>");
            var header = $("<div class='panel-heading'>" + title + "</div>");
            var content = $("<div class='panel-body'/>");
            div.append(header).append(content);

            panels[element] = div;
            if (type == Constants.STYLE) {
                $("#" + Constants.SIDEBAR_STYLE_ID).append(div);
            } else if (type == Constants.CONTENT) {
                $("#" + Constants.SIDEBAR_CONTENT_ID).append(div);
            }
        }
        return panels[element];
    }



}]);
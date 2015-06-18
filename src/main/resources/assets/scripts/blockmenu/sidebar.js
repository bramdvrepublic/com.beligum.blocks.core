/**
 * Created by wouter on 15/06/15.
 */


base.plugin("blocks.core.Sidebar", ["blocks.core.Broadcaster", "constants.blocks.common", "blocks.core.DomManipulation", "blocks.core.Layouter", "blocks.core.Plugin-Utils",  function (Broadcaster, Constants, DOM, Layouter, Plugin)
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
        currentProperty.element.parents().siblings().addClass(Constants.OPACITY_CLASS);
        currentProperty.element.siblings().addClass(Constants.OPACITY_CLASS);
        sidebarElement.removeClass(Constants.OPACITY_CLASS);
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
                $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
                currentProperty.editFunction.blur(currentProperty);
                currentProperty = null;
                Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
                SideBar.reset();
            }
        });
    };

    this.reset = function() {
        configPanels = {};
        $("#" + Constants.SIDEBAR_STYLE_ID).empty();
        $("#" + Constants.SIDEBAR_CONTENT_ID).empty();
        $("#inbetween").empty();
        var property = Broadcaster.getContainer();
        if (property.editFunction != null && property.editFunction.focus != null) {
            property.editFunction.focus(property, null);
        }
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
            if (property.canDrag) SideBar.addRemoveBlockButton(property);
            while (property.parent != null) {
                property = property.parent;
                if (property instanceof blocks.elements.Block) {
                    if (property.editFunction != null && property.editFunction.focus != null) {
                        property.editFunction.focus(property, blockEvent);
                    }
                    if (property.canDrag) SideBar.addRemoveBlockButton(property);
                }
            }
        }
    };


    this.addRemoveBlockButton = function(property) {
        var remove = $("<div class='panel panel-default "+ Constants.REMOVE_BLOCK_CLASS +"'/>");
        var body = $("<div class='panel-body'><div>");
        var text = $("<div class='text'><span>Remove block</span></div>");
        var button = $("<span class='btn btn-danger btn-sm pull-right'><i class='fa fa-trash-o'></i></span></div>");
        var confirm = $("<div class='confirm hidden'><div class='confirm-text'>Are you sure you want to remove this block?</div><div><span class='btn btn-sm btn-success'><i class='fa fa-check'></i></span><span class='btn btn-sm btn-danger'><i class='fa fa-times'></i></span></div></div>")
        var yes = confirm.find(".btn-success");
        var no = confirm.find(".btn-danger");

        button.click(function() {
            confirm.removeClass("hidden");
            text.addClass("hidden");
            highlight(property.element);
        });

        no.click(function() {
            text.removeClass("hidden");
            confirm.addClass("hidden");
        });

        yes.click(function() {
            highlight(property.element);
            SideBar.reset();
            $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
            Layouter.removeBlock(property);
        })

        remove.append(body.append(text.append(button)).append(confirm));

        remove.mouseenter(function() {
            highlight(property.element);
        })
        //$("#" + Constants.SIDEBAR_STYLE_ID).prepend(remove);
        $("#inbetween").prepend(remove);
    };

    this.addUIForProperty = function(windowId, element, html) {
        var config = SideBar.getWindowForId(windowId, element, null);
        var content = config.children(".panel-body");
        content.append(html);
    };

    this.createWindow = function(type, property, title)
    {
        var windowId = Plugin.makeid();
        if (configPanels[type] == null) configPanels[type] = {};
        var panels = configPanels[type];

        if (panels[windowId] == null) {

            var div = $("<div class='panel panel-default'/>");
            var header = $("<div class='panel-heading'>" + title + "</div>");
            var content = $("<div class='panel-body'/>");
            div.append(header).append(content);

            panels[windowId] = div;
            if (type == Constants.STYLE) {
                $("#" + Constants.SIDEBAR_STYLE_ID).append(div);
            } else if (type == Constants.CONTENT) {
                $("#" + Constants.SIDEBAR_CONTENT_ID).append(div);
            }

            div.mouseenter(function() {
                highlight(property.element);
            });
        }
        return windowId
    }

    this.getWindowForId = function(id) {
        var retVal = null;

        if (configPanels[Constants.STYLE] != null) {
            var panels = configPanels[Constants.STYLE];
            retVal = panels[id];
        }

        if (retVal == null && configPanels[Constants.CONTENT] != null) {
            var panels = configPanels[Constants.CONTENT];
            retVal = panels[id];
        }

        return retVal;
    }

    this.addUniqueClass = function(windowId, element, label, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addUniqueClass(element, label, values));
    };

    this.addOptionalClass = function(windowId, element, label, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addOptionalClass(element, label, values));
    };

    this.addUniqueAttributeValue = function(windowId, element, label, name, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addUniqueAttributeValue(element, label, name, values));
    };

    this.addUniqueAttribute = function(windowId, element, label, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addUniqueAttribute( element, label, values));
    };

    this.addValueAttribute = function(windowId, element, label, name, confirm) {
        SideBar.addUIForProperty(windowId, element, Plugin.addValueAttribute(element, label, name, confirm));
    };

    this.addValueHtml = function(windowId, element, label, confirm) {
        SideBar.addUIForProperty(windowId, element, Plugin.addValueHtml(element, label, confirm));
    };

    // PRIVATE



    var highlight = function(element) {
        element.addClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        element.addClass(Constants.HIGHLIGHT_CLASS);
        setTimeout(function(){
            element.removeClass(Constants.HIGHLIGHT_CLASS);
            setTimeout(function(){
                element.removeClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
            }, 400);
        }, 400);
    }

}]);
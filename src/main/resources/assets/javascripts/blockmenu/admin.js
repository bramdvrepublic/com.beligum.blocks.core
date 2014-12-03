/**
 * Created by wouter on 26/11/14.
 */

blocks.plugin("blocks.core.Admin", ["blocks.core.BlockMenu", "blocks.core.Overlay", "blocks.core.Broadcaster", function(Menu, Overlay, Broadcaster) {

    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-edit"></i></div>');

    var registeredPlugins = [];
    var selectedPlugin = null;

    var enabled = function(block) {
        var retVal = false;
        for (var i=0; i < registeredPlugins.length; i++) {
            var plugin = registeredPlugins[i];
            retVal = plugin.enabled(block);
            if (retVal) {
                selectedPlugin = plugin;
                break;
            }
        }
        if(!retVal) selectedPlugin = null;
        return retVal;
    };

    Menu.addButton({
        element: button,
        priority: 100,
        enabled: enabled
    })

    var makeAbsolute = function(element, width, height) {
        element.css("width", width);
        element.css("height", height);
        element.css("position", "absolute");
        element.css("top", "0px");
        element.css("left", "0px");
    };

    var makeNotAbsolute = function(element) {
        element.css("width", "");
        element.css("height", "");
        element.css("position", "");
        element.css("top", "");
        element.css("left", "");
    };

    var deleteTransition = function(element) {
        element.css("-webkit-transform", "");
        element.css("transform", "");
        element.css("-webkit-transition", "");
        element.css("transition", "");
        element.css("-webkit-transform-style", "");
        element.css("transform-style", "");
    }

    button.click(function() {
        // insert html
        // store the current block before we deactivate the mouse
        var block = Menu.currentBlock();
        Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
        var element = block.element;
        var front = $("<div />");
        var back = $("<div />");
        var side = $("<div />");
        var master = $("<div />");
        var parent = element;
        var doCopy = true;
        if (element.siblings().length == 0) {
            doCopy = false;
            master = element.parent();
            parent = master.parent();
            front = element;
        }

        var width = (block.right - block.left);
        var height = block.bottom - block.top;

        back.css("-webkit-transform", "rotateY(-180deg) translateZ("+(width/2)+"px)");
        back.css("transform", "rotateY(-180deg) translateZ("+(width/2)+"px);");

//        back.css("-webkit-transform", "rotateY(-180deg) translateZ("+2+"px)");
//        back.css("transform", "rotateY(-180deg) translateZ("+(2)+"px);");
        back.css("background-color", "white");
        back.append($("<div/>").append(selectedPlugin.element));
        var okBtn = $('<button class="btn btn-default pull-right" >Ok</button>');
        back.append($("<div />").append(okBtn));
        back.addClass("admin-backend");
        makeAbsolute(back, width, height);

        okBtn.click(function() {
            selectedPlugin.callback(block, back);
            Overlay.removeOverlay();
        });

        side.css("-webkit-transform", "rotateY(-90deg) translateZ("+(width/2)+"px)");
        side.css("transform", "rotateY(-90deg) translateZ("+(width/2)+"px);");
        side.css("background-color", "#EEEEEE");
        makeAbsolute(side, width, height);
        var bear = $("<h1><center>Please wait ...</center></h1>")
        side.append(bear);

//        front.css("-webkit-transform", "translateZ("+(width/2)+"px);");
//        front.css("transform", "translateZ("+(width/2)+"px)");
        var frontback = Overlay.addBlockBackground(block).remove();
        frontback.css("-webkit-transform", "translateZ("+(0)+"px);");
        frontback.css("transform", "translateZ("+(0)+"px)");
        frontback.css("top", 0);
        frontback.css("left", 0);

        front.css("-webkit-transform", "translateZ("+(0)+"px);");
        front.css("transform", "translateZ("+(0)+"px)");
        makeAbsolute(front, width, height);

        master.css("-webkit-transition", "-webkit-transform 0.6s");
        master.css("transition", "transform 0.6s");
        master.css("-webkit-transform-style", "preserve-3d");
        master.css("transform-style", "preserve-3d");
        master.css("width", width);
        master.css("height", height);
        master.css("position", "relative");

        Overlay.createForBlock(block, function() {
            master.removeClass("flip");
            setTimeout(function() {
                makeNotAbsolute(front);
                makeNotAbsolute(master);
                deleteTransition(front);
                deleteTransition(master);
                master.css("z-index", "");
                front.css("z-index", "");
                back.remove();
                side.remove();
                frontback.remove();
                Broadcaster.send(new Broadcaster.EVENTS.DOM_DID_CHANGE());
                Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());
            }, 600);
        });

        var zindex = Overlay.maxIndex() + 1;
        master.css("z-index", (zindex + 2));
        frontback.css("z-index", (zindex + 3));
        front.css("z-index", (zindex + 4));
        side.css("z-index", (zindex + 5));
        back.css("z-index", (zindex + 6));
        if (doCopy) {
            var children = block.element.children().remove();
            front.append(children);
            master.append(front).append(frontback).append(side).append(back);
            parent.append(master);
        } else {
            master.append(frontback);
            master.append(side);
            master.append(back);
        }
//
//        parent.css("-webkit-perspective", "1400px;");
//        parent.css("-webkit-perspective-origin", "50% 200px;");
        setTimeout(function() {master.addClass("flip");}, 100);

        // turn block around
        //

    });




    /*
    * Register with object with properties:
    *   - enabled(block) = function that returns true when backend is enabled for this block
    *   - callback(block) = function that is called when the button is pressed
    *   - html = element to insert as backend
    *   When 2 plugins register for the same block, the first will win.
    * */
    this.register = function(plugin) {
        if (plugin.enabled == null) plugin.enabled = function() {return false;};
        if (plugin.callback == null) plugin.callback = function() {};
        // TODO check for string
        if (plugin.element == null) plugin.element = "";
        registeredPlugins.push(plugin);
    }

    // Cleanup nethod, to be called by the plugin
    this.finish = function() {

    }

}]);
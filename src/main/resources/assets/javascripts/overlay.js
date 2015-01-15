blocks.plugin("blocks.core.Overlay", ["blocks.core.Constants", "blocks.core.Broadcaster", function(Constants, Broadcaster) {


    var maxIndex = function() {
        return Math.max.apply(null,$.map($('body  *'), function(e,n){
                if($(e).css('position')=='absolute' || $(e).css('position')=='relative')
                    return parseInt($(e).css('z-index'))||1 ;
            })
        );
    };

    this.maxIndex = maxIndex;

    var highlightBorder = {
        0: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS),
        1: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS),
        2: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS),
        3: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS)
    };



    this.highlightBlock = function(block) {
        var borderWidth = 1
        highlightBorder[0].css("left", block.left + "px");
        highlightBorder[0].css("top", block.top + "px");
        highlightBorder[0].css("width", (block.right - block.left) + "px");
        highlightBorder[0].css("height", (borderWidth) + "px");

        highlightBorder[1].css("left", block.left + "px");
        highlightBorder[1].css("top", block.bottom + "px");
        highlightBorder[1].css("width", (block.right - block.left) + "px");
        highlightBorder[1].css("height", (borderWidth) + "px");

        highlightBorder[2].css("left", block.left + "px");
        highlightBorder[2].css("top", block.top + "px");
        highlightBorder[2].css("width", (borderWidth) + "px");
        highlightBorder[2].css("height", (block.bottom - block.top) + "px");

        highlightBorder[3].css("left", block.right + "px");
        highlightBorder[3].css("top", block.top + "px");
        highlightBorder[3].css("width", (borderWidth) + "px");
        highlightBorder[3].css("height", (block.bottom - block.top) + "px");
        $("." + Constants.BLOCK_HOVER_CLASS).show();
    };

    this.unhighlightBlock = function(block) {
        $("." + Constants.BLOCK_HOVER_CLASS).hide();
    };

    this.highlightProperty = function(property) {
        property.element.addClass(Constants.PROPERTY_HOVER_CLASS);
    };

    this.unhighlightProperty = function(property) {
        property.element.removeClass(Constants.PROPERTY_HOVER_CLASS);
    };

    this.highlightElementAsProperty = function(element) {
        element.addClass(Constants.PROPERTY_HOVER_CLASS);
    };

    this.unhighlightElementAsProperty = function(element) {
        element.removeClass(Constants.PROPERTY_HOVER_CLASS);
    };

//    this.createForBlock = function (block, callback) {
//        var overlayElement = $("<div>").addClass(Constants.OVERLAY_CLASS);
//        var zindex = maxIndex() + 1;
//        overlayElement.css("z-index", zindex + 1);
////        var overlay = {overlayElement: overlayElement, element: block.element, position: $(block.element).css("position")};
////        $(block.element).css("z-index", zindex + 3);
////        if ($(block.element).css("position") != "relative") {
//
////        }
//        $(block.element).css("z-index", zindex + 3);
//        $(block.element).css("position", "relative");
//        $(block.element).css("box-shadow", "-1000px -1000px 5000px 5000px rgba(255,255,255, 0.7)");
//        $(block.element).before(overlayElement);
//
//        var removeOverlay = function() {
//            $(block.element).css("z-index", "");
//            $(block.element).css("position", "");
//            $(block.element).css("box-shadow", "none");
//            overlayElement.remove();
//            if (callback!= null) callback();
//        };
//        this.removeOverlay = removeOverlay;
//
//        overlayElement.on("click", function(event) {
//            event.preventDefault();
//            event.stopPropagation();
//            removeOverlay();
//        })
//    }

    this.overlayForElement = function (element, callback) {
        var ol = $("<div />").addClass(Constants.OVERLAY_CLASS);
        ol.attr("style", "position: absolute; top: 0px; left: 0px; position: fixed; top: 0px; bottom: 0px; left: 0px; right: 0px; background-color: rgba(0,0,0,0.8); z-index: 9000");

        var oldElement = element;

        var oldStyle = null;
        if (oldElement.hasAttribute('style')) oldStyle = oldElement.attr('style');
        oldElement.css("visibility", "hidden");
        var clonedElement = oldElement.clone();
        clonedElement.attr("style", "position: absolute; z-index: 9001; left: " + element.offset().left +"px; top: "+ element.offset().top+"px; width:" + (element.width()) + "px");

        $("body").append(ol).append(clonedElement);

        ol.on("click", function () {

            ol.remove();
            clonedElement.remove();
            oldElement.css("visibility", "visible");
            if (oldStyle != null) {
                clonedElement.attr("style", oldStyle);
            } else {
                clonedElement.removeAttr("style");
            }
            oldElement.replaceWith(clonedElement);
            callback();

        });

        return clonedElement;

    };


    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function() {
        for (var i = 0; i < 4; i++) {
            $("body").append(highlightBorder[i].remove());
        }
    })

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function() {
        for (var i = 0; i < 4; i++) {
            highlightBorder[i].remove();
        }
    })


}]);
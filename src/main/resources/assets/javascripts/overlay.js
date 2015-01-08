blocks.plugin("blocks.core.Overlay", ["blocks.core.Constants", function(Constants) {


    var maxIndex = function() {
        return Math.max.apply(null,$.map($('body  *'), function(e,n){
                if($(e).css('position')=='absolute' || $(e).css('position')=='relative')
                    return parseInt($(e).css('z-index'))||1 ;
            })
        );
    };

    this.maxIndex = maxIndex;
    var overlayStack = [];
    var highlightBackground = null;

    this.highlightBlock = function(block) {
        if (highlightBackground != null) highlightBackground.remove();
//        highlightBackground = addBlockBackground(block);
//        highlightBackground.addClass(Constants.BLOCK_HOVER_CLASS);
        block.element.addClass(Constants.BLOCK_HOVER_CLASS);
//        var zindex = maxIndex() + 1;
//        block.element.css("position", "relative");
//        block.element.css("z-index", zindex);
    };

    this.unhighlightBlock = function(block) {
        if (highlightBackground != null) highlightBackground.remove();
        block.element.removeClass(Constants.BLOCK_HOVER_CLASS)
//        block.element.css("position", "");
//        block.element.css("z-index", "");
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

//    var addBlockBackground = function(block) {
//        var overlaybackground = $("<div>").addClass(Constants.OVERLAY_BACKGROUND_CLASS);
//        var zindex = maxIndex() + 1;
//        overlaybackground.css("width", (block.right - block.left));
//        overlaybackground.css("height", (block.bottom - block.top));
//        overlaybackground.css("position", "absolute");
//        overlaybackground.css("top", block.top);
//        overlaybackground.css("left", block.left);
//
//        $("body").append(overlaybackground);
//        return overlaybackground;
//    };

    this.createForBlock = function (block, callback) {
        var overlayElement = $("<div>").addClass(Constants.OVERLAY_CLASS);
        var zindex = maxIndex() + 1;
        overlayElement.css("z-index", zindex + 1);
//        var overlay = {overlayElement: overlayElement, element: block.element, position: $(block.element).css("position")};
//        $(block.element).css("z-index", zindex + 3);
//        if ($(block.element).css("position") != "relative") {

//        }
        $(block.element).css("z-index", zindex + 3);
        $(block.element).css("position", "relative");
        $(block.element).css("box-shadow", "-1000px -1000px 5000px 5000px rgba(255,255,255, 0.7)");
        $(block.element).before(overlayElement);

        var removeOverlay = function() {
            $(block.element).css("z-index", "");
            $(block.element).css("position", "");
            $(block.element).css("box-shadow", "none");
            overlayElement.remove();
            if (callback!= null) callback();
        };
        this.removeOverlay = removeOverlay;

        overlayElement.on("click", function(event) {
            event.preventDefault();
            event.stopPropagation();
            removeOverlay();
        })
    }

    this.createForElement = function (element, callback) {
//        var overlay = $("<div>").addClass(Constants.OVERLAY_CLASS);
//        var zindex = maxIndex() + 1;
//        element = $(element);
//        overlay.css("z-index", zindex + 1);
//        element.css("z-index", zindex + 3);
//        $(element).css("position", "relative");
//        element.css("box-shadow", "-1000px -1000px 5000px 5000px rgba(255,255,255, 0.7)");
//        $(element).before(overlay);

        var undoOverlay = function(event) {
            var e = element;
            var x1 = e.offset().left;
            var x2 = x1 + e.width();
            var y1 = e.offset().top;
            var y2 = y1 + e.height();

            var cke = $(".cke");
            var a1 = 0; var a2 = 0; var b1 = 0; var b2 = 0;
            if (cke.length > 0) {
                var a1 = cke.offset().left;
                var a2 = a1 + e.width();
                var b1 = cke.offset().top;
                var b2 = b1 + e.height();
            }

            if (!((a1 < event.pageX && event.pageX < a2 && b1 < event.pageY && event.pageY < b2) ||
                (x1 < event.pageX && event.pageX < x2 && y1 < event.pageY && event.pageY < y2))) {
                event.preventDefault();
                event.stopPropagation();
                removeOverlay();
            }
        }

        var removeOverlay = function() {
//            $(element).css("z-index", "");
//            $(element).css("position", "");
//            $(element).css("box-shadow", "none");
//            overlay.remove();
            $(document).unbind("click", undoOverlay)
            if (callback!= null) callback();
        };
//        this.removeOverlay = removeOverlay;


        $(document).bind("click", undoOverlay)
    }



    var setAbsolute = function(block, relative) {
        var zindex = maxIndex() + 1;
        block.element.css("z-index", zindex);

        block.element.css("width", (block.right - block.left));
        block.element.css("height", (block.bottom - block.top));
        if (!relative) {
            block.element.css("position", "absolute");
            block.element.css("top", block.top);
            block.element.css("left", block.left);
        } else {
            block.element.css("position", "relative");
        }
    }

    var unsetAbsolute = function(block, relative) {
        var zindex = maxIndex() + 1;
        block.element.css("z-index", "");
        block.element.css("position", "");
        block.element.css("width", "");
        block.element.css("height", "");
        block.element.css("top", "");
        block.element.css("left","");
    }



}]);
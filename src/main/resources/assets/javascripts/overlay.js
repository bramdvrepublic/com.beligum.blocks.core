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
        highlightBackground = addBlockBackground(block);
        highlightBackground.addClass(Constants.BLOCK_HOVER_CLASS);
        var zindex = maxIndex() + 1;
        block.element.css("position", "relative");
        block.element.css("z-index", zindex);
    };

    this.unhighlightBlock = function(block) {
        if (highlightBackground != null) highlightBackground.remove();
        block.element.removeClass(Constants.BLOCK_HOVER_CLASS)
        block.element.css("position", "");
        block.element.css("z-index", "");
    };

    var addBlockBackground = function(block) {
        var overlaybackground = $("<div>").addClass(Constants.OVERLAY_BACKGROUND_CLASS);
        var zindex = maxIndex() + 1;
        overlaybackground.css("width", (block.right - block.left));
        overlaybackground.css("height", (block.bottom - block.top));
        overlaybackground.css("position", "absolute");
        overlaybackground.css("top", block.top);
        overlaybackground.css("left", block.left);

        $("body").append(overlaybackground);
        return overlaybackground;
    }

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
        var overlay = $("<div>").addClass(Constants.OVERLAY_CLASS);
        var zindex = maxIndex() + 1;
        element = $(element);
        overlay.css("z-index", zindex + 1);
        element.css("z-index", zindex + 3);
        $(element).css("position", "relative");
        element.css("box-shadow", "-1000px -1000px 5000px 5000px rgba(255,255,255, 0.7)");
        $(element).before(overlay);


        var removeOverlay = function() {
            $(element).css("z-index", "");
            $(element).css("position", "");
            $(element).css("box-shadow", "none");
            overlay.remove();
            if (callback!= null) callback();
        };
        this.removeOverlay = removeOverlay;

        overlay.on("click", function(event) {
            event.preventDefault();
            event.stopPropagation();
            removeOverlay();
        })
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
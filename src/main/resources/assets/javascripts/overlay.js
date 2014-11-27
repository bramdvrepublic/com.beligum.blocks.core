blocks.plugin("blocks.core.Overlay", ["blocks.core.Constants", function(Constants) {


    var maxIndex = function() {
        return Math.max.apply(null,$.map($('body > *'), function(e,n){
                if($(e).css('position')=='absolute')
                    return parseInt($(e).css('z-index'))||1 ;
            })
        );
    };

    this.maxIndex = maxIndex;
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

    this.createForBlock = function (block, callback) {
        var overlay = $("<div>").addClass(Constants.OVERLAY_CLASS);
        var zindex = maxIndex() + 1;
        overlay.css("z-index", zindex + 1);
        var overlaybackground = addBlockBackground(block);
        overlaybackground.css("z-index", zindex + 2);
        $(block.element).css("z-index", zindex + 3);
        $(block.element).css("position", "relative");
        $(block.element).css("background-color", "white");
        $("body").append(overlay);



        var removeOverlay = function() {
            $(block.element).css("z-index", "");
            $(block.element).css("position", "");
            $(block.element).css("background-color", "");
            overlay.remove();
            overlaybackground.remove();
            if (callback!= null) callback();
        };
        this.removeOverlay = removeOverlay;

        overlay.on("click", function(event) {
            removeOverlay();
        })
    }

    var addBlockBackground = function(block) {
        var overlaybackground = $("<div>").addClass(Constants.OVERLAY_BACKGROUND_CLASS);
        overlaybackground.css("background-color", "white");
        var zindex = maxIndex() + 1;
        overlaybackground.css("width", (block.right - block.left));
        overlaybackground.css("height", (block.bottom - block.top));
        overlaybackground.css("position", "absolute");
        overlaybackground.css("top", block.top);
        overlaybackground.css("left", block.left);

        $("body").append(overlaybackground);
        return overlaybackground;
    }
    this.addBlockBackground = addBlockBackground;
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
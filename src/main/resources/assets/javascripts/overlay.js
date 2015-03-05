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
        0: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS).addClass("top"),
        1: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS).addClass("bottom"),
        2: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS).addClass("left"),
        3: $('<div />').addClass(Constants.BLOCK_HOVER_CLASS).addClass("right")
    };

    var hideAll = function(element) {
        if (element.prop("tagName") != "BODY") {
            var siblings = element.siblings().addClass("not-visible");
            hideAll(element.parent());
        }
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

    var outsideElement = function(element, event) {
        var retVal = false;
        if (event.pageX < element.offset().left ||  event.pageX > (element.offset().left + element.outerWidth()) ||
            event.pageY < element.offset().top ||  event.pageY > (element.offset().top + element.outerHeight())) {
            retVal = true;
        }
        return retVal;
    };

    var overlayList = [];

    this.overlayForElement = function (element, allowRemove, callback) {
        if (overlayList.length == 0 || overlayList[overlayList.length-1].element != element) {
            hideAll(element);
            overlayList.push({element: element, allowRemove: allowRemove, callback: callback});
        }
        return element;
    };


    $(document).on("mouseup.blocks_overlay", function(event) {
        if (overlayList.length > 0) {
            var overlayItem = overlayList.pop();

            if (outsideElement(overlayItem.element, event) && overlayItem.allowRemove(event)) {
                $(".not-visible").removeClass("not-visible");
                var overlayListLength = overlayList.length;
                if (overlayListLength > 0) {
                    hideAll(overlayList[overlayListLength-1].element);
                    Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, overlayList[overlayListLength-1].element);
                } else {
                    Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, null);
                }
                overlayItem.callback();
            } else {
                overlayList.push(overlayItem);
            }
        }
    });


    var Overlay = this;




}]);
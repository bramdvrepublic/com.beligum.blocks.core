blocks.plugin("blocks.core.Overlay", ["blocks.core.Constants", function(Constants) {


    var maxIndex = function() {
        return Math.max.apply(null,$.map($('body > *'), function(e,n){
                if($(e).css('position')=='absolute')
                    return parseInt($(e).css('z-index'))||1 ;
            })
        );
    };

    this.createForBlock = function (block, callback) {
        var overlay = $("<div>").addClass(Constants.OVERLAY_CLASS);
        $("body").append(overlay);
        var zindex = maxIndex() + 1;
        overlay.css("z-index", 601);
        $(block.element).css("z-index", 10000)
        $(block.element).css("position", "relative");

        var removeOverlay = function() {
            $(block.element).css("z-index", "");
            $(block.element).css("position", "");
            overlay.remove();
            if (callback!= null) callback();
        }

        overlay.on("click", function(event) {
            removeOverlay();
        })
    }



}]);
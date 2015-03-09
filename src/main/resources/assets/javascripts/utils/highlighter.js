/*
* For simple layout helpers
*
* - highlights blocks and properties on hover
*
*
* */

blocks.plugin("blocks.core.Highlighter", ["blocks.core.Layouter", "blocks.core.Broadcaster", "blocks.core.Constants", function(Layouter, Broadcaster, Constants) {
    var Highlighter = this;
    this.removePropertyOverlay = function() {
        $("." + Constants.PROPERTY_HOVER_CLASS).removeClass(Constants.PROPERTY_HOVER_CLASS);
    }

    this.showPropertyOverlay = function(property) {
        if (property != null) {
            property.element.addClass(Constants.PROPERTY_HOVER_CLASS);
        }
    };

    this.removeBlockOverlay = function() {
        $("." + Constants.BLOCK_HOVER_CLASS).removeClass(Constants.BLOCK_HOVER_CLASS);
    };

    this.showBlockOverlay = function(block) {
        if (block != null) {
            block.element.addClass(Constants.BLOCK_HOVER_CLASS);
        }
    }



}]);
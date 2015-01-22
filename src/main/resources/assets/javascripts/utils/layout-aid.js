/*
* For simple layout helpers
*
* - show frame of all blocks on hoover.
*
*
* */

blocks.plugin("blocks.core.Highlighter", ["blocks.core.Layouter", "blocks.core.Broadcaster", "blocks.core.Elements", "blocks.core.Constants", "blocks.core.Overlay", "blocks.core.BlockMenu", function(Layouter, Broadcaster, Elements, Constants, Overlay, BlockMenu) {
    var Highlighter = this;
    this.removePropertyOverlay = function() {
        $("." + Constants.PROPERTY_CLASS).removeClass(Constants.PROPERTY_CLASS);
    }

    this.showPropertyOverlay = function(property) {
        if (property != null) {
            property.element.addClass(Constants.PROPERTY_CLASS);
        }
    };

    this.removeBlockOverlay = function() {
        $("." + Constants.BLOCK_CLASS).removeClass(Constants.BLOCK_CLASS);
    };

    this.showBlockOverlay = function(block) {
        if (block != null) {
            block.element.addClass(Constants.BLOCK_CLASS);
        }
    }



}]);
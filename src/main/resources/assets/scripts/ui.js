base.plugin("blocks.core.UI", ["constants.base.core", "constants.blocks.core", function (BaseConstants, BlocksConstants)
{
    var UI = this;

    this.init = function (options)
    {
        UI.sidebar = $('.' + BlocksConstants.PAGE_SIDEBAR_CLASS);
        UI.newBlockBtn = $('.' + BlocksConstants.CREATE_BLOCK_CLASS);
    };

}]);
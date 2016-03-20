base.plugin("blocks.core.UI", ["constants.base.core.internal", "constants.blocks.core", function (BaseConstantsInternal, BlocksConstants)
{
    var UI = this;

    this.init = function (options)
    {
        UI.sidebar = $('.' + BlocksConstants.PAGE_SIDEBAR_CLASS);
        UI.newBlockBtn = $('.' + BlocksConstants.CREATE_BLOCK_CLASS);
    };

}]);
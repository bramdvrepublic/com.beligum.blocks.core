/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.imports.BlocksCarousel", ["base.core.Class", function (Class)
{
    var BlocksCarousel = this;

    //init the carousels on the page
    $(document).ready(function ()
    {
        $('.carousel').carousel({});
    });

}]);
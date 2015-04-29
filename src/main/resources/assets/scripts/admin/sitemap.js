/**
 * Created by bas on 18.03.15.
 */
$(document).on("click", ".panel-heading", function ()
{
    var glyphiconUp = $(this).find(".glyphicon-chevron-up");
    var glyphiconDown = $(this).find(".glyphicon-chevron-down");
    if (glyphiconUp) {
        glyphiconUp.removeClass("glyphicon-chevron-up");
        glyphiconUp.addClass("glyphicon-chevron-down");
    }
    if (glyphiconDown) {
        glyphiconDown.removeClass("glyphicon-chevron-down");
        glyphiconDown.addClass("glyphicon-chevron-up");
    }
});


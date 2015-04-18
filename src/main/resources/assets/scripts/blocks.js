
$(document).on("click", ".smaller", function(e) {
    var size = parseInt($("body").css("font-size"));
    if (size > 3) size -= 1;
    $("body").css("font-size", size + "px");
    e.preventDefault();

});

$(document).on("click", ".larger", function(e) {
    var size = parseInt($("body").css("font-size"));
    if (size < 30) size += 1;
    $("body").css("font-size", size + "px");
    e.preventDefault();
});


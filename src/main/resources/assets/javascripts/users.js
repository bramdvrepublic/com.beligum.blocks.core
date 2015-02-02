jQuery(document).ready(function($) {
    $(".red-table tr").click(function() {
        window.document.location = $(this).attr("href");
    });
});
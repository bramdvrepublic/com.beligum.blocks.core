jQuery(document).ready(function($) {
    $(".red-table tr td").click(function() {
        if ($(this).attr("href")) {
            window.document.location = $(this).attr("href");
        }
    });
    $(".red-table tr").hover(
        function onEntry() {
            $(this).addClass("hover");
        },
        function out() {
            $(this).removeClass("hover");
        }
    );
    $(".red-table tr a").hover(
        function onEntry() {
            $(this).addClass("hover");
            $(".red-table tr").removeClass("hover");
            $(".red-table tr").addClass("nohover");
        },
        function out() {
            $(this).removeClass("hover");
            $(this).parentsUntil(".red-table").addClass("hover");
            $(".red-table tr").removeClass("nohover");
        }
    );
    $(".red-table tbody tr a").click(function() {
        $.ajax({
            url: $(this).attr("url"),
            type: 'DELETE'
        });
    });
});
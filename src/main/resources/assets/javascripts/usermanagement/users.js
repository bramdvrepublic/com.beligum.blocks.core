function queryParam(paramName) {
    var query = window.location.search.split("?");
    if (query.length > 1) {
        var paramPairs = query[1].split("&");
        var i;
        for (i = 0; i < paramPairs.length; i++) {
            var paramPair = paramPairs[i];
            var pair = paramPair.split("=");
            if (pair[0] == paramName) {
                return pair[1];
            }
        }
    }
    return null;
}

var showInactive = queryParam("inactive");

function changeActiveView(){
    showInactive = !showInactive;
    if(showInactive) {
        $("#changeActiveView").text("Hide inactive");
        $(".inactive").show();
    }
    else{
        $("#changeActiveView").text("Show inactive");
        $(".inactive").hide();
    }
    var sortingLinks = $(".sortlink");
    var i;
    for(i = 0; i<sortingLinks.length; i++){
        var sortLink = sortingLinks[i];
        sortLink.href ="/users?sort=" + sortLink.name + "&inactive="+showInactive;
    }
}

jQuery(document).ready(function($) {
    $(".red-table tr td").click(function() {
        if ($(this).attr("href")) {
            window.document.location = $(this).attr("href");
        }
    });

    //all beneath implement the hover-behaviour for a table-line, making sure the trash-icon has it's own proper behaviour
    $(".red-table tr").hover(
        function onEntry() {
            var trashIconHovered = $(this).children(".delete").children(".hover");
            if(!trashIconHovered.length>0){
                $(this).addClass("hover");
            }
            $(this).select(".delete").addClass("nohover");
        },
        function out() {
            $(this).removeClass("hover");
            $(this).select(".delete").removeClass("nohover");
        }
    );
    $(".red-table tr .delete").hover(
        function onEntry() {
            $(".red-table tr").removeClass("hover");
            $(".red-table tr").addClass("nohover");
            $(this).children().removeClass("nohover");
            $(this).children().addClass("hover");
        },
        function out() {
            $(this).parentsUntil(".red-table").addClass("hover");
            $(".red-table tr").removeClass("nohover");
            $(this).children().addClass("nohover");
            $(this).children().removeClass("hover");
        }
    );
});


function deleteUser(userId){
    $.ajax({
        url: "/users/" + userId,
        type: 'delete',
        success: function(response){
            var modalData = {
                bodyText: response ? response : "The user has been deleted.",
                onConfirm: "toUsersIndex()"
            };
            MODALS.show(MODALS.SUCCESS, modalData);
        },
        error: function(response){
            var modalData = {
                bodyText: response.status == 403 ? response.responseText : "An error occurred while deleting the user."
            };
            MODALS.show(MODALS.ERROR, modalData);
        }
    });
}

function toUsersIndex(){
    window.location = "/users";
}
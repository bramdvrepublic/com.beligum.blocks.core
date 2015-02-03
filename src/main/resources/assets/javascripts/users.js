function queryParam(paramName){
    var query = window.location.search.split("?");
    var paramPairs = query[1].split("&");
    var i;
    for(i = 0; i<paramPairs.length; i++){
        var paramPair = paramPairs[i];
        var pair = paramPair.split("=");
        if(pair[0] == paramName){
            return pair[1];
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
    $(".red-table tr").click(function() {
        if ($(this).attr("href")) {
            window.document.location = $(this).attr("href");
        }
    });
    if(!showInactive) {
        $(".inactive").hide();
    }
});
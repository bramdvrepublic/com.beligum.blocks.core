function save(){
    var html = document.documentElement.outerHTML;
    $.ajax({
        type: "PUT",
        data:  html,
        url: "/pages" + document.location.pathname
    });
}
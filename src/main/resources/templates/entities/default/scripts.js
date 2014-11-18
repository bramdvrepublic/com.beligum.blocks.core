function save(){
    var html = document.documentElement.outerHTML;
    $.ajax({
        type: "PUT",
        contentType: "application/json",
        data: JSON.stringify({
            html: html + "",
            entityClassName: "default"
        }),
        url: "/entities" + document.location.pathname
    });
}
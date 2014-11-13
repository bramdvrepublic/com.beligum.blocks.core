function save(){
    var html = document.documentElement.outerHTML;
    $.ajax({
        type: "PUT",
        data: {
            html: html,
            entityClassName: "default"
        },
        url: "/entities" + document.location.pathname
    });
}
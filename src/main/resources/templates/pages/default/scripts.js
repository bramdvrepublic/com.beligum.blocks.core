function save(){
    var html = document.documentElement.outerHTML;
    var node = document.doctype;
    //adding doctype-tag in a way compatible with all browsers
    html = "<!DOCTYPE "
        + node.name
        + (node.publicId ? ' PUBLIC "' + node.publicId + '"' : '')
        + (!node.publicId && node.systemId ? ' SYSTEM' : '')
        + (node.systemId ? ' "' + node.systemId + '"' : '')
        + '>' + html;
    $.ajax({
        type: "PUT",
        data:  html,
        url: "/pages" + document.location.pathname
    });
}
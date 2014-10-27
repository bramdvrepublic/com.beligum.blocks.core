function save(){
    var html = document.documentElement.outerHTML;
//    var node = document.doctype;
//    //adding doctype-tag in a way compatible with all browsers
//    //TODO BAS: stick DOCTYPE to html server-side (put it in PageClassCache)
//    html = "<!DOCTYPE "
//        + node.name
//        + (node.publicId ? ' PUBLIC "' + node.publicId + '"' : '')
//        + (!node.publicId && node.systemId ? ' SYSTEM' : '')
//        + (node.systemId ? ' "' + node.systemId + '"' : '')
//        + '>' + html;
    $.ajax({
        type: "PUT",
        data:  html,
        url: "/pages" + document.location.pathname
    });
}
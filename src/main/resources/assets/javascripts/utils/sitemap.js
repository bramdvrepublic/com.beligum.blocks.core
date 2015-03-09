/**
 * Created by bas on 09.03.15.
 */
blocks.plugin("blocks.core.Sitemap", ["blocks.core.Notification", function(Notification) {
    this.urlsModal = function (){
        $.ajax({
            type: 'GET',
            url: "/urls",
            dataType: "json"
        })
            .success(function(sitemap){
                var root = sitemap.root;
                var depth = 0;
                var html = renderHtmlForChildren(root);
                BootstrapDialog.show(
                    {
                        message: html,
                        title: "Choose an url."
                    }
                );
            })
            .error(function(response, textStatus, errorThrown){
                Notification.error("An error occurred while fetching the sitemap.");
            });
    };

    var renderHtmlForChildren = function(parentObject) {
        if (parentObject && parentObject.children && parentObject.children.length > 0) {
            var html = '<ul class="sitemap-list">';
            for (var i = 0; i < parentObject.children.length; i++) {
                var child = parentObject.children[i];
                html += '<li class="sitemap-item"><span>';
                html += child.relativePath;
                html +='</span>';
                html += renderHtmlForChildren(child);
                html += '</li>';
            }
            html += '</ul>';
            return html;
        }
        else{
            return '';
        }
    }



}]);
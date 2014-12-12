blocks.plugin("blocks.finder", [function() {
    var Finder = this;
    var WINDOW_CLASS = "finder-window";
    var MENU_CLASS = "finder-menu";
    var STATUSBAR_CLASS = "finder-statusbar";
    var CONTENT_CLASS = "finder-content";
    var CONTENT_SIDE_CLASS = "finder-content-sidebar";
    var CONTENT_MAIN_CLASS = "finder-content-main";
    var DIR_SIDE_CLASS = "directory-side";
    var DIR_COLLAPSE_ICON_CLASS = "dir-collapse-icon";
    var DIR_NAME_CLASS = "dir-name";
    var DIR_CHILDREN = "dir-children";

    var finder = $('<div/>').addClass(WINDOW_CLASS);
    var contentSide = $('<div/>').addClass(CONTENT_SIDE_CLASS);
    var contentMain = $('<div/>').addClass(CONTENT_MAIN_CLASS);
    var contentWindow = $('<div/>').addClass(CONTENT_CLASS);
    contentWindow.append(contentSide).append(contentMain);
    var menu = $('<div/>').addClass(MENU_CLASS);
    var statusBar = $('<div/>').addClass(STATUSBAR_CLASS);
    finder.append(menu).append(contentWindow).append(statusBar);

    var dirMap = {};

    this.show = function(element) {
        finder.remove();
        element.append(finder);
        initDirs(fs);
        showRoot();
    };

    var initDirs = function(dir) {
        if (dir == null || dir.children == null) return;
        var list = dir.children;
        for (var i=0; i < list.length; i++) {
            if (list[i].folder) {
                dirMap[list[i].id] = list[i];
                if (list[i].children != null && list[i].children.length > 0) {
                    initDirs(list[i]);
                }
            }
        }
    };

    var showRoot = function() {
        contentSide.empty()
        contentMain.empty();
        var tree = $("<ul/>").addClass("tree");
        var rootElement = directoryElement(root);
        collapseDirectory(rootElement);
        tree.append(rootElement);
        contentSide.append(tree);
    };

    /*
    * Fill directory element with child dirs in sidebar
    * */
    var fillSubTree = function(element, root) {
        for (var i = 0; i < root.children.length; i++) {
            element.append(directoryElement(root.children[i]));
        }
        return element;
    }

    var directoryElement = function(dir) {
        var dirElement = $('<li/>').attr("id", dir.id);
        dirElement.html(dir.name);
        return dirElement;
    };

    var appendFile = function(dir) {

    };

    /*
    * Collapse directory in sidebar and show/hide children
    * */
    var collapseDirectory = function(dirElement) {
        var id = dirElement.attr("id");
        var dir = dirMap[id];
        if (dirElement.hasClass("open")) {
            dirElement.removeClass("open");
        } else if (dir.children.length > 0) {
            var subTree = dirElement.children("ul");
            if (subTree.length != 1) {
                subTree.remove();
                subTree = $("<ul>");
                subTree = fillSubTree(subTree, dir);
                dirElement.append(subTree);
            }
            dirElement.addClass("open");
        } else {
            //no children so keep closed
        }
    };


    $(document).on("click", ".tree li", function(event) {
        collapseDirectory($(event.target));
        event.preventDefault();
        event.stopPropagation();
    });



    var fs = {name: "FS", id: "FS", folder: true, children:  [
        {name: "dir 1", id: "dir1", folder: true, children: [
            {name: "dir 100", id: "dir100", folder: true, children: [
                {name: "file 1", id: "file1", folder: false, children: []}
            ]},
            {name: "dir 2", id: "dir2", folder: true, children: [
                {name: "file 2", id: "file2", folder: false, children: []}
            ]},
            {name: "dir 3", id: "dir3", folder: true, children: []},
            {name: "dir 4", id: "dir4", folder: true, children: [
                {name: "file 3", id: "file3", folder: false, children: []},
                {name: "file 4", id: "file4", folder: false, children: []},
                {name: "dir 75", id: "file5", folder: true, children: [
                    {name: "file 3", id: "file15", folder: false, children: []},
                    {name: "file 4", id: "file16", folder: false, children: []}
                ]},
                {name: "file 6", id: "file6", folder: false, children: []}
            ]}
        ]},
        {name: "dir 5", id: "dir5", folder: true, children: [
            {name: "file 3", id: "file7", folder: false, children: []},
            {name: "file 4", id: "file8", folder: false, children: []},
            {name: "file 5", id: "file9", folder: false, children: []},
            {name: "file 6", id: "file10", folder: false, children: []}
        ]},
        {name: "dir 6", id: "dir6", folder: true, children: [
            {name: "file 3", id: "file11", folder: false, children: []},
            {name: "file 4", id: "file12", folder: false, children: []}
        ]},
        {name: "dir 7", id: "dir7", folder: true, children: [
            {name: "file 4", id: "file10", folder: false, children: []},
            {name: "file 4", id: "file11", folder: false, children: []},
            {name: "file 4", id: "file12", folder: false, children: []},
            {name: "file 3", id: "file13", folder: false, children: []},
            {name: "file 4", id: "file14", folder: false, children: []},
            {name: "file 3", id: "file15", folder: false, children: []},
            {name: "file 4", id: "file16", folder: false, children: []},
            {name: "file 3", id: "file17", folder: false, children: []},
            {name: "file 4", id: "file18", folder: false, children: []},
            {name: "file 3", id: "file19", folder: false, children: []},
            {name: "file 4", id: "file20", folder: false, children: []},
            {name: "file 4", id: "file21", folder: false, children: []},
            {name: "file 4", id: "file22", folder: false, children: []},
            {name: "file 3", id: "file23", folder: false, children: []},
            {name: "file 4", id: "file24", folder: false, children: []},
            {name: "file 3", id: "file25", folder: false, children: []},
            {name: "file 4", id: "file26", folder: false, children: []},
            {name: "file 3", id: "file27", folder: false, children: []},
            {name: "file 4", id: "file28", folder: false, children: []},
            {name: "file 3", id: "file29", folder: false, children: []},
            {name: "file 4", id: "file30", folder: false, children: []},
            {name: "file 4", id: "file31", folder: false, children: []},
            {name: "file 4", id: "file32", folder: false, children: []},
            {name: "file 3", id: "file33", folder: false, children: []},
            {name: "file 4", id: "file34", folder: false, children: []},
            {name: "file 3", id: "file35", folder: false, children: []},
            {name: "file 4", id: "file36", folder: false, children: []},
            {name: "file 3", id: "file37", folder: false, children: []},
            {name: "file 4", id: "file38", folder: false, children: []},
            {name: "file 3", id: "file39", folder: false, children: []},
            {name: "file 4", id: "file40", folder: false, children: []},
            {name: "file 4", id: "file41", folder: false, children: []},
            {name: "file 4", id: "file42", folder: false, children: []},
            {name: "file 3", id: "file43", folder: false, children: []},
            {name: "file 4", id: "file44", folder: false, children: []},
            {name: "file 3", id: "file45", folder: false, children: []},
            {name: "file 4", id: "file46", folder: false, children: []},
            {name: "file 3", id: "file47", folder: false, children: []},
            {name: "file 4", id: "file48", folder: false, children: []},
            {name: "file 3", id: "file49", folder: false, children: []},
        ]}
    ]};

    var root = fs;


}]);
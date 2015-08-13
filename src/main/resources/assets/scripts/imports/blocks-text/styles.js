/**
 * Created by bram on 8/13/15.
 */
/**
 * Created by wouter on 17/07/15.
 */

base.plugin("mot.site.styles", ["messages.blocks.core", function (Messages)
{
    //-----EDITOR STYLES-----
    //possible it's not loaded (eg. because the block wasn't used in the page)
    var MediumEditor = base.getPlugin("blocks.core.MediumEditor");
    if (MediumEditor) {
        // Styles is an array with objects
        // object is of type {value:"", text""}
        // value = "p:red" -> text before the colon is the tag, text after the colon are the classes that will be added
        // nothing after colon will remove allm classes, nothing before colon will not touch the tag
        // text is the text in the dropdown
        MediumEditor.setStylePickerStyles([

            {text: null, value: '<li class="dropdown-header">'+Messages.styles_sectionText+'</li>'},
            {text: Messages.styles_p, value: "p:"},
            {text: null, value: '<li role="separator" class="divider"></li>'},

            {text: null, value: '<li class="dropdown-header">'+Messages.styles_sectionTitles+'</li>'},
            {text: Messages.styles_h1, value: "h1:"},
            {text: Messages.styles_h2, value: "h2:"},
            {text: Messages.styles_h3, value: "h3:"},
            {text: null, value: '<li role="separator" class="divider"></li>'},
        ]);
    }
}]);
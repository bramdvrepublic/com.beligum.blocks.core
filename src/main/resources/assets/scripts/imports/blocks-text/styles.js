/**
 * Created by bram on 8/13/15.
 */
/**
 * Created by wouter on 17/07/15.
 */

base.plugin("blocks.core.MediumEditorStyles", ["messages.blocks.core", function (BlocksMessages)
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

            {text: null, value: '<li class="dropdown-header">' + BlocksMessages.widgetTextStyles_sectionTitles + '</li>'},
            {text: BlocksMessages.widgetTextStyles_h1, value: "h1:"},
            {text: BlocksMessages.widgetTextStyles_h2, value: "h2:"},
            {text: BlocksMessages.widgetTextStyles_h3, value: "h3:"},
            {text: null, value: '<li role="separator" class="divider"></li>'},

            {text: null, value: '<li class="dropdown-header">' + BlocksMessages.widgetTextStyles_sectionText + '</li>'},
            {text: BlocksMessages.widgetTextStyles_p, value: "p:"},
            {text: null, value: '<li role="separator" class="divider"></li>'},
        ]);
    }
}]);
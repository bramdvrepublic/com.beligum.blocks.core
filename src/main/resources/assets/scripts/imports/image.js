/*
 * Allows editing of an image when youy click on it
 * */
base.plugin("blocks.imports.Image", ["base.core.Class", "blocks.imports.Property", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.SidebarUtils",  function (Class, Property, BlocksConstants, BlocksMessages, Sidebar, SidebarUtils)
{
    var Image = this;
    this.TAGS = ["img"];

    (this.Class = Class.create(Property.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Image.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            Image.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            Image.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = Image.Class.Super.prototype.getConfigs.call(this, block, element);

            retVal.push(SidebarUtils.addValueAttribute(Sidebar, element, "Image url", "Paste or type an image link", "src", false, true, false));

            // if we click on a random IMG element (eg inside another block), don't show the bordered options,
            // because it won't work since the css styling for the .bordered class is wrapped in 'blocks-image'
            if (block.element.prop("tagName") == 'BLOCKS-IMAGE') {
                //note that we always add the config classes to the outer block (the template instance) to be as flexible as possible
                retVal.push(SidebarUtils.addUniqueClass(Sidebar, block.element, "Rand", [
                    {value: "bordered", name: "Met rand"},
                    {value: "", name: "Zonder rand"}
                ]));
            }

            retVal.push(this._getCreateLinkConfig(block, element));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetImageTitle;
        },

    })).register(this.TAGS);

}]);
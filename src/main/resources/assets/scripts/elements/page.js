/**
 * Created by wouter on 10/06/15.
 */

base.plugin("blocks.core.Elements.Page", ["base.core.Class", "constants.blocks.core", function (Class, Constants)
{
    blocks = window['blocks'] || {};
    // Region where templates can be dragged
    blocks.elements = blocks.elements || {};
    blocks.elements.Page = Class.create(blocks.elements.LayoutElement, {
        constructor: function ()
        {
            //super(this, element, parent, index)
            blocks.elements.Page.Super.call(this, $("." + Constants.PAGE_CONTENT_CLASS), null, 0);

            this.canDrag = false;
            this.blocks = [];
            // find everything that is a container or a template or a property
            this.overlay = null;
        },

        generateProperties: function (parent, index)
        {
            var children = parent.children();
            var childcount = children.length;
            for (var i = 0; i < childcount; i++) {
                var child = $(children[i]);
                if (child[0].tagName == "BLOCKS-LAYOUT") {
                    var b = new blocks.elements.Container($(child.children(".container")[0]), this, index);
                    this.children.push(b);
                    index++;
                } else if (child.hasAttribute("property")) {
                    var b = new blocks.elements.Property(child, this, index);
                    this.children.push(b);
                    index++;
                } else if (child[0].tagName.indexOf("-") > 0) {
                    var b = new blocks.elements.Block(child, this, index, false);
                    this.children.push(b);
                    index++;
                } else if (child.children.length > 0) {
                    this.generateProperties(child, index);
                }
            }
        },

        getLayoutContainer: function ()
        {
            var retVal = null;
            for (var i = 0; i < this.children.length; i++) {
                var child = this.children[i];
                if (child instanceof blocks.elements.Container) {
                    retVal = child;
                    break;
                }
            }
            return retVal;
        }
    });
}]);
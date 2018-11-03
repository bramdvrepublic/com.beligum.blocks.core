/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Region where templates can be dragged
 *
 * Created by wouter on 10/06/15.
 */
base.plugin("blocks.core.Elements.Page", ["base.core.Class", "constants.blocks.core", function (Class, Constants)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Page = Class.create(blocks.elements.LayoutElement, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            blocks.elements.Page.Super.call(this, null, $("." + Constants.PAGE_CONTENT_CLASS));

            //We manually call this for the top-level structure
            //to boot the entire subtree-generation
            this._buildSubmodel();
        },

        //-----IMPLEMENTED METHODS-----
        // _initChildren: function (parentEl, index)
        // {
        //     //Note: this will only iterate all immediate children
        //     //and recurse down (see below)
        //     var children = parentEl.children();
        //
        //     for (var i = 0; i < children.length; i++) {
        //         var child = $(children[i]);
        //         //if we hit the main layout tag, find it's container
        //         //TODO: we should change this to the data-property of the blocks-layout
        //         if (child[0].tagName.toLowerCase() == blocks.elements.LayoutElement.BLOCKS_LAYOUT_TAG) {
        //             //Note: this only searches one level down, maybe it should be altered to find().first(), but beware of crossing another template tag
        //             var containerEls = child.children("[property=" + blocks.elements.LayoutElement.CONTAINER_PROPERTY + "],[data-property=" + blocks.elements.LayoutElement.CONTAINER_PROPERTY + "]");
        //             if (containerEls.length > 1) {
        //                 Logger.warn('Encountered multiple container tags, only using first, please check this');
        //             }
        //
        //             this.children.push(new blocks.elements.Container(containerEls.first(), this, index));
        //             index++;
        //         }
        //         else if (child.hasAttribute("property") || child.hasAttribute("data-property")) {
        //             this.children.push(new blocks.elements.Property(child, this, index));
        //             index++;
        //         }
        //         else if (child[0].tagName.indexOf("-") > 0) {
        //             this.children.push(new blocks.elements.Block(child, this, index, false));
        //             index++;
        //         }
        //         //recurse if the child has children of its own and is not one of the above
        //         else if (child.children.length > 0) {
        //             this._initChildren(child, index);
        //         }
        //     }
        // },

        //-----PUBLIC METHODS-----
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
        },

        //-----PRIVATE METHODS-----
        _newChildInstance: function(element)
        {
            return new blocks.elements.Container(this, element);
        },
        _isAcceptableChild: function(element)
        {
            return element.hasClass('container');
        },
    });
}]);
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
                    var b = new blocks.elements.Container($(child.children("[property=container], [data-property=container]")[0]), this, index);
                    this.children.push(b);
                    index++;
                } else if (child.hasAttribute("property") || child.hasAttribute("data-property")) {
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
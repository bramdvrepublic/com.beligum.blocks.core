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

            //this will recursively trigger the entire sub-model
            this._buildPageModel(this, this.element.children());
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
        /**
         * Note: passing down a specific element (instead of using surface.element), allows us to
         * iterate further down in the same surface context, trying to find deeper sub-children to
         * connect to that parent surface context.
         */
        _buildPageModel: function (contextSurface, childElements)
        {
            for (var i = 0; i < childElements.length; i++) {

                var child = $(childElements[i]);

                //this will hold the 'parent' surface in which we're creating children
                var currentContextSurface = contextSurface;

                if (contextSurface instanceof blocks.elements.Page && child.hasClass('container')) {
                    Logger.info('container in page', child[0].outerHTML.split(child.html())[0]);
                    currentContextSurface = contextSurface._addChild(new blocks.elements.Container(contextSurface, child));
                }
                else if (contextSurface instanceof blocks.elements.Container && child.hasClass('row')) {
                    Logger.info('row in container', child[0].outerHTML.split(child.html())[0]);
                    currentContextSurface = contextSurface._addChild(new blocks.elements.Row(contextSurface, child));
                }
                else if (contextSurface instanceof blocks.elements.Row && child.is('[class*="col-"]')) {
                    Logger.info('column in row', child[0].outerHTML.split(child.html())[0]);
                    currentContextSurface = contextSurface._addChild(new blocks.elements.Column(contextSurface, child));
                }
                else if (contextSurface instanceof blocks.elements.Column && child[0].tagName.indexOf("-") > 0) {
                    Logger.info('block in column', child[0].outerHTML.split(child.html())[0]);
                    currentContextSurface = contextSurface._addChild(new blocks.elements.Block(contextSurface, child, true));
                }
                else if (contextSurface instanceof blocks.elements.Block && (child.hasAttribute("property") || child.hasAttribute("data-property"))) {
                    Logger.info('property in block', child[0].outerHTML.split(child.html())[0]);
                    currentContextSurface = contextSurface._addChild(new blocks.elements.Property(contextSurface, child));
                }

                //note: this means:
                // - we iterate depth-first
                // - we allow grandchildren to be part of the same context (eg. we support in-between divs)
                this._buildPageModel(currentContextSurface, child.children());
            }
        },

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
        /**
         * Add a container to this page
         * @param containerSurface
         * @private
         * @override
         */
        _addChild: function(containerSurface)
        {
            return blocks.elements.Page.Super.prototype._addChild.call(this, containerSurface);
        }

    });
}]);
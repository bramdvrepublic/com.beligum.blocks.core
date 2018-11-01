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
 * A container contains properties
 * A property can contain a new container itself to go up the tree
 *
 * Created by wouter on 5/03/15.
 */
base.plugin("blocks.core.Elements.Property", ["base.core.Class", "constants.base.core.internal", "constants.blocks.core", "blocks.core.DomManipulation", "base.core.Commons", function (Class, BaseConstantsInternal, BlocksConstants, DOM, Commons)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Property = Class.create(blocks.elements.LayoutElement, {

        //-----STATICS-----
        STATIC: {
            //will keep an index of all registered properties (to back-reference from their overlays)
            INDEX: {},
            OVERLAY_INDEX_ATTR: "data-property-index"
        },

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Property.Super.call(this, parentSurface, element);

            if (this.element.siblings().length == 0 && this.element.parent == parentSurface.element) {
                this.left = this.parent.left;
                this.right = this.parent.right;
                this.top = this.parent.top;
                this.bottom = this.parent.bottom;
            }

            this.canDrag = false;
            this.isField = !(this instanceof blocks.elements.Block);
            this.isTemplate = !this.isField;

            //we removed the max z-index application because eg. the notifications needs to come in front
            this.overlay = $("<div />")/*.css("z-index", DOM.getMaxZIndex())*/.addClass(BlocksConstants.SURFACE_ELEMENT_CLASS);
            if (this.isTemplate) {
                this.overlay.addClass(BlocksConstants.BLOCK_OVERLAY_CLASS);
            }
            else {
                this.overlay.addClass(BlocksConstants.PROPERTY_OVERLAY_CLASS);
            }

            //will be used to back-reference from the overlay to this object
            this.id = Commons.generateId();
            blocks.elements.Property.INDEX[this.id] = this;
            this.overlay.attr(blocks.elements.Property.OVERLAY_INDEX_ATTR, this.id);

            // Remove sides of layout lines to prevent overlap
            var block = this.parent.parent;
            if (!(this instanceof blocks.elements.Block) && block != null && block.overlay != null) {
                if (this._isNear(block.left, this.left)) {
                    this.overlay.addClass(blocks.elements.LayoutElement.LEFT_CLASS);
                }
                if (this._isNear(block.top, this.top)) {
                    this.overlay.addClass(blocks.elements.LayoutElement.TOP_CLASS);
                }
                if (this._isNear(block.right, this.right)) {
                    this.overlay.addClass(blocks.elements.LayoutElement.RIGHT_CLASS);
                }
                if (this._isNear(block.bottom, this.bottom)) {
                    this.overlay.addClass(blocks.elements.LayoutElement.BOTTOM_CLASS);
                }
            }
            else if (this instanceof blocks.elements.Block) {
                if (this.index == 0 && this.parent.parent.index == 0 && this.getContainer().parent != null && this.getContainer().parent.index > 0) {
                    this.overlay.addClass(blocks.elements.LayoutElement.TOP_CLASS);
                }
            }
        },

        //-----PUBLIC METHODS-----
        // Easily walk the tree and find the block that contains the coordinates
        findElements: function (minSearchLevel, maxSearchLevel)
        {
            //TODO this looks a lot like layout.findElements()...
            minSearchLevel = minSearchLevel == null ? 0 : minSearchLevel;
            maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
            var retVal = [];
            if (minSearchLevel <= 0) {
                retVal.push(this);
            }
            if (maxSearchLevel != 0) {
                for (var i = 0; i < this.children.length; i++) {
                    var props = this.children[i].findElements(minSearchLevel - 1, maxSearchLevel - 1);
                    for (var j = 0; j < props.length; j++) {
                        retVal.push(props[j]);
                    }
                }
            }

            return retVal;
        },

        isOuterTop: function ()
        {
            return this.element.prev().length == 0;
        },

        isOuterBottom: function ()
        {
            return this.element.next().length == 0;
        },

        getElementAtSide: function (side)
        {
            if (DOM.isColumn(this.element)) {
                if (side == BaseConstantsInternal.SIDE.LEFT) {
                    return this.getPrevious();
                }
                else if (side == BaseConstantsInternal.SIDE.RIGHT) {
                    return this.getNext();
                }
                else {
                    return null;
                }
            }
            else if (side == BaseConstantsInternal.SIDE.TOP) {
                return this.getPrevious();
            }
            else if (side == BaseConstantsInternal.SIDE.BOTTOM) {
                return this.getNext();
            }
            else {
                return null;
            }
        },

        //-----PRIVATE METHODS-----
        // //TODO refactor this (same code as in page.js)
        // _initChildren: function (parent, index)
        // {
        //     var children = parent.children();
        //
        //     for (var i = 0; i < children.length; i++) {
        //         var child = $(children[i]);
        //
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
        //         else if (child[0].tagName.indexOf("-") > 0) {
        //             this.children.push(new blocks.elements.Block(child, this, index, false));
        //             index++;
        //         }
        //         else if (child.children.length > 0) {
        //             this._initChildren(child, index);
        //         }
        //     }
        // },
        _isNear: function (one, two)
        {
            var retVal = false;
            var THRESHOLD = 1;
            if (Math.abs(one - two) <= THRESHOLD) {
                retVal = true;
            }
            return retVal;
        },

    });

}]);

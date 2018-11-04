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
 * Is the abstract class for DOM elements (row, container, block)
 * contains some helper functions
 *
 * Created by wouter on 5/03/15.
 */
base.plugin("blocks.core.Elements.LayoutElement", ["base.core.Class", "constants.base.core.internal", "blocks.core.DomManipulation", "constants.blocks.core", "blocks.core.Broadcaster", "blocks.core.Hover", function (Class, Constants, DOM, BlocksConstants, Broadcaster, Hover)
{
    var body = $("body");

    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.LayoutElement = Class.create(blocks.elements.Surface, {

        //-----STATICS-----
        STATIC: {
            BLOCKS_LAYOUT_TAG: 'blocks-layout',
            CONTAINER_PROPERTY: 'container',
            ROW_CLASS: 'row',
            LEFT_CLASS: 'left',
            RIGHT_CLASS: 'right',
            TOP_CLASS: 'top',
            BOTTOM_CLASS: 'bottom',

            ORIENTATION_NONE: 0,
            ORIENTATION_HORIZONTAL: 1,
            ORIENTATION_VERTICAL: 2,
        },

        //-----CONSTANTS-----

        //-----VARIABLES-----
        parent: undefined,
        index: undefined,
        element: undefined,
        children: undefined,
        resizeHandles: undefined,
        totalBlocks: undefined,
        canDrag: undefined,
        overlay: undefined,
        isTemplate: undefined,

        //-----CONSTRUCTORS-----
        /**
         * @param element The jQuery element bound to this surface
         * @param parentSurface The parent-surface or null if it doesn't have a parent
         * @param index The index of this child in it's parent's children array
         */
        constructor: function (parentSurface, element)
        {
            blocks.elements.LayoutElement.Super.call(this,
                this._calculateTop(element),
                this._calculateBottom(element),
                this._calculateLeft(element),
                this._calculateRight(element));

            this.element = element;
            this.parent = parentSurface;
            this.index = parentSurface ? parentSurface.children.length : 0;
            this.children = [];
            this.resizeHandles = [];
            this.totalBlocks = null;
            this.canDrag = false; // only for first level blocks inside a container
            this.overlay = null;
            this.isTemplate = false;

            //this._initChildren(this.element, 0);
        },

        //-----PUBLIC METHODS-----
        // Easily walk the tree and find the block that contains the coordinates
        findElements: function (minSearchLevel, maxSearchLevel)
        {
            minSearchLevel = minSearchLevel == null ? 0 : minSearchLevel;
            maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
            var retVal = [];
            for (var i = 0; i < this.children.length; i++) {
                var props = this.children[i].findElements(minSearchLevel, maxSearchLevel);
                for (var j = 0; j < props.length; j++) {
                    retVal.push(props[j]);
                }
            }

            return retVal;
        },
        // returns true if this block has no sibling on his left/right/top/bottom
        // to be overridden by subclasses
        isOuterLeft: function ()
        {
            return true
        },
        isOuterRight: function ()
        {
            return true
        },
        isOuterTop: function ()
        {
            return true
        },
        isOuterBottom: function ()
        {
            return true
        },
        isOuter: function (side)
        {
            if (side == Constants.SIDE.TOP) {
                return this.isOuterTop();
            }
            else if (side == Constants.SIDE.BOTTOM) {
                return this.isOuterBottom();
            }
            else if (side == Constants.SIDE.LEFT) {
                return this.isOuterLeft();
            }
            else if (side == Constants.SIDE.RIGHT) {
                return this.isOuterRight();
            }
        },

        // Get element at side, general function for getNext, getPrevious
        getElementAtSide: function (side)
        {
            if (side == Constants.SIDE.TOP || side == Constants.SIDE.LEFT) {
                return this.getPrevious();
            }
            else {
                return this.getNext();
            }
        },

        getNext: function ()
        {
            if (this.parent == null) {
                return null;
            }

            if (this.index + 1 < this.parent.children.length) {
                return this.parent.children[this.index + 1];
            }
            else {
                return null;
            }
        },
        getPrevious: function ()
        {
            if (this.parent == null) {
                return null;
            }
            if (this.index - 1 >= 0) {
                return this.parent.children[this.index - 1];
            }
            else {
                return null;
            }
        },

        // find most left and right column and use them to calculate the width of the parent
        getFullWidth: function ()
        {
            var retVal = 0;

            if (this.parent != null) {
                var outerleft = this;
                var outerright = this;
                while (!this.isOuterLeft() && this.parent != null) {
                    outerleft = this.parent;
                }
                while (!this.isOuterRight() && this.parent != null) {
                    outerright = this.parent;
                }
                retVal = outerright.right - outerleft.left;

            }
            else {
                retVal = this.right - this.left;
            }

            return retVal;
        },

        // find all dropspots for an element
        // is called for a block and returns all dropspots for this block and his parents.
        createAllDropspots: function ()
        {
            if (this instanceof blocks.elements.Block) {
                this.generateDropspots();
            }
            else {
                if (this.children.length > 0) {
                    for (var i = 0; i < this.children.length; i++) {
                        this.children[i].createAllDropspots();
                    }
                }
            }
            if (this.container != null) {
                this.container.createAllDropspots();
            }
        },

        getBlocks: function ()
        {
            if (this.totalBlocks != null) return this.totalBlocks;
            this.totalBlocks = 0;
            for (var i = 0; i < this.children.length; i++) {
                if (this.children[i] instanceof blocks.elements.Block) {
                    this.totalBlocks += 1;
                } else {
                    this.totalBlocks += this.children[i].getBlocks();
                }
            }
            return this.totalBlocks;
        },

        // Container is a LayoutElement without a parent
        getContainer: function ()
        {
            var parent = this.parent;
            while (parent != null && !(parent instanceof blocks.elements.Container)) {
                parent = parent.parent;
            }

            return parent;
        },

        showOverlay: function ()
        {
            if (this.overlay != null) {
                this.overlay.css("width", (this.right - this.left) + "px");
                this.overlay.css("height", (this.bottom - this.top) + "px");

                if (this.overlay.parent().length > 0) {
                    this.overlay.remove();
                }
                this.overlay.css("left", this.left + "px");
                this.overlay.css("top", this.top + "px");

                //TODO if we want to put them all in a wrapper element
                //var wrapper = $('.'+BlocksConstants.BLOCK_OVERLAYS_WRAPPER_CLASS);
                //if (wrapper.length==0) {
                //    wrapper = $("<div class='" + BlocksConstants.BLOCK_OVERLAYS_WRAPPER_CLASS + "' />").appendTo($('.'+BlocksConstants.PAGE_CONTENT_CLASS));
                //}
                //wrapper.append(this.overlay);
                body.append(this.overlay);

                //this only seems to work after the overlay has been added to the body
                //Note: difference between mouseenter and mouseover:
                // see http://jsfiddle.net/ZCWvJ/7/ (from http://stackoverflow.com/questions/7286532/jquery-mouseenter-vs-mouseover)
                // each time your mouse enters or leaves a child element, mouseover is triggered, but not mouseenter.
                var _this = this;
                this.overlay
                    .mouseenter(function (event)
                    {
                        $(this).addClass(BlocksConstants.OVERLAY_HOVER_CLASS);
                        Hover.setHoveredBlock(_this);
                        Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_OVERLAY, event, _this);
                    })
                    .mouseleave(function (event)
                    {
                        $(this).removeClass(BlocksConstants.OVERLAY_HOVER_CLASS);
                        //this might be troublesome: what if the event is processed after the mouseenter of the next block?
                        Hover.setHoveredBlock(null);
                        Broadcaster.send(Broadcaster.EVENTS.HOVER_LEAVE_OVERLAY, event, _this);
                    });
            }
        },

        removeOverlay: function ()
        {
            if (this.overlay != null) {
                this.overlay.remove();
            }
        },

        calculateDropspots: function (side, dropspots)
        {
            return [];
        },

        generateDropspots: function ()
        {
        },

        //-----PRIVATE METHODS-----
        /**
         * Build the sub-surface-model for this surface
         *
         * @private
         */
        _buildSubmodel: function ()
        {
            //Optimization: no need to search for sub-children if we can't have them
            if (this._canHaveChildren()) {

                //if a specific element is supplied, use that one
                this._findChildren(this.element);

                // Now iterate the layouted children again
                // to build their sub-models
                for (var i = 0; i < this.children.length; i++) {
                    //Note how we switch context to the child surface
                    this.children[i]._buildSubmodel();
                }
            }
        },
        /**
         * Iterate the element width-first and populate the this.children array
         *
         * @param parentElement
         * @private
         */
        _findChildren: function (parentElement)
        {
            var childElements = parentElement.children();

            // Main idea is to iterate width-first instead of depth-first,
            // because low-level surfaces depend on their parents to be
            // layouted correctly, before building the sub-model
            for (var i = 0; i < childElements.length; i++) {

                var childElement = $(childElements[i]);

                //Note: if we find an acceptable child, we stop recursion,
                //because we iterate width-first
                if (this._isAcceptableChild(childElement)) {
                    this.children.push(this._layoutChild(this._newChildInstance(childElement)));
                }
                // This recursion allows grandchildren to be part of
                // the same context (eg. we support in-between divs)
                else {
                    this._findChildren(childElement);
                }
            }
        },
        /**
         * Implement in subclasses: instantiate a child-surface for this parent surface
         * @param element
         * @returns {null}
         * @private
         */
        _newChildInstance: function (element)
        {
            return null;
        },
        /**
         * If this surface typ can't have children, overload and return false
         * @returns {boolean}
         * @private
         */
        _canHaveChildren: function ()
        {
            return true;
        },
        /**
         * If this surface accepts the element argument, return true (eg. a column in a row)
         * @param element
         * @returns {boolean}
         * @private
         */
        _isAcceptableChild: function (element)
        {
            return false;
        },
        /**
         * Adds a new child surface to this parent.
         * To be overloaded by subclasses for extra initializing.
         *
         * @param childSurface The surface to add to this parent
         * @returns The child surface for chaining
         * @protected
         */
        _layoutChild: function (childSurface)
        {
            switch (this._getChildOrientation()) {
                case blocks.elements.LayoutElement.ORIENTATION_VERTICAL:
                    this._layoutVerticalChild(childSurface);
                    break;
                case blocks.elements.LayoutElement.ORIENTATION_HORIZONTAL:
                    this._layoutHorizontalChild(childSurface);
                    break;
            }

            //for chaining
            return childSurface;
        },
        /**
         * Returns whether this children in this surface are layouted horizontally
         * or vertically or no specific orientation is known beforehand.
         *
         * @returns {number}
         * @private
         */
        _getChildOrientation: function ()
        {
            return blocks.elements.LayoutElement.ORIENTATION_NONE;
        },
        /**
         * Adds a child to this vertical-oriented parent surface
         * @param childSurface
         * @protected
         */
        _layoutVerticalChild: function (childSurface)
        {
            childSurface.left = this.left;
            childSurface.right = this.right;

            if (childSurface.index == 0) {
                childSurface.top = this.top;
            }

            // We should only sync the bounds of the last child,
            // but every added child will be last, so we'll just sync now
            // and revert the bounds of the previous one below
            childSurface.bottom = this.bottom;
            if (childSurface.index > 0) {
                //revert the bottom of the previous one if we're not the first
                var previousChild = this.children[childSurface.index - 1];
                //revert the bottom of the previous child
                previousChild.bottom = previousChild.realBottom;

                //glue the two children together
                var middle = Math.floor((previousChild.bottom + childSurface.top) / 2);
                previousChild.bottom = middle;
                childSurface.top = middle;
            }
        },
        /**
         * Adds a child to this horizontal-oriented parent surface
         * @param childSurface
         * @protected
         */
        _layoutHorizontalChild: function (childSurface)
        {
            childSurface.top = this.top;
            childSurface.bottom = this.bottom;

            if (childSurface.index == 0) {
                childSurface.left = this.left;
            }

            //See comments above
            childSurface.right = this.right;
            if (childSurface.index > 0) {
                //revert the bottom of the previous one if we're not the first
                var previousChild = this.children[childSurface.index - 1];
                previousChild.right = previousChild.realRight;

                var middle = Math.floor((previousChild.right + childSurface.left) / 2);
                previousChild.right = middle;
                childSurface.left = middle;
            }
        },
    });

}]);

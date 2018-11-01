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
                this.calculateTop(element),
                this.calculateBottom(element),
                this.calculateLeft(element),
                this.calculateRight(element));

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

        //-----ABSTRACT METHODS-----
        /**
         * Search the sub-surfaces of this surface
         * @param parentEl The parent jQuery element on which we'll iterate it's children
         * @param index The index of this surface in it's parent
         */
        // _initChildren: function (parentEl, index)
        // {
        // },

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
                this.overlay.mouseenter(function (event)
                {
                    $(this).addClass(BlocksConstants.OVERLAY_HOVER_CLASS);
                    Hover.setHoveredBlock(_this);
                    Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_OVERLAY, event, _this);
                }).mouseleave(function (event)
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
            if (this.overlay != null){
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
         * Adds a new child surface to this parent.
         * To be overloaded by subclasses for extra initializing.
         *
         * @param childSurface The surface to add to this parent
         * @returns The child surface for chaining
         * @protected
         */
        _addChild: function(childSurface)
        {
            this.children.push(childSurface);

            //for chaining
            return childSurface;
        },
        /**
         * Adds a child to this vertical-oriented parent surface
         * @param childSurface
         * @protected
         */
        _addVerticalChild: function(childSurface)
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
        _addHorizontalChild: function(childSurface)
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
        /**
         * Creates rows (if the flag is up) or blocks (if the flag is down) inside a parent (container or column)
         * and add them to the children array.
         */
        _generateVerticalChildren: function (rows)
        {
            var children = rows ? this.element.children("." + blocks.elements.LayoutElement.ROW_CLASS) : this.element.children();

            for (var i = 0; i < children.length; i++) {
                // create zone for child
                var child = $(children[i]);

                if (rows) {
                    this.children.push(new blocks.elements.Row(child, this, i));
                }
                else {
                    this.children.push(new blocks.elements.Block(child, this, i, true));
                }
            }
        },
        /**
         * Iterate the current children (which are expected to be rows or blocks)
         * and create or initialize their sub-children.
         */
        _fillVerticalChildren: function ()
        {
            for (var i = 0; i < this.children.length; i++) {

                var child = this.children[i];
                var first = i == 0;
                var last = (i + 1) == this.children.length;
                child.left = this.left;
                child.right = this.right;
                if (first) {
                    child.top = this.top;
                }

                //melt the edges with the parent or next because it feels
                //more natural
                if (last) {
                    child.bottom = this.bottom;
                }
                else {
                    var next = this.children[i + 1];
                    var middle = Math.floor((child.bottom + next.top) / 2);
                    next.top = middle;
                    child.bottom = middle;
                }

                if (child instanceof blocks.elements.Row) {
                    child._fillHorizontalChildren();
                }
                else if (child instanceof blocks.elements.Block) {
                    //these two classes will remove the borders left and top so we don't
                    //have double borders when two blocks are next to each other
                    if (this.index > 0) {
                        child.overlay.addClass(blocks.elements.LayoutElement.LEFT_CLASS);
                    }
                    if (i > 0 || this.parent.index > 0) {
                        child.overlay.addClass(blocks.elements.LayoutElement.TOP_CLASS);
                    }
                }
                else {
                    Logger.error('Encountered non-row and non-block as a vertical child, this shouldn\'t happen', child);
                }
            }
        },
        /**
         * Creates columns inside a row and add them to the children array.
         */
        _generateHorizontalChildren: function ()
        {
            // check only for columns
            var columns = this.element.children('[class*="col-"]');

            if (columns.length > 0) {
                var oldColumn = null;
                for (var i = 0; i < columns.length; i++) {
                    var col = $(columns[i]);

                    var newColumn = new blocks.elements.Column(col, this, i);
                    this.children.push(newColumn);

                    if (oldColumn != null) {
                        this.resizeHandles.push(new blocks.elements.ResizeHandle(oldColumn, newColumn));
                    }
                    oldColumn = newColumn;
                }
            }
        },
        _fillHorizontalChildren: function ()
        {
            for (var i = 0; i < this.children.length; i++) {
                var child = this.children[i];

                var last = (i + 1) == this.children.length;
                var first = i == 0;
                child.bottom = this.bottom;
                child.top = this.top;
                if (first) {
                    child.left = this.left;
                }
                if (last) {
                    child.right = this.right;
                }
                else {
                    var next = this.children[i + 1];
                    var middle = Math.floor((child.right + next.left) / 2);
                    next.left = middle;
                    child.right = middle;
                }
                if (child instanceof blocks.elements.Column) {
                    child._fillVerticalChildren();
                }
            }
        },

    });

}]);

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
 * An element with 4 corners.
 */
base.plugin("blocks.core.elements.Surface", ["base.core.Class", "base.core.Commons", "constants.base.core.internal", "constants.blocks.core", "messages.blocks.core", function (Class, Commons, Constants, BlocksConstants, BlocksMessages)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Surface = Class.create({

        //-----STATICS-----
        STATIC: {
            //will keep an index of all registered properties (to back-reference from their overlays)
            INDEX: {},
            INDEX_ATTR: "data-index",

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
        id: undefined,
        type: undefined,
        name: undefined,
        parent: undefined,
        index: undefined,
        element: undefined,
        children: undefined,
        resizeHandles: undefined,
        totalBlocks: undefined,
        canDrag: undefined,
        overlay: undefined,

        top: 0,
        bottom: 0,
        left: 0,
        right: 0,

        // The values above don't necessarily represent
        // the real bounds of this surface because we sometimes
        // sync them with the bounds of their parents for a more intuitive
        // look and feel. The values below hold the original bounds
        // that were used during construction, so we can reset the ones above.
        realTop: 0,
        realBottom: 0,
        realLeft: 0,
        realRight: 0,

        //-----CONSTRUCTORS-----
        /**
         * @param element The jQuery element bound to this surface
         * @param parentSurface The parent-surface or null if it doesn't have a parent
         */
        constructor: function (parentSurface, element)
        {
            //will be used to back-reference from the overlay to this object
            this.id = Commons.generateId();
            blocks.elements.Surface.INDEX[this.id] = this;

            this.type = this._getType();
            this.name = this._getName();
            this.element = element;
            this.parent = parentSurface;
            this.index = parentSurface ? parentSurface.children.length : 0;
            this.children = [];
            this.resizeHandles = [];
            this.totalBlocks = null;
            this.canDrag = false; // only for first level blocks inside a container
            this.overlay = null;

            //this allows us to call this constructor with no arguments
            if (element) {
                var top = this._calculateTop(element);
                var bottom = this._calculateBottom(element);
                var left = this._calculateLeft(element);
                var right = this._calculateRight(element);

                this.top = Math.min(top, bottom);
                this.bottom = Math.max(top, bottom);
                this.left = Math.min(left, right);
                this.right = Math.max(left, right);

                this.realTop = this.top;
                this.realBottom = this.bottom;
                this.realLeft = this.left;
                this.realRight = this.right;
            }
        },

        //-----PUBLIC METHODS-----
        /**
         * These are shortcut functions to detect which kind of surface we're dealing with.
         */
        isPage: function()
        {
            return this instanceof blocks.elements.Page;
        },
        isContainer: function()
        {
            return this instanceof blocks.elements.Container;
        },
        isRow: function()
        {
            return this instanceof blocks.elements.Row;
        },
        isColumn: function()
        {
            return this instanceof blocks.elements.Column;
        },
        isBlock: function()
        {
            return this instanceof blocks.elements.Block;
        },
        isProperty: function()
        {
            return this instanceof blocks.elements.Property;
        },

        /**
         * Returns the first child we can find where the bounds wrap the supplied coordinate
         * or null if no such child was found
         */
        childAt: function (x, y)
        {
            var retVal = null;

            for (var i = 0; !retVal && i < this.children.length; i++) {
                var child = this.children[i];
                if (child.isInside(x, y)) {
                    retVal = child;
                }
            }

            return retVal;
        },
        /**
         * Returns true of the supplied coordinate is inside this surface, it's bounds included
         */
        isInside: function (x, y)
        {
            return this.top <= y && y <= this.bottom && this.left <= x && x <= this.right;
        },

        //-----TODO UNCHECKED-----
        /**
         * Returns true if this surface has no sibling on the specified side
         * @param side
         * @returns {*|boolean}
         */
        isOuter: function (side)
        {
            if (side == Constants.SIDE.TOP) {
                return this._isOuterTop();
            }
            else if (side == Constants.SIDE.BOTTOM) {
                return this._isOuterBottom();
            }
            else if (side == Constants.SIDE.LEFT) {
                return this._isOuterLeft();
            }
            else if (side == Constants.SIDE.RIGHT) {
                return this._isOuterRight();
            }
        },
        /**
         * Returns the element at specified side, general function for getNext, getPrevious
         * @param side
         * @returns {*}
         */
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
                while (!this._isOuterLeft() && this.parent != null) {
                    outerleft = this.parent;
                }
                while (!this._isOuterRight() && this.parent != null) {
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
            if (this.isBlock()) {
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
                if (this.children[i].isBlock()) {
                    this.totalBlocks += 1;
                }
                else {
                    this.totalBlocks += this.children[i].getBlocks();
                }
            }
            return this.totalBlocks;
        },
        // Container is a LayoutElement without a parent
        getContainer: function ()
        {
            var parent = this.parent;
            while (parent != null && !parent.isContainer()) {
                parent = parent.parent;
            }

            return parent;
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
         * Returns the type of this class in a human readable string.
         * Overload in the subclasses.
         * @returns {string}
         * @private
         */
        _getType: function()
        {
            return 'surface';
        },
        /**
         * Returns the i18n name of this class.
         * Overload in the subclasses.
         * @returns {string}
         * @private
         */
        _getName: function()
        {
            return BlocksMessages.surfaceName;
        },
        /**
         * Build the sub-surface-model for this surface
         *
         * @private
         */
        _buildSubmodel: function ()
        {
            //Optimization: no need to search for sub-children if this subclass can't have them
            if (this._canHaveChildren()) {

                //if a specific element is supplied, use that one
                this._findChildren(this.element);

                // Now iterate the layouted children again to build their sub-models
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
                case blocks.elements.Surface.ORIENTATION_VERTICAL:
                    this._layoutVerticalChild(childSurface);
                    break;
                case blocks.elements.Surface.ORIENTATION_HORIZONTAL:
                    this._layoutHorizontalChild(childSurface);
                    break;
            }

            //for chaining
            return childSurface;
        },
        /**
         * Returns whether this children in this surface are layouted horizontally
         * or vertically or have no specific orientation
         *
         * @returns {number}
         * @private
         */
        _getChildOrientation: function ()
        {
            return blocks.elements.Surface.ORIENTATION_NONE;
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
                var middle = (previousChild.bottom + childSurface.top) / 2;
                previousChild.bottom = middle;
                childSurface.top = middle;

                previousChild._redraw();
            }

            childSurface._redraw();
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

                var middle = (previousChild.right + childSurface.left) / 2;
                previousChild.right = middle;
                childSurface.left = middle;

                previousChild._redraw();
            }

            childSurface._redraw();
        },
        _createOverlay: function ()
        {
            var retVal = $("<div />").addClass(BlocksConstants.SURFACE_ELEMENT_CLASS);

            //this will allow us to do a reverse lookup starting from the element
            retVal.attr(blocks.elements.Surface.INDEX_ATTR, this.id);

            if (this.canDrag) {
                retVal.addClass(BlocksConstants.BLOCK_DRAGGABLE_CLASS);
            }

            return retVal;
        },
        /**
         * Redraws the UI surface if we have one
         * @private
         */
        _redraw: function ()
        {
            if (this.overlay) {

                var top = this.top;
                var left = this.left;
                var width = this.right - this.left;
                var height = this.bottom - this.top;

                if (this.element && this.element.is(':visible') && width * height > 0) {
                    this.overlay.css("top", top + "px");
                    this.overlay.css("left", left + "px");
                    this.overlay.css("width", width + "px");
                    this.overlay.css("height", height + "px");
                    this.overlay.show();
                }
                else {
                    this.overlay.hide();
                }
            }
        },
        /**
         * Calculates the top value for the constructor of the supplied element
         */
        _calculateTop: function (element)
        {
            return element.offset().top
        },
        /**
         * Calculates the bottom value for the constructor of the supplied element
         */
        _calculateBottom: function (element)
        {
            return element.offset().top + element.outerHeight();
        },
        /**
         * Calculates the left value for the constructor of the supplied element
         */
        _calculateLeft: function (element)
        {
            return element.offset().left
        },
        /**
         * Calculates the right value for the constructor of the supplied element
         */
        _calculateRight: function (element)
        {
            return element.offset().left + element.outerWidth()
        },
        /**
         * Returns true if this block has no sibling on his left
         * (to be overridden by subclasses)
         * @returns {boolean}
         * @private
         */
        _isOuterLeft: function ()
        {
            return true
        },
        /**
         * Returns true if this block has no sibling on his right
         * (to be overridden by subclasses)
         * @returns {boolean}
         * @private
         */
        _isOuterRight: function ()
        {
            return true
        },
        /**
         * Returns true if this block has no sibling on his top
         * (to be overridden by subclasses)
         * @returns {boolean}
         * @private
         */
        _isOuterTop: function ()
        {
            return true
        },
        /**
         * Returns true if this block has no sibling on his bottom
         * (to be overridden by subclasses)
         * @returns {boolean}
         * @private
         */
        _isOuterBottom: function ()
        {
            return true
        },
    });

}]);

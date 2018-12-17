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
base.plugin("blocks.core.elements.Surface", ["base.core.Class", "base.core.Commons", "constants.base.core.internal", "constants.blocks.core", "messages.blocks.core", "blocks.core.UI", function (Class, Commons, Constants, BlocksConstants, BlocksMessages, UI)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    var _ORIENTATION_NONE = 0;
    var _ORIENTATION_VERTICAL = 1;
    var _ORIENTATION_HORIZONTAL = 2;

    var _SIDE_NONE = {
        id: 0,
        name: 'none',
        opposite: undefined,
        orientation: _ORIENTATION_NONE,
        cssClass: '',
    };
    var _SIDE_TOP = {
        id: 1,
        name: 'top',
        opposite: undefined,
        orientation: _ORIENTATION_VERTICAL,
        cssClass: 'top',
    };
    var _SIDE_RIGHT = {
        id: 2,
        name: 'right',
        opposite: undefined,
        orientation: _ORIENTATION_HORIZONTAL,
        cssClass: 'right',
    };
    var _SIDE_BOTTOM = {
        id: 3,
        name: 'bottom',
        opposite: undefined,
        orientation: _ORIENTATION_VERTICAL,
        cssClass: 'bottom',
    };
    var _SIDE_LEFT = {
        id: 4,
        name: 'left',
        opposite: undefined,
        orientation: _ORIENTATION_HORIZONTAL,
        cssClass: 'left',
    };
    _SIDE_TOP.opposite = _SIDE_BOTTOM;
    _SIDE_BOTTOM.opposite = _SIDE_TOP;
    _SIDE_RIGHT.opposite = _SIDE_LEFT;
    _SIDE_LEFT.opposite = _SIDE_RIGHT;

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

            //when we create new elements and don't specify the tag name,
            //this is the tag we'll use by default
            DEFAULT_TAG: 'div',

            ORIENTATION: {
                NONE: _ORIENTATION_NONE,
                VERTICAL: _ORIENTATION_VERTICAL,
                HORIZONTAL: _ORIENTATION_HORIZONTAL,
            },

            SIDE: {
                NONE: _SIDE_NONE,
                TOP: _SIDE_TOP,
                RIGHT: _SIDE_RIGHT,
                BOTTOM: _SIDE_BOTTOM,
                LEFT: _SIDE_LEFT,
            },

            /**
             * Reverse-lookup the surface belonging to the supplied element based on it's id attribute
             *
             * @param element
             */
            lookup: function (element)
            {
                var retVal = null;

                //note that only the active dropspot is visible (and there should only be one)
                if (element && element.hasAttribute(blocks.elements.Surface.INDEX_ATTR)) {
                    retVal = blocks.elements.Surface.INDEX[element.attr(blocks.elements.Surface.INDEX_ATTR)];
                }

                return retVal;
            },

            /**
             * Returns true of the element is a bootstrap container
             */
            isContainer: function (element)
            {
                return element.hasClass(BlocksConstants.CONTAINER_CLASS);
            },

            /**
             * Returns true of the element is a bootstrap row
             */
            isRow: function (element)
            {
                return element.hasClass(BlocksConstants.ROW_CLASS);
            },

            /**
             * Returns true of the element is a bootstrap column
             */
            isColumn: function (element)
            {
                return element.is('[class*="' + blocks.elements.Column.CLASS_PREFIX + '"]');
            },

            /**
             * Returns true of the element is a template block
             */
            isBlock: function (element)
            {
                return element[0].tagName.indexOf("-") > 0;
            },

            /**
             * Returns true of the element is a property element
             */
            isProperty: function (element)
            {
                return element.hasAttribute("property") || element.hasAttribute("data-property");
            },

            /**
             * Reset all surface overlay DOM elements
             */
            clearSurfaces: function ()
            {
                UI.surfaceWrapper.empty();
            },

            /**
             * Reset all resizer DOM elements
             */
            clearResizers: function ()
            {
                UI.resizerWrapper.empty();
            },

            /**
             * Reset all dropspot DOM elements
             */
            clearDropspots: function ()
            {
                UI.dropspotWrapper.empty();
            },

            /**
             * Returns the currently selected dropspot
             */
            getActiveDropspot: function ()
            {
                //note that only the active dropspot is visible (and there should only be one)
                return blocks.elements.Surface.lookup(UI.dropspotWrapper.children(':visible').first());
            },
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
        overlay: undefined,
        //this holds a data structure of parent-surfaces for each side
        layoutParents: undefined,

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
            this.overlay = null;
            this.layoutParents = {};

            //note: we explicitly don't call refresh() here because
            //we want to build all siblings in the parent first and then
            //call refresh on them (so we know if we're last or not)
        },

        //-----PUBLIC METHODS-----
        /**
         * These are shortcut functions to detect which kind of surface we're dealing with.
         */
        isPage: function ()
        {
            return this instanceof blocks.elements.Page;
        },
        isContainer: function ()
        {
            return this instanceof blocks.elements.Container;
        },
        isRow: function ()
        {
            return this instanceof blocks.elements.Row;
        },
        isColumn: function ()
        {
            return this instanceof blocks.elements.Column;
        },
        isBlock: function ()
        {
            return this instanceof blocks.elements.Block;
        },
        isProperty: function ()
        {
            return this instanceof blocks.elements.Property;
        },
        isResizer: function ()
        {
            return this instanceof blocks.elements.Resizer;
        },
        isDropspot: function ()
        {
            return this instanceof blocks.elements.Dropspot;
        },

        width: function ()
        {
            return this.right - this.left;
        },
        realWidth: function ()
        {
            return this.realRight - this.realLeft;
        },
        height: function ()
        {
            return this.bottom - this.top;
        },
        realHeight: function ()
        {
            return this.realBottom - this.realTop;
        },

        /**
         * Returns the first child we can find where the bounds wrap the supplied coordinate
         * (bounds included) or null if no such child was found.
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
         * Returns true if the supplied coordinate is inside this surface, it's bounds included
         */
        isInside: function (x, y)
        {
            return this.top <= y && y <= this.bottom && this.left <= x && x <= this.right;
        },

        /**
         * Create a visual preview of what would happen if this surface would be moved in the direction of
         * the indicated vector, over the supplied surface
         *
         * @param surface
         * @param vector
         */
        previewMoveTo: function (surface, vector)
        {
            //find out on which side the vector intersects with the hovered surface
            var side = surface._findIntersectingSide(vector);

            if (side !== blocks.elements.Surface.SIDE.NONE) {
                //create a list of possible dropspots on the given side
                var dropspots = surface._createDropspots(this, side, null);

                //select the optimal dropspot
                var idx = surface._selectDropspot(vector, this, side, dropspots);
                if (idx >= 0) {
                    dropspots[idx].show();
                }
            }
        },

        /**
         * Effectively move this surface to the indicated side of the supplied surface.
         * Overload to implement for a specific type.
         *
         * @param surface
         * @param side
         */
        moveTo: function (surface, side)
        {
            //NOOP
            Logger.error('Unimplemented DnD action called: move ' + this.type + ' to ' + side.name + ' of ' + surface.type);
        },

        //-----PRIVATE METHODS-----
        /**
         * Returns the type of this class in a human readable string.
         * Overload in the subclasses.
         * @returns {string}
         * @private
         */
        _getType: function ()
        {
            return 'surface';
        },
        /**
         * Returns the i18n name of this class.
         * Overload in the subclasses.
         * @returns {string}
         * @private
         */
        _getName: function ()
        {
            return BlocksMessages.surfaceName;
        },
        /**
         * Returns the first parent of this surface with the specified class or null if it couldn't be found.
         * @private
         */
        _getParent: function (parentClass)
        {
            var retVal = this;

            while (retVal && !(retVal instanceof parentClass)) {
                retVal = retVal.parent;
            }

            return retVal;
        },
        /**
         * Uniformly returns the native tag name of this surface if it has a valid element or null if it doesn't.
         *
         * @returns {*}
         * @private
         */
        _getTagName: function()
        {
            var retVal = null;

            if (this.element && this.element.length > 0) {
                retVal = this.element[0].tagName;
            }

            return retVal;
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

                //if a specific element is supplied, use that one as the parent context element
                this._initChildren(this.element);

                //before we start building the submodel, we need to make sure
                //our bounds are initialized and correct, because the new children will sometimes
                //need to 'pull open' their bounds to match their parent
                this._refresh();

                // Now we know the order of the children (eg. which one is first
                // and last), we iterate the children again to refresh them
                // and build their sub-models
                for (var i = 0; i < this.children.length; i++) {
                    //Note how the recursion context switches to the child surface
                    //Also note that the child will be refreshed first-thing
                    this.children[i]._buildSubmodel();
                }
            }
        },
        /**
         * Iterate the element width-first and populate the children array
         *
         * @param parentElement
         * @private
         */
        _initChildren: function (parentElement)
        {
            //note: children() does not return text nodes
            var childElements = parentElement.children();

            // Main idea is to iterate width-first instead of depth-first,
            // because low-level surfaces depend on their parents to be
            // layouted correctly, before building the sub-model
            for (var i = 0; i < childElements.length; i++) {

                var childElement = $(childElements[i]);

                //Note: if we find an acceptable child,
                // we won't dig deeper because we iterate width-first
                if (this._isAcceptableChild(childElement)) {
                    this.children.push(this._newChildInstance(childElement));
                }
                // This recursion allows grandchildren to be part of
                // the same parent-element context
                // (eg. because of this, we support in-between divs)
                else {
                    this._initChildren(childElement);
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
         * Returns whether the children in this surface are layouted
         * horizontally, vertically or have no specific orientation.
         *
         * @returns {number}
         * @private
         */
        _getChildOrientation: function ()
        {
            return blocks.elements.Surface.ORIENTATION.NONE;
        },
        /**
         * Initializes the layout parents data structure. It is used to
         * make this surface fit nicely with the bounds of the parent and to
         * easily walk surfaces to show (and eliminate) possible dropspots.
         *
         * @protected
         */
        _initLayoutParents: function ()
        {
            //start out with a fresh structure
            this.layoutParents = {};

            //we can't have layout parents of we don't have a parent
            if (this.parent) {
                switch (this.parent._getChildOrientation()) {
                    case blocks.elements.Surface.ORIENTATION.VERTICAL:
                        this._layoutVertically();
                        break;
                    case blocks.elements.Surface.ORIENTATION.HORIZONTAL:
                        this._layoutHorizontally();
                        break;
                }
            }
        },
        /**
         * Adds a child to this vertical-oriented parent surface
         *
         * @param surface
         * @protected
         */
        _layoutVertically: function ()
        {
            this.layoutParents[blocks.elements.Surface.SIDE.LEFT.id] = this.parent;
            this.layoutParents[blocks.elements.Surface.SIDE.RIGHT.id] = this.parent;

            if (this.index === 0) {
                this.layoutParents[blocks.elements.Surface.SIDE.TOP.id] = this.parent;
            }

            if (this.index === this.parent.children.length - 1) {
                this.layoutParents[blocks.elements.Surface.SIDE.BOTTOM.id] = this.parent;
            }
        },
        /**
         * Adds a child to this horizontal-oriented parent surface
         * @param surface
         * @protected
         */
        _layoutHorizontally: function ()
        {
            this.layoutParents[blocks.elements.Surface.SIDE.TOP.id] = this.parent;
            this.layoutParents[blocks.elements.Surface.SIDE.BOTTOM.id] = this.parent;

            if (this.index === 0) {
                this.layoutParents[blocks.elements.Surface.SIDE.LEFT.id] = this.parent;
            }

            if (this.index === this.parent.children.length - 1) {
                this.layoutParents[blocks.elements.Surface.SIDE.RIGHT.id] = this.parent;
            }
        },
        /**
         * Uniform superclass implementation for all overlay elements.
         * Call this method from the subclasses and extend from here.
         * @returns {jQuery}
         * @private
         */
        _createOverlay: function (containerElement)
        {
            var retVal = $("<div />").addClass(BlocksConstants.SURFACE_ELEMENT_CLASS);

            //this will allow us to do a reverse lookup starting from the element
            retVal.attr(blocks.elements.Surface.INDEX_ATTR, this.id);

            if (containerElement) {
                containerElement.append(retVal);
            }

            return retVal;
        },
        /**
         * Refreshes all possible variables of this surface. Usually, this is called
         * when the bounds/classes/properties of the underlying element were altered and
         * new variables must be calculated. However, it can also be used to refresh the position
         * of element-less surfaces (like resizers)
         *
         * @private
         */
        _refresh: function (deep)
        {
            //this allows us to call the constructor with no arguments
            if (this.element) {

                //note: these are the core dimension translations of an element to
                //a surface. Eg. we don't include margins.
                var topLeft = this.element.offset();
                var width = this.element.outerWidth();
                var height = this.element.outerHeight();

                this.top = topLeft.top;
                this.right = topLeft.left + width;
                this.bottom = topLeft.top + height;
                this.left = topLeft.left;

                //backup the bounds before they are (possibly) altered below
                this.realTop = this.top;
                this.realRight = this.right;
                this.realBottom = this.bottom;
                this.realLeft = this.left;

                this._initLayoutParents();

                // if we have a parent layout element, it looks a lot nicer (more intuitive)
                // to sync the side of this surface to the side of that parent
                // (see _layoutChildVertically() and _layoutChildHorizontally())
                if (this.layoutParents[blocks.elements.Surface.SIDE.TOP.id]) {
                    this.top = this.layoutParents[blocks.elements.Surface.SIDE.TOP.id].top;
                }
                if (this.layoutParents[blocks.elements.Surface.SIDE.RIGHT.id]) {
                    this.right = this.layoutParents[blocks.elements.Surface.SIDE.RIGHT.id].right;
                }
                if (this.layoutParents[blocks.elements.Surface.SIDE.BOTTOM.id]) {
                    this.bottom = this.layoutParents[blocks.elements.Surface.SIDE.BOTTOM.id].bottom;
                }
                if (this.layoutParents[blocks.elements.Surface.SIDE.LEFT.id]) {
                    this.left = this.layoutParents[blocks.elements.Surface.SIDE.LEFT.id].left;
                }
            }

            //now we have updated the bounds, it's time to redraw the overlay element
            this._redrawOverlay();

            //If a deep refresh is requested, force a refresh on the children too
            if (deep) {
                for (var i = 0; i < this.children.length; i++) {
                    this.children[i]._refresh(deep);
                }
            }
        },
        /**
         * Redraws the UI surface if we have one
         * @private
         */
        _redrawOverlay: function ()
        {
            if (this.overlay) {

                var width = this.width();
                var height = this.height();

                //the surface is visible if one of these is true:
                // - it either has no element attached at all (eg. a resizer)
                // - it's attached element is visible and the area is larger than zero
                if ((!this.element || this.element.is(':visible')) && width * height > 0) {
                    this.overlay.css("top", this.top + "px");
                    this.overlay.css("left", this.left + "px");
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
         * Calculates the side of this surface that intersects with the supplied vector.
         * Note that this only returns one side, even if two sides would intersect.
         * The order that is checked is top, bottom, left, right.
         *
         * @param vector The vector (having x1, y1, x2, y2 properties)
         * @returns BaseConstantsInternal.DIRECTION
         */
        _findIntersectingSide: function (vector)
        {
            if (this._intersects(vector.x1, vector.y1, vector.x2, vector.y2, this.left, this.top, this.right, this.top)) {
                return blocks.elements.Surface.SIDE.TOP;
            }
            else if (this._intersects(vector.x1, vector.y1, vector.x2, vector.y2, this.left, this.bottom, this.right, this.bottom)) {
                return blocks.elements.Surface.SIDE.BOTTOM;
            }
            else if (this._intersects(vector.x1, vector.y1, vector.x2, vector.y2, this.left, this.top, this.left, this.bottom)) {
                return blocks.elements.Surface.SIDE.LEFT;
            }
            else if (this._intersects(vector.x1, vector.y1, vector.x2, vector.y2, this.right, this.top, this.right, this.bottom)) {
                return blocks.elements.Surface.SIDE.RIGHT;
            }
            else {
                return blocks.elements.Surface.SIDE.NONE;
            }
        },
        /**
         * Checks if two line segments (seg1 and seg2) intersect with each other.
         * See https://gist.github.com/Joncom/e8e8d18ebe7fe55c3894
         */
        _intersects: function (seg1StartX, seg1StartY, seg1EndX, seg1EndY, seg2StartX, seg2StartY, seg2EndX, seg2EndY)
        {
            var s1_x, s1_y, s2_x, s2_y;
            s1_x = seg1EndX - seg1StartX;
            s1_y = seg1EndY - seg1StartY;
            s2_x = seg2EndX - seg2StartX;
            s2_y = seg2EndY - seg2StartY;

            var s, t;
            s = (-s1_y * (seg1StartX - seg2StartX) + s1_x * (seg1StartY - seg2StartY)) / (-s2_x * s1_y + s1_x * s2_y);
            t = (s2_x * (seg1StartY - seg2StartY) - s2_y * (seg1StartX - seg2StartX)) / (-s2_x * s1_y + s1_x * s2_y);

            // Collision detected
            if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
                return true;
            }
            // No collision
            else {
                return false;
            }
        },
        /**
         * Recursively create the dropspots of this surface (and the parents) on the specified side.
         * @param surface The surface that's being dragged around
         * @param side The side of this surface to show dropspots for
         * @param prevSurface The previous surface in the recursive call on the way to the top page surface
         * @returns {Array}
         * @private
         */
        _createDropspots: function (surface, side, prevSurface)
        {
            var retval = [];

            //if we have a previous surface (on the way to the top surface, eg. the page),
            //and that surface has the same dimensions as this, we prefer to show that one
            //(the most inward one) and not show it's parents with the same dimension
            var show = !(prevSurface && this._getDimension(side) === prevSurface._getDimension(side));

            //we don't show dropspots on the sides of the block that is being dragged around
            show &= surface !== this;

            if (show) {
                retval.push(new blocks.elements.Dropspot(this, side));
            }

            var layoutParent = this.layoutParents[side.id];
            if (layoutParent) {
                //adds the resulting parent array to the retVal array
                retval = retval.concat(layoutParent._createDropspots(surface, side, this));
            }

            return retval;
        },
        /**
         * Selects the dropspot to use, based on the data in the vector
         *
         * @param vector The vector that's currently dragging
         * @param surface The surface that's being dragged around
         * @param side The side on the surface the vector is pointing to as calculated with _findIntersectingSide()
         * @param dropspots The array of dropspots that are available for the current vector and surface
         * @returns {int} The index in the dropspots array
         * @private
         */
        _selectDropspot: function (vector, surface, side, dropspots)
        {
            var retVal = -1;

            if (dropspots.length > 0) {

                // The idea is to calculate the fraction of the dragging position
                // relative to the full width/height in the same direction and to
                // use that fraction to calculate the relative index in the array
                // of dropspots
                var width = this.right - this.left;
                var height = this.bottom - this.top;
                var fraction = 0;
                switch (side.id) {
                    case blocks.elements.Surface.SIDE.TOP.id:
                        fraction = (vector.y1 - this.top) / height;
                        break;
                    case blocks.elements.Surface.SIDE.BOTTOM.id:
                        fraction = (this.bottom - vector.y1) / height;
                        break;
                    case blocks.elements.Surface.SIDE.LEFT.id:
                        fraction = (vector.x1 - this.left) / width;
                        break;
                    case blocks.elements.Surface.SIDE.RIGHT.id:
                        fraction = (this.right - vector.x1) / width;
                        break;
                }

                //convert the [1..0] range to [0..1]
                fraction = 1.0 - fraction;

                //only use half of the dimension;
                //eg. when dragging right, the full left half of the block
                //will return index 0 (the innermost dropspot) since this one
                //is most likely to be wanted by the user. The surface of the
                //right half will be divided by the number of dropspots available
                fraction = Math.max(fraction - 0.5, 0) * 2.0;

                //convert the fraction to an index in the array
                retVal = Math.floor(dropspots.length * fraction);

                //failsafe, shouldn't happen
                retVal = Math.min(Math.max(retVal, 0), dropspots.length - 1);
            }

            return retVal;
        },
        /**
         * Returns the dimension of the side of this surface
         *
         * @param side
         * @returns {number}
         * @private
         */
        _getDimension: function (side)
        {
            switch (side.id) {
                case blocks.elements.Surface.SIDE.TOP.id:
                case blocks.elements.Surface.SIDE.BOTTOM.id:
                    return this.right - this.left;
                case blocks.elements.Surface.SIDE.RIGHT.id:
                case blocks.elements.Surface.SIDE.LEFT.id:
                    return this.bottom - this.top;
            }
        },
        /**
         * Adds a new child at the indicated index (defaults to appending).
         * Note: this doesn't perform a refresh!
         *
         * @param surface
         * @param index
         * @param before
         * @private
         */
        _addChild: function (surface, index)
        {
            //if we don't supply an index, we default to appending
            index = Commons.isUnset(index) ? this.children.length : index;

            //if we have children, we need to decide where to put the new surface
            //also the index should point to a valid existing child, otherwise we just append
            if (this.children.length > 0 && this.children[index]) {

                //we always add the child _before_ the element at the index,
                //because we will push back all the others (see below)
                this.children[index].element.before(surface.element);

                //remove zero elements from the specified index, then add surface at that index
                this.children.splice(index, 0, surface);

                //add one to all next children to keep the index in sync
                for (var i = index + 1; i < this.children.length; i++) {
                    this.children[i].index++;
                }
            }
            //if we don't have children, we can just append the new child
            else {
                this.element.append(surface.element);
                this.children.push(surface);
            }

            //sync the relationship-variables
            surface.index = index;
            surface.parent = this;

            //for chaining
            return surface;
        },
        /**
         * Removed the supplied child surface from this surface
         * (does nothing and logs an error if the supplied surface doesn't match the child of this parent).
         * Note: this also detaches a possible element from the DOM.
         * Note: this doesn't perform a refresh!
         *
         * @param surface
         * @private
         */
        _removeChild: function (surface)
        {
            var retVal = null;

            // This should always be true
            // Note: JS object-equality checks by reference
            if (this.children[surface.index] === surface) {

                //remove one child at the specified index
                this.children.splice(surface.index, 1);

                //subtract one from all next children to keep the index in sync
                for (var i = surface.index; i < this.children.length; i++) {
                    this.children[i].index--;
                }

                //'detach' the child surface from the parent
                surface.index = -1;
                surface.parent = null;

                if (surface.element) {
                    surface.element.detach();
                }

                retVal = surface;
            }
            else {
                Logger.error('Encountered a situation where a child surface is out of sync with its parent, this shouldn\'t happen');
            }

            return retVal;
        },


        //-----TODO UNCHECKED-----
        // /**
        //  * Returns true if this surface has no sibling on the specified side
        //  * @param side
        //  * @returns {*|boolean}
        //  */
        // isOuter: function (side)
        // {
        //     if (side == Constants.SIDE.TOP) {
        //         return this._isOuterTop();
        //     }
        //     else if (side == Constants.SIDE.BOTTOM) {
        //         return this._isOuterBottom();
        //     }
        //     else if (side == Constants.SIDE.LEFT) {
        //         return this._isOuterLeft();
        //     }
        //     else if (side == Constants.SIDE.RIGHT) {
        //         return this._isOuterRight();
        //     }
        // },
        // /**
        //  * Returns the element at specified side, general function for getNext, getPrevious
        //  * @param side
        //  * @returns {*}
        //  */
        // getElementAtSide: function (side)
        // {
        //     if (side == Constants.SIDE.TOP || side == Constants.SIDE.LEFT) {
        //         return this.getPrevious();
        //     }
        //     else {
        //         return this.getNext();
        //     }
        // },
        // getNext: function ()
        // {
        //     if (this.parent == null) {
        //         return null;
        //     }
        //
        //     if (this.index + 1 < this.parent.children.length) {
        //         return this.parent.children[this.index + 1];
        //     }
        //     else {
        //         return null;
        //     }
        // },
        // getPrevious: function ()
        // {
        //     if (this.parent == null) {
        //         return null;
        //     }
        //     if (this.index - 1 >= 0) {
        //         return this.parent.children[this.index - 1];
        //     }
        //     else {
        //         return null;
        //     }
        // },
        // // find most left and right column and use them to calculate the width of the parent
        // getFullWidth: function ()
        // {
        //     var retVal = 0;
        //
        //     if (this.parent != null) {
        //         var outerleft = this;
        //         var outerright = this;
        //         while (!this._isOuterLeft() && this.parent != null) {
        //             outerleft = this.parent;
        //         }
        //         while (!this._isOuterRight() && this.parent != null) {
        //             outerright = this.parent;
        //         }
        //         retVal = outerright.right - outerleft.left;
        //
        //     }
        //     else {
        //         retVal = this.right - this.left;
        //     }
        //
        //     return retVal;
        // },
        // // find all dropspots for an element
        // // is called for a block and returns all dropspots for this block and his parents.
        // createAllDropspots: function ()
        // {
        //     if (this.isBlock()) {
        //         this.generateDropspots();
        //     }
        //     else {
        //         if (this.children.length > 0) {
        //             for (var i = 0; i < this.children.length; i++) {
        //                 this.children[i].createAllDropspots();
        //             }
        //         }
        //     }
        //     if (this.container != null) {
        //         this.container.createAllDropspots();
        //     }
        // },
        // getBlocks: function ()
        // {
        //     if (this.totalBlocks != null) return this.totalBlocks;
        //     this.totalBlocks = 0;
        //     for (var i = 0; i < this.children.length; i++) {
        //         if (this.children[i].isBlock()) {
        //             this.totalBlocks += 1;
        //         }
        //         else {
        //             this.totalBlocks += this.children[i].getBlocks();
        //         }
        //     }
        //     return this.totalBlocks;
        // },
        // // Container is a LayoutElement without a parent
        // getContainer: function ()
        // {
        //     var parent = this.parent;
        //     while (parent != null && !parent.isContainer()) {
        //         parent = parent.parent;
        //     }
        //
        //     return parent;
        // },
        // calculateDropspots: function (side, dropspots)
        // {
        //     return [];
        // },
        // generateDropspots: function ()
        // {
        // },
        // /**
        //  * Show the best dropspot for the supplied vector
        //  *
        //  * @param surface The surface that's being dragged around (and is hovering over this surface)
        //  * @param vector Data structure with (x1, y1) and (x2, y2) properties
        //  */
        // showDropspot: function (surface, vector)
        // {
        //     var side = this._findIntersectingSide(vector);
        //
        //     //start out by removing the existing dropspots
        //     this.resetDropspots();
        //
        //     if (side !== blocks.elements.Surface.SIDE.NONE) {
        //         var dropspots = this._createDropspots(surface, side, null);
        //         var idx = this._selectDropspot(vector, surface, side, dropspots);
        //         if (idx >= 0) {
        //             this.dropspot = dropspots[idx];
        //             this.dropspot.show();
        //         }
        //     }
        // },
        //
        // /**
        //  * Reset all active dropspots of this surface
        //  */
        // resetDropspots: function ()
        // {
        //     // note: this will clear all dropspots in the DOM,
        //     // even the ones not attached to this surface
        //     UI.dropspotWrapper.empty();
        //
        //     this.dropspot = null;
        // },
        // /**
        //  * Calculates the top value for the constructor of the supplied element
        //  */
        // _calculateTop: function (element)
        // {
        //     return element.offset().top;
        // },
        // /**
        //  * Calculates the bottom value for the constructor of the supplied element
        //  */
        // _calculateBottom: function (element)
        // {
        //     return element.offset().top + element.outerHeight();
        // },
        // /**
        //  * Calculates the left value for the constructor of the supplied element
        //  */
        // _calculateLeft: function (element)
        // {
        //     return element.offset().left;
        // },
        // /**
        //  * Calculates the right value for the constructor of the supplied element
        //  */
        // _calculateRight: function (element)
        // {
        //     return element.offset().left + element.outerWidth();
        // },
        // /**
        //  * Returns true if this block has no sibling on his left
        //  * (to be overridden by subclasses)
        //  * @returns {boolean}
        //  * @private
        //  */
        // _isOuterLeft: function ()
        // {
        //     return true;
        // },
        // /**
        //  * Returns true if this block has no sibling on his right
        //  * (to be overridden by subclasses)
        //  * @returns {boolean}
        //  * @private
        //  */
        // _isOuterRight: function ()
        // {
        //     return true;
        // },
        // /**
        //  * Returns true if this block has no sibling on his top
        //  * (to be overridden by subclasses)
        //  * @returns {boolean}
        //  * @private
        //  */
        // _isOuterTop: function ()
        // {
        //     return true;
        // },
        // /**
        //  * Returns true if this block has no sibling on his bottom
        //  * (to be overridden by subclasses)
        //  * @returns {boolean}
        //  * @private
        //  */
        // _isOuterBottom: function ()
        // {
        //     return true;
        // },
    });

}]);

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
 * Created by wouter on 5/03/15.
 */

base.plugin("blocks.core.Elements.Container", ["base.core.Class", "constants.base.core.internal", "blocks.core.DomManipulation", function (Class, Constants, DOM)
{
    blocks = window['blocks'] || {};
    // Region where templates can be dragged
    blocks.elements = blocks.elements || {};
    blocks.elements.Container = Class.create(blocks.elements.LayoutElement, {
        constructor: function (element, parent)
        {
            blocks.elements.Container.Super.call(this, element, parent, 0);
            this.blocks = [];

            if (this.parent != null && this.parent instanceof blocks.elements.Block) {
                this.left = this.parent.left;
                this.right = this.parent.right;
                this.top = this.parent.top;
                this.bottom = this.parent.bottom;
            }

            this.canLayout = true;
            this.generateChildrenForColumn();
            this.fillRows();

            this.overlay = null;
        },

        getElementAtSide: function (side)
        {
            return null;
        },

        calculateDropspots: function (side, dropspots)
        {
            if ((side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) && this.children.length > 1) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            return dropspots;
        },

        generateProperties: function (parent, index)
        {

        },

        getContainer: function ()
        {
            return this;
        }

    });

}]);

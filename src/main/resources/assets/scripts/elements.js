
/*
 * Classes for all layout types and surface
 * Layouter builds a virtual tree from these objects for easy searching and triggering
 * and to take some load of the dom while calculating
 *
 * The layout tree starts from a container ( = element)
 *  - if this container has a can-layout attribute we build a tree with children
 *  - if not, we search all first level properties in this container
 *      - if this property is can-layout then the property creates a new container
 *      - etc ...
 * */
blocks
    .plugin("blocks.core.Elements", ["base.core.Class", "blocks.core.Constants", "blocks.core.DomManipulation", "blocks.core.Edit", function (Class, Constants, DOM, Edit)
    {

        blocks.elements = {};
        blocks.elements.ResizeHandle = resizeHandle;
        blocks.elements.Row = row;
        blocks.elements.Container = container;
        blocks.elements.Block = block;
        blocks.elements.Column = column;
        blocks.elements.Property = property;
    } ]);
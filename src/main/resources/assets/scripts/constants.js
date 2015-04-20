/**
 * Created by wouter on 13/10/14.
 */

// All constants

base.plugin("blocks.core.Constants", function ()
{

    // Must be ordered from small to big
    this.COLUMN_WIDTH_CLASS = [
        {name: "col-xs-", min: 0, max: 767},
        {name: "col-sm-", min: 768, max: 991},
        {name: "col-md-", min: 992, max: 1199},
        {name: "col-lg-", min: 120, max: 10000}
    ];
    this.CAN_LAYOUT_ROW_CLASS = "can-layout"; // can layout row and add and delete templates
    this.CAN_EDIT_BLOCK_CLASS = "can-edit"; // specifies edit and delete
    this.IS_ENTITY = "use-blueprint";
    this.IS_PROPERTY = "property";

    this.MAX_COLUMNS = 12;

    this.COLUMN_RESIZER_CLASS = "column-resize-handle";
    this.BLOCK_HOVER_CLASS = "block-hover";
    this.BLOCK_OVERLAY_CLASS = "block-overlay";
    this.DRAGGED_BLOCK_OVERLAY_CLASS = "dragged-block-overlay";
    this.PROPERTY_OVERLAY_CLASS = "property-overlay";
    this.PROPERTY_HOVER_CLASS = "property-hover";
    this.PROPERTY_EDIT_CLASS = "property-edit";
    this.BLOCK_DRAGGABLE_CLASS = "draggable";
    this.FORCE_RESIZE_CURSOR = "force-col-resize-cursor";
    this.FORCE_DRAG_CURSOR = "force-dragging-cursor";
    this.NO_TEXT_CLASS = "property-no-text";
    this.DRAG_CREATE_BLOCK = "drag-create-block";
    this.CREATE_BLOCK_TYPE = "create-block-type";

    this.EDIT_TEXT = "edit-text";
    this.EDIT_NONE = "edit-none";
    this.EDIT_OTHER = "edit-other";

    this.maxIndex = 0;
    this.calculateMaxIndex = function() {
        this.maxIndex = Math.max.apply(null,$.map($('body  *'), function(e,n){
                if($(e).css('position')=='absolute' || $(e).css('position')=='relative')
                    return parseInt($(e).css('z-index'))||1 ;
            })
        );
    };
});



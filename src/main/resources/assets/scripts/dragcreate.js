/**
 * Created by wouter on 12/03/15.
 */

base.plugin("blocks.core.DragCreate", ["constants.blocks.common", "blocks.core.DragDrop", "blocks.core.Broadcaster", "blocks.core.DomManipulation", "blocks.core.Layouter", "blocks.core.Notification", function (Constants, DragDrop, Broadcaster, DOM, Layouter, Notification)
{
    FakeDragDrop = this;

    var active = false;
    var dragging = false;
    var targetButton = null;

    // Start and stop on start stop templates
    this.activate = function ()
    {
        active = true;
        targetButton = null;
        $(document).on("mousedown.fakedragdrop", ".drag-create-block", function (event)
        {
            if (event.which == 1) {
                targetButton = $(event.currentTarget);
                start();
            }
        });
    };

    this.deactivate = function ()
    {
        active = false;
        $(document).off("mousedown.fakedragdrop");
    };

    var start = function ()
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        $(document).on("mouseup.fakedragdrop", function (event)
        {
            end();
        });
        dragging = true;

        DOM.disableSelection();
        DragDrop.dragStarted(null);
        $(document).on("mousemove.fakedragdrop", function (event)
        {
            Broadcaster.send(Broadcaster.EVENTS.DRAG_OVER_BLOCK);
        });

    };

    var end = function ()
    {
        $(document).off("mousemove.fakedragdrop");
        var dropSpot = DragDrop.getCurrentDropspot();
        Logger.debug(dropSpot);
        Broadcaster.send(Broadcaster.EVENTS.ABORT_DRAG);
        $(document).off("mouseup.fakedragdrop");
        dragging = false;

        if (dropSpot != null && dropSpot.block != null) {
            var name = targetButton.attr(Constants.CREATE_BLOCK_TYPE);
            if (name == null) {
                askBlockToAdd(dropSpot.side, dropSpot.block);
            } else {
                addBlock(name, dropSpot.side, dropSpot.block);
            }
        }

        DOM.enableSelection();
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
    };


    function askBlockToAdd(side, currentBlock)
    {
        $.getJSON("/entities/list").success(function (data)
        {
            var modalText = '<div class="form-inline" role="form"><div class="form-group"></div></div>';
            var optionList = $('<select class="form-control" id="blocktypeselect"></div>');
            var label = '<label for="inputPassword2" class="sr-only">Type block : </label>';
            for (var i = 0; i < data.length; i++) {
                optionList.append('<option value="' + data[i] + '">' + data[i] + '</option>');
            }
            var list = $(modalText);
            list.find(".form-group").empty().append(label).append(optionList);

            BootstrapDialog.show({
                title: 'Add a custom block',
                message: list,
                buttons: [
                    {
                        id: 'btn-close',
                        label: 'Cancel',
                        action: function (dialogRef)
                        {
                            dialogRef.close();
                        }
                    },
                    {
                        id: 'btn-ok',
                        icon: 'glyphicon glyphicon-check',
                        label: 'Ok',
                        cssClass: 'btn-primary',
                        action: function (dialogRef)
                        {
                            var value = dialogRef.$modalBody.find("#blocktypeselect").val();
                            if (value != null && value != "") {
                                addBlock(value, side, currentBlock);
                            } else {
                                Notification.error("An invalid block was selected");
                            }
                            dialogRef.close();
                        }

                    }]

            })

        });
    }

    function addBlock(name, side, currentBlock)
    {
        $.getJSON("/entities/class/" + name).success(function (data)
        {
            Layouter.addNewBlockAtLocation(side, $(data.template), currentBlock);
        }).error(function ()
        {
            // error
            Notification.error("This block could not be found on the server");
        });
    }


}
]);

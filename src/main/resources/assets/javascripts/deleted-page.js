/**
 * Created by bas on 09.02.15.
 */
function showSelectionModal(){
    var selectionModal = new BootstrapDialog()
        .setTitle('Choose a page type')
        .setMessage($('<div></div>').load('/modals/newpage?entityurl=' + window.location.href))
        .setType(BootstrapDialog.TYPE_INFO)
        .setButtons([
            {
                label: 'Cancel',
                action: function (newPageDialog) {
                    newPageDialog.close();
                }
            },
            {
                label: 'New page',
                cssClass: 'btn-info',
                action: function (newPageDialog) {
                    $('#newpage').submit();
                    newPageDialog.close();
                }
            }])
        .open();
}

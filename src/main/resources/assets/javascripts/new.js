/**
 * Created by bas on 07.10.14.
 */
function newPage() {
    $.post("/page/default", function onSuccess(data) {
        $( "html" ).html( data );
    });
}
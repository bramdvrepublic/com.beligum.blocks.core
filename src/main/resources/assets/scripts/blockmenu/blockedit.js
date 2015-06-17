
base.plugin("blocks.core.BlockEdit", [function ()
{

    this.selectClass = function(element, keywords) {

        var select = $("<select />");
        for (var i=0; i < keywords.length; i++) {
            select.append($("<option value='"+ keywords[i] +"'>"+ keywords[i] +"</option>"));
        }
    };

    this.optionClass = function(element, keywords) {

        var select = $("<select />");
        for (var i=0; i < keywords.length; i++) {
            select.append($("<option value='"+ keywords[i] +"'>"+ keywords[i] +"</option>"));
        }
    };

}]);
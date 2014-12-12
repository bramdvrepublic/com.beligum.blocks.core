blocks.plugin("blocks.finder.embed", ["blocks.finder", function(Finder) {
  $(document).ready(function() {
      Finder.show($(".container"));
  })

}]);
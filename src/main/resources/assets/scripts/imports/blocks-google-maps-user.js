/**
 * Created by wouter on 2/09/15.
 */
/*
 * This script sets the auto function for thge google maps block
 * must be loaded even if we are not admin, because it is part of the general functionality
 * of this block
 * */
base.plugin("mot.imports.MapsUser", ["base.core.Class", function () {
    var mapIndex = 0;
    var DATA_INDEX = "data-index";
    var DATA_AUTOLOAD = "data-autoload";
    var DATA_ADDRESS = "data-address";
    var DATA_API_KEY = "data-api-key";

    var maps = {};
    var geocoder = null;


    var setLocation = function(element)
    {
        if (element.hasAttribute(DATA_AUTOLOAD)) {
            autoSetAddress(element);
        }
        var address = element.attr(DATA_ADDRESS);
        var map = maps[element.attr(DATA_INDEX)];
        geocodeAddress(address, map);

    };

    var autoSetAddress = function(element) {
        var parent = element.parent();

        // Search our parent block. We will search inside this parent for our address values
        while (!(element.prop("tagName").indexOf("-") != -1) && !(element.prop("tagName") === "body")) {
            parent = parent.parent();
        }
        var country = parent.find("[property='http://www.mot.be/ontology/country']").html();
        var city = parent.find("[property='http://www.mot.be/ontology/city']").html();
        var borough = parent.find("[property='http://www.mot.be/ontology/borough']").html()
        var street = parent.find("[property='http://www.mot.be/ontology/street']").html();
        var streetNumber = parent.find("[property='http://www.mot.be/ontology/streetNumber']").html();
        element.attr(DATA_ADDRESS, country + ", " + city + ", " + borough + ", " + street + ", " + streetNumber);

    };

    /*
     * Try to geocode the address. If we can not find the address, try with a shorter version until we find
     * Keep trying until our address is an empty string
     * */
    var geocodeAddress = function(address, resultsMap) {
        if (address != null && address.length > 0) {
            geocoder.geocode({'address': address}, function (results, status) {
                if (status === google.maps.GeocoderStatus.OK) {
                    resultsMap.setCenter(results[0].geometry.location);
                    var marker = new google.maps.Marker({
                        map: resultsMap,
                        position: results[0].geometry.location
                    });
                } else {
                    geocodeAddress(shorterAddress(address), resultsMap);
                }
            });
        } else {
            Logger.error("Address could not be geolocated")
        }
    };

    var shorterAddress = function(address) {
        var retVal = address;
        var i = retVal.lastIndexOf(",");
        if (i > 0) {
            retVal = retVal.substring(0, i);
        } else {
            retVal = "";
        }
        return retVal.trim();
    };

    // Create a js map object for each google maps block
    var createMap = function(index, element) {
        element.attr(DATA_INDEX, index);
        map = new google.maps.Map(element[0], {
            zoom: 12
        });
        maps[index] = map;
    };



    // Called by the google api when script is loaded
    initBlocksGoogleMaps = function()
    {
        geocoder = new google.maps.Geocoder();
        var mapElements = $("blocks-google-maps");
        mapElements.each(function (index) {
            var element = $(this);
            createMap(index, element);
            setLocation(element);
        })
    };

    $(document).ready(function () {
        // find a block with an api key defined. We will use this key for all maps
        var keyBlock = $("blocks-google-maps > span["+DATA_API_KEY+"]").remove().first();
        var apiKey = null;

        if (keyBlock.length == 1) {
             apiKey = keyBlock.attr(DATA_API_KEY).trim();
        }

        if (apiKey != null && apiKey != "") {
            $.getScript("https://maps.googleapis.com/maps/api/js?key=" + apiKey + "&callback=initBlocksGoogleMaps", function () {
            });
        } else {
            Logger.error("No Google API key found on page. Could not load the map(s)");
        }


    });



}]);


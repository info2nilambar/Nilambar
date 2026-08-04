(function () {
    var button = document.getElementById('use-location');
    if (!button) {
        return;
    }
    var status = document.getElementById('location-status');
    var latitude = document.getElementById('latitude');
    var longitude = document.getElementById('longitude');

    button.addEventListener('click', function () {
        if (!navigator.geolocation) {
            status.textContent = 'Geolocation is not supported by this browser.';
            return;
        }
        status.textContent = 'Locating...';
        navigator.geolocation.getCurrentPosition(
            function (position) {
                latitude.value = position.coords.latitude.toFixed(6);
                longitude.value = position.coords.longitude.toFixed(6);
                status.textContent = 'Coordinates filled in.';
            },
            function () {
                status.textContent = 'Could not read your location; enter the coordinates manually.';
            }
        );
    });
})();

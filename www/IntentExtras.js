var exec = require('cordova/exec'),
    cordova = require('cordova');


/**
 * This represents a thin shim layer over the Android Intent implementation
 * @constructor
 */
function ItexpertApi() {
    var me = this;
}

ItexpertApi.prototype.getIntent = function(successCallback, failureCallback) {
    exec(successCallback, failureCallback, "ItexpertApi", "getIntent", []);
};
ItexpertApi.prototype.sendDatabase = function(successCallback, failureCallback, url, databaseName) {
    exec(successCallback, failureCallback, "ItexpertApi", "sendDatabase", [url, databaseName]);
};

window.itexpertApi = new ItexpertApi();
window.plugins = window.plugins || {};
window.plugins.itexpertApi = window.itexpertApi;
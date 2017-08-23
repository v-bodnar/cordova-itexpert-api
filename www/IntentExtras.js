var exec = require('cordova/exec'),
    cordova = require('cordova');


/**
 * This represents a thin shim layer over the Android Intent implementation
 * @constructor
 */
function IntentExtras() {
    var me = this;
}

IntentExtras.prototype.getIntent = function(successCallback, failureCallback) {
    exec(successCallback, failureCallback, "IntentExtras", "getIntent", []);
};


window.intentExtras = new IntentExtras();
window.plugins = window.plugins || {};
window.plugins.intentShim = window.intentExtras;
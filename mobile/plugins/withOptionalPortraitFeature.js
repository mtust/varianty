// Google Play infers android.hardware.screen.portrait as a *required* feature
// from android:screenOrientation="portrait" (set by "orientation": "portrait"
// in app.json). That hides the app on devices/form factors without a portrait
// screen and blocks submission. Declaring the feature explicitly with
// required="false" keeps the portrait lock but makes the feature optional.
const { withAndroidManifest } = require('expo/config-plugins');

const FEATURES = ['android.hardware.screen.portrait'];

function makeFeaturesOptional(manifest) {
  manifest.$ = manifest.$ || {};
  manifest.$['xmlns:tools'] = manifest.$['xmlns:tools'] || 'http://schemas.android.com/tools';
  manifest['uses-feature'] = manifest['uses-feature'] || [];

  for (const name of FEATURES) {
    let feature = manifest['uses-feature'].find((f) => f.$ && f.$['android:name'] === name);
    if (!feature) {
      feature = { $: { 'android:name': name } };
      manifest['uses-feature'].push(feature);
    }
    feature.$['android:required'] = 'false';
    feature.$['tools:replace'] = 'android:required';
  }
  return manifest;
}

const withOptionalPortraitFeature = (config) =>
  withAndroidManifest(config, (cfg) => {
    makeFeaturesOptional(cfg.modResults.manifest);
    return cfg;
  });

module.exports = withOptionalPortraitFeature;
module.exports.makeFeaturesOptional = makeFeaturesOptional;

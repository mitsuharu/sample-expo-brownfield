// Test-only Babel config. The app itself is transformed by Metro, which has no
// babel.config.js in SDK 57 — keeping this under a different filename means
// adding Jest support does not change how the app is bundled.
module.exports = {
  presets: [['babel-preset-expo', { jsxRuntime: 'automatic' }]],
};

<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <style>
    @font-face {
      font-family: "JetBrains Mono";
      src: url("/fonts/JetBrainsMono-Medium.ttf") format("truetype");
      font-weight: 500;
      font-style: normal;
    }
  </style>
</head>
<body>
<div id="root"></div>
<!-- Environment variables -->
<script>
  window.theme = "${theme}";
  window.platform = "JetBrains";
  window.ide = "${ide}";
  window.mainFont = "${mainFont}";
  window.codeFont = "${codeFont}";
  window.isJaegerEnabled = ${isJaegerEnabled?string('true', 'false')};
  window.userEmail = "${userEmail}";
  window.isObservabilityEnabled = ${isObservabilityEnabled?string('true', 'false')};
  window.isDigmaEngineInstalled = ${isDigmaEngineInstalled?string('true', 'false')};
  window.isDigmaEngineRunning = ${isDigmaEngineRunning?string('true', 'false')};
  window.isDockerInstalled = ${isDockerInstalled?string('true', 'false')};
  window.isDockerComposeInstalled = ${isDockerComposeInstalled?string('true', 'false')};
  window.isDigmathonModeEnabled = ${isDigmathonModeEnabled?string('true', 'false')};
  window.productKey = "${productKey}";

  window.wizardFirstLaunch = ${wizardFirstLaunch?string('true', 'false')};
  window.wizardSkipInstallationStep = ${wizardSkipInstallationStep?string('true', 'false')};
</script>
<script src="/index.js"></script>
</body>
</html>

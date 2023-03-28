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
    body {
      background: ${(theme == 'light')?then('#F1F5FA','#383838')};
    }
  </style>
</head>
<body>
<div id="root"></div>
<!-- Environment variables -->
<script>
  window.environment = "JetBrains";
  window.theme = "${theme}";
  window.mainFont;
  window.codeFont;
  window.recentActivityRefreshInterval;
  window.recentActivityExpirationLimit;
  window.recentActivityDocumentationURL;
</script>
<script src="/index.js"></script>
</body>
</html>

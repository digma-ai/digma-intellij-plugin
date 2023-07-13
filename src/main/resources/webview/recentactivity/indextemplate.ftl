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
      window.platform = "JetBrains";
      window.theme = "${theme}";
      window.mainFont = "${mainFont}";
      window.codeFont = "${codeFont}";
      window.isJaegerEnabled = ${isJaegerEnabled?string('true', 'false')};
      window.recentActivityExpirationLimit = ${recentActivityExpirationLimit};
      window.recentActivityDocumentationURL = "https://youtu.be/F1Y0kETn-QQ";
    </script>
    <script src="/index.js"></script>
  </body>
</html>

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
      background: #5A5A5A;
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
</script>
<script src="/index.js"></script>
</body>
</html>

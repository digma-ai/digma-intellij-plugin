<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <style>
      @font-face {
        font-family: "JetBrains Mono";
        src: url("/assets/fonts/JetBrainsMono-Medium.ttf") format("truetype");
        font-weight: 500;
        font-style: normal;
      }
    </style>
  </head>
  <body>
    <div id="root"></div>
    <!-- Environment variables -->
    <script>
      @GLOBAL_ENV_VARS@

    </script>
    <script src="./index.js"></script>
  </body>
</html>

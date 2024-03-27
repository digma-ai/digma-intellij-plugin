## Logging API calls

in Idea go to Help -> Diagnostic Tools -> Debug Log Settings<br>
add:<br>
api.digma.org:all<br>
(no need to restart)

## Logging sensitive data

Sensitive info is hidden by default, to include sensitive info:<br>
in Idea go to Help -> Edit Custom Properties<br>
add<br>
api.digma.org.log.sensitive=true<br>
and restart

Log messages will contain 'Digma: API'

```
2024-03-28 01:47:09,798 [   2998]   FINE - api.digma.org - Digma: API:--> GET https://localhost:3051/CodeAnalytics/environments
2024-03-28 01:47:09,798 [   2998]   FINE - api.digma.org - Digma: API:Accept: application/+json
2024-03-28 01:47:09,798 [   2998]   FINE - api.digma.org - Digma: API:Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoiYWRtaW5AZGlnbWEuYWkiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3ByaW1hcnlzaWQiOiJjYzY3NDFmMi1iMTQ0LTQ2YTktOGNiYi02ZmZjZTc4MjE1OGQiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiOiJBZG1pbiIsImV4cCI6MTcxMTU4NDEyOX0._bKXhoL7aa9_yZOGxY2K-qf-l4clv2GdqjYvrKRsVgc
2024-03-28 01:47:09,798 [   2998]   FINE - api.digma.org - Digma: API:--> END GET
2024-03-28 01:47:09,800 [   3000]   FINE - api.digma.org - Digma: API:<-- 200 https://localhost:3051/CodeAnalytics/environments (2ms)
2024-03-28 01:47:09,800 [   3000]   FINE - api.digma.org - Digma: API:content-type: application/json; charset=utf-8
2024-03-28 01:47:09,800 [   3000]   FINE - api.digma.org - Digma: API:date: Wed, 27 Mar 2024 23:47:09 GMT
2024-03-28 01:47:09,800 [   3000]   FINE - api.digma.org - Digma: API:server: Kestrel
2024-03-28 01:47:09,800 [   3000]   FINE - api.digma.org - Digma: API:vary: Accept-Encoding
2024-03-28 01:47:09,800 [   3000]   FINE - api.digma.org - Digma: API:
2024-03-28 01:47:09,800 [   3000]   FINE - api.digma.org - Digma: API:["LOCAL-TESTS","LOCAL"]
2024-03-28 01:47:09,801 [   3001]   FINE - api.digma.org - Digma: API:<-- END HTTP (23-byte body)

```

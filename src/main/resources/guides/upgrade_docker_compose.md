# Upgrade guide for Digma using Docker Compose

### Linux and macOS

Run the following command in order to get the recent version

```shell
curl -L https://get.digma.ai/ --output docker-compose.yml
```

Then Run 
```shell
docker compose up -d --remove-orphans
```

### Windows (PowerShell)

Run the following command in order to get the recent version

```shell
iwr https://get.digma.ai/ -outfile docker-compose.yml
```

Then Run 
```shell
docker compose up -d --remove-orphans
```

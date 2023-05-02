# Upgrade guide for Digma using Helm

## If you are not the maintainer of the Helm file

Please do one of the either:
1. Contact Digma for support
2. Contact your DevOps team and ask them to upgrade

## If you are the maintainer of the Helm file please follow 

Assuming you installed Digma following the [main guide](https://github.com/digma-ai/helm-chart/tree/gh-pages)

Update the chart repository:

```shell
helm repo update
```

And now update digma with this command:

```shell
helm upgrade digma digma/digma 
```

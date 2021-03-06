# Drools 

## Installing and Running
- Kafka Cluster on Openshift
https://github.com/desmax74/openshift-handbook/blob/master/fedora/kafka.md

```sh
 mvn clean compile quarkus:dev  
 mvn package -Pnative
```


#### Build Container on docker
```sh
docker build -t quickstarter/openshift-kie-quarkus:latest .
docker images | grep openshift-kie
```

#### Deploy on Openshift
Relax RBAC for configmap
```sh
kubectl create clusterrolebinding permissive-binding --clusterrole=cluster-admin --group=system:serviceaccounts
```

By default will be created under project called "My Project"
```sh
kubectl create -f kubernetes/deployment.yaml 
kubectl create -f kubernetes/service.yaml 
oc expose service  openshift-kie-thorntail
```
 ```
 oc get route
 
  NAME           HOST/PORT                                      PATH      SERVICES       PORT      TERMINATION   WILDCARD
  openshift-kie   openshift-kie-myproject.192.168.99.109.nip.io           openshift-kie   http                    None
  ```
  
  Your address will be
  http://quick-drools-myproject.192.168.99.109.nip.io/rest/env/all
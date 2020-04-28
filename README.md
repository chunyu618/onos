## Useful Command
* Register a new app 
```
mvn archetype:generate -DarchetypeGroupId=org.onosproject -DarchetypeArtifactId=onos-bundle-archetype
```
* Compile
```
mvn clean install
```
* Install application 
```
onos-*.*.*/bin/onos-app install 127.0.0.1 *.oar
```
## Mininet
* Clean mininet
```
sudo mn -c
```
* Create a 2-layer topology
```
sudo mn --topo tree,2,3 --mac --controller remote,ip=127.0.0.1 --switch ovsk,protocol=OpenFlow13
```
## ONOS 
* start ONOS
```
onos-*.*.*/bin/onos-service start
```
* CLI

&emsp;[method1]
```
ssh -p 8101 onos@<IP>
#default username:onos password:rocks
```
&emsp;[method2]
```
onos-*.*.*/apache-karaf-*.*.*/bin/client
```
* GUI
```
http://<IP>:8181/onos/ui/index.html
```
## Application control
* Remember to start openflow
```
onos> app activate org.onosproject.openflow
```
* openflow forwarding application
```
onos> app activate org.onosproject.fwd
```
* Activate your own application
```
onos> app activate org.<groupId>.<app name>
```
* Deactivate application
```
onos> app deactivate org.<groupId>.<app name>
```
* Check active application
```
onos> app -a -s
```
* Check all application
```
onos> app -s
```
* Other useful command
```
onos> flows
onos> hosts
onos> devices
```
##Reference

&emsp; https://hackmd.io/K78rZTC0ROiOxosEsAA1qw?view

&emsp; https://github.com/benkajaja/ONOS-Apps/blob/master/MACLearning/src/main/java/jaja/AppComponent.java

&emsp; https://github.com/YanHaoChen/Learning-SDN/tree/master/Controller/ONOS

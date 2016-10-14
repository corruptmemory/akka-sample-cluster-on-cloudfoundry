# How to run Akka Cluster on CloudFoundry

This is a short guide to walk you through how to deploy and run Akka Cluster based application on [CloudFoundry](https://www.cloudfoundry.org/)

**Background:** [Akka Cluster](http://doc.akka.io/docs/akka/snapshot/scala/cluster-usage.html) is based on TCP communication in its pre-Aeron versions ( < 2.4.11 ) and can be configured to use UDP from Aeron version on ( > 2.4.11). However, the current CloudFoundry release [v245](https://github.com/cloudfoundry/cf-release/releases/tag/v245) doesn't support container-to-container TCP or UDP based communication. The develop branch of [Container Networking for Garden-RunC](https://github.com/cloudfoundry-incubator/netman-release) plugin contains support for this type of communication on CloudFoundry with fixed port numbers (no support for port number ranges). In this guide we will use this experimential plugin to show how to run Akka Cluster that uses TCP. In case of UDP, setting `canonical.port` ([2.4.11 release notes](http://akka.io/news/2016/09/30/akka-2.4.11-released.html)) will make UDP based Akka Cluster usable on CloudFoundry as well.


**Note:** This guide includes information how to install CloudFoundry **locally** using bosh-lite. CI deployment scripts and config for AWS provided by CloudFoundry could be found [here](https://github.com/cloudfoundry-incubator/container-networking-ci).


**Installing CloudFoundry components:**

- A development environment for BOSH: [bosh-lite](https://github.com/cloudfoundry/bosh-lite) - we have deployed from `develop` as of September 28th, 2016
- A container networking plugin: [netman-release]( https://github.com/cloudfoundry-incubator/netman-release) - we have deployed from `develop` as of September 28th, 2016

	**Note:** All issues we've encountered during installation and their respective solutions could be find [here](https://github.com/katrinsharp/akka-sample-cluster-on-cloudfoundry/blob/master/README.md).

**Enabling container-to-container networking**

By default, there is no network access between different containers. CloudFoundry provides sample application [Cats-and-Dogs](https://github.com/cloudfoundry-incubator/netman-release/tree/master/src/example-apps/cats-and-dogs) that contains instructions how to enable the access on per port basis.

**Deploying Akka application** 

You can deploy Akka application by using [java-buildpack](https://github.com/cloudfoundry/java-buildpack.git). Our sample application is inspired by [akka-sample-cluster](https://github.com/akka/akka/tree/master/akka-samples/akka-sample-cluster-scala)). It has backend nodes that calculate factorial upon receiving messages from frontend nodes. Frontend nodes also exposing HTTP interface `GET <frontend-hostname>/info` that shows number of jobs completed.

- Testing locally: 
	```
	cd akka-sample-cluster
	```
	
	+ Backends:

	```
	sbt backend:assembly
	java -jar target/scala-2.11/akka-sample-backend.jar 2551 
	java -jar target/scala-2.11/akka-sample-backend.jar 2552 #another CLI window
	```

	+ Frontend:
	```
	sbt frontend:assembly
	java -jar target/scala-2.11/akka-sample-frontend.jar
	```
	
	Check the number of completed factorial jobs
	```
	curl http://127.0.0.1:8080/info
	```

- Deploying to bosh-lite:

	**Background:** with Akka Cluster every node should know IPs/hostnames and ports of [cluster seed nodes](http://doc.akka.io/docs/akka/current/scala/cluster-usage.html#Joining_to_Seed_Nodes). Containers in Cloud Foundry have dynamic IPs making it impossible to manage list of static IPs for seed nodes. One possible way to bootstrap a cluster is when the first node joins itself and publishes its IP in some sort of shared registry that is accessable to the rest of nodes. More nodes could register themeselves as seed nodes later. Cloud Foundry doesn't have any type of Service Registry built-in. Some of the solutions could be to use [etcd](https://github.com/coreos/etcd) directly or [ConstructR](https://github.com/hseeberger/constructr) that utilizes etcd as Akka extension. In our guide we decided to use [amalgam8](https://github.com/amalgam8/amalgam8/tree/master/registry) and reason being that CloudFoundry provides an easy way to install that on bosh-lite. While it works for this guide, amalgam8 can't be used in production as is since simultaneous seed nodes registration with amalgam8 has high chances of forming multiple separated cluster.

	+ amalgam8 installation along with sample application can be found [here](https://github.com/cloudfoundry-incubator/netman-release/tree/develop/src/example-apps/tick).


	+ Installing Akka Backend: with `--no-route` and `--health-check-type none` options since backend doesn't expose any HTTP ports
	```
	cf push --no-route --health-check-type none sample-akka-cluster-backend -p target/scala-2.11/akka-sample-backend.jar -b https://github.com/cloudfoundry/java-buildpack.git
	cf access-allow sample-akka-cluster-backend sample-akka-cluster-backend --port 2551 --protocol tcp
	cf logs sample-akka-cluster-backend # check the log to see that first node joined itself
	cf scale sample-akka-cluster-backend -i 2 # needed since our sample application requires 2 backends, can be changed configuration
	```

	+ Installing Frontend:
	```
	cf push sample-akka-cluster-frontend -p target/scala-2.11/akka-sample-frontend.jar -b https://github.com/cloudfoundry/java-buildpack.git
	cf access-allow sample-akka-cluster-frontend sample-akka-cluster-backend --port 2551 --protocol tcp
	```

	+ Veryfing that it works:
	```
	curl GET sample-akka-cluster-frontend.bosh-lite.com/info #it should show the number of completed jobs
	```

## Summary

This guide is a prototype and it requires more than it presents to have a production Akka Cluster on CloudFoundry, but it is a successful proof-of-concept that it is doable with latest CloudFoundry release and new experimential network plugin.






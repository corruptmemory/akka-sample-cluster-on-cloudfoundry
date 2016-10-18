# Clean version of the repo

This repo contains sample Akka Cluster app integrated with amalgam8.

# Instructions for running sample locally:

- Run amalgam8:
	+ Pull amalgam image: 
	```
	docker pull amalgam8/a8-registry:0.1
	```
	More information about amalgam8 configuration [here](https://github.com/elevran/registry)
	+ Run docker image:
	```
	docker run -p 8080:8080 amalgam8/a8-registry:0.1 # the default port is 8080
	```

	**Note:** Check that it works by calling `curl http://192.168.99.100:8080/api/v1/instances`

- Run backend nodes:
```
java -jar target/scala-2.11/akka-sample-backend.jar 2551 local
java -jar target/scala-2.11/akka-sample-backend.jar 2552 local
```
- Run frontend node:
```
java -jar target/scala-2.11/akka-sample-frontend.jar 10 true local
```

**Note:** The `local` flag for both backend and frontend means that amalgam8 is assumed at `192.168.99.100:8080`, if omitted `registry.bosh-lite.com` will be used.

# Running on bosh-lite:

Follow instructions [here](http://developer.lightbend.com/guides/running-akka-cluster-on-cloudfoundry/), just checkout this branch instead of `master`. Should work.   

## Creating Docker images

```
docker build -t activator -f Dockerfile-activator .

docker build -t sources -f Dockerfile-sources .
```

`activator` is a base image for `sources`. It need to build only once, then evert time that sources change, rebuild `sources` only.


## Running locally in Docker on mac:

```
docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_THIS_PORT=2551" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" -p "2551:2551"  --name akka-sample-backend-1 sources ./bin/activator "runMain sample.cluster.factorial.FactorialBackend"

docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_THIS_PORT=2552" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" -p "2552:2552"  --name akka-sample-backend-2 sources ./bin/activator "runMain sample.cluster.factorial.FactorialBackend"

docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" --name akka-sample-frontend sources ./bin/activator "runMain sample.cluster.factorial.FactorialFrontend"
```

Substitute `192.168.99.100` with IP of your Docker machine (`docker-machine env`).

# IN PROGRESS:

1. CloudFoundry management machine(Dockerfile -> Docker image -> Docker container)  -- what else should installed?

To build Cloud Foundry image:

```
docker build -t cf -f Dockerfile-cf .
```

2. Akka application (2 Dockerfiles -> 2 images -> X containers)

3. How can I use images from 2. to push as CF applications.
  a. To push Akka Docker images to https://docs.docker.com/docker-hub/repos/ or other PUBLIC Docker repository.
  b. https://docs.pivotal.io/pivotalcf/1-7/concepts/docker.html - How to use Docker images to run CF application

4. Can we use CF somehow as sandox w/o going through AWS or another Cloud Provider route. Will it be considered as valid confirmation? 

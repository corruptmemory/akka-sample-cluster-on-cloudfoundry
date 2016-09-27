# Install bosh cli
```gem install bosh_cli --no-ri --no-rdoc```

# Install bosh lite:

## Install vagrant
https://www.vagrantup.com/downloads.html

## Clone bosh lite
```git clone https://github.com/cloudfoundry/bosh-lite```

## Install virtualbox
https://www.virtualbox.org/wiki/Downloads

## virtual box create
```cd bosh-lite```
```vagrant up --provider=virtualbox```

### Note: if you encounter `default: Warning: Authentication failure. Retrying...` that goes on forever, 
then use the following fix: fix the ssh bug (https://github.com/mitchellh/vagrant/issues/7610) and don't forget to restart the vagrant.

I followed it like this: 
```vagrant ssh``` # (default password: vagrant)

vagrant@agent-id-bosh-0:~$ ls -la ~/.ssh/authorized_keys
-rw-rw-r-- 1 vagrant vagrant 389 Sep 21 15:27 /home/vagrant/.ssh/authorized_keys
vagrant@agent-id-bosh-0:~$ chmod 0600 /home/vagrant/.ssh/authorized_keys
vagrant@agent-id-bosh-0:~$ ls -la ~/.ssh/authorized_keys
-rw------- 1 vagrant vagrant 389 Sep 21 15:27 /home/vagrant/.ssh/authorized_keys

vagrant suspend
vagrant up

## Login into bosh director
```bosh target 192.168.50.4 vagrant``` # credentials: admin/admin

## Adding routes
bin/add-route

# Install CF 
(https://github.com/cloudfoundry/bosh-lite/blob/master/README.md#deploy-cloud-foundry)

```cd .. # be at the same level as bosh-lite```
```git clone https://github.com/cloudfoundry/cf-release```
```./bin/provision_cf```

## It will complain about bundler
```gem install bundler```

```./bin/provision_cf```

## It will complain spiff
## spiff should be Darwin for Mac (https://github.com/cloudfoundry-incubator/spiff/releases)
```unzip spiff_darwin_amd64.zip```
```mkdir bin```
```mv spiff binvi ~/.bash_profile` # add `pwd`/bin to PATH
```source ~/.bash_profile```

## Here should be success
```./bin/provision_cf```

# Install CF CLI 
https://github.com/cloudfoundry/cli#downloads
```curl -L "https://cli.run.pivotal.io/stable?release=macosx64-binary&source=github" | tar -zx```
```mv cf bin/```


### Comment: check if cluster is running properly
```bosh cck cf-warden

# Build / deploy applications

## set cf api point
```cf api --skip-ssl-validation https://api.bosh-lite.com```

## cf login
```cf login``` # credentials: admin/admin

## Create and target org
```cf create-org lightbend```
```cf target -o lightbend```

## Create space
```cf create-space development```

## Target org and space
```cf target -o "lightbend" -s "development"```

## deploy non clustered akka app -- Optional
```cd akka-sample-cluster```
```sbt assembly```
```cf push sample-akka-non-cluster -p target/scala-2.11/akka-sample-cluster-assembly-0.1-SNAPSHOT.jar -b https://github.com/cloudfoundry/java-buildpack.git```

# Network plugin installation

## Install Go
```brew install go```

## Set env var for go build and go to network plugin folder -- CHANGE /Users/admin/projects/akka-sample-cluster-on-cloudfoundry-2 to where it really is
```export GOPATH=/Users/admin/projects/akka-sample-cluster-on-cloudfoundry-2/netman-release```
```cd netman-release/src/cli-plugin```

## Build and install plugin
```go build -o /tmp/network-policy-plugin```
```chmod +x /tmp/network-policy-plugin```
```cf install-plugin -f /tmp/network-policy-plugin```

## From bosh-lite folder
```vagrant ssh -c 'sudo modprobe br_netfilter'```

## From workspace folder
```curl -L -o bosh-lite-stemcell-latest.tgz https://bosh.io/d/stemcells/bosh-warden-boshlite-ubuntu-trusty-go_agent```
```bosh upload stemcell bosh-lite-stemcell-latest.tgz```


```git clone --recursive https://github.com/cloudfoundry/diego-release```
```git clone --recursive https://github.com/cloudfoundry/cf-release```
```git clone --recursive https://github.com/cloudfoundry-incubator/netman-release```

```git clone --recursive https://github.com/cloudfoundry/garden-runc-release```
```cd garden-runc-release```
```git checkout develop```
```git submodule update --init --recursive```

```bosh target vagrant && bosh create release && bosh upload release```

```vagrant up```

## set alias to lite so scripts will work
```bosh target 192.168.50.4 lite```

## set go variables
```export GOROOT=/usr/local/opt/go/libexec```
## not sure it us necessary `export GOPATH=/Users/admin/projects/akka-sample-cluster-on-cloudfoundry-2`

```cd diego-release```
```git checkout develop```
```git submodule update --init --recursive```

##  Deploy
```cd netman-release```
```./scripts/deploy-to-bosh-lite```

## Notes:
### If you get 'the master was dirty', check releases folders: diego, cf, netman and do
```git submodule update --init --recursive```

### If you get duplicate releases then you you need to remove `dev_releases` folder in `netman-release` and call 
```bosh delete release netman 0.2.0+dev.1` # it maybe 1 or 2 or whatever at the end

#### Optional: CF sample app
```cd netman-release/src/example-apps/cats-and-dogs/frontend```
```cf push frontend```
```cd netman-release/src/example-apps/cats-and-dogs/backend```
```cf push backend```
```cf set-env backend CATS_PORTS "5678,9876"```
```cf restage backend```
```cf access-allow frontend backend --port 9876 --protocol tcp``` 

# how to install amalgam8 on CF
https://github.com/cloudfoundry-incubator/netman-release/tree/develop/src/example-apps/tick

#cluster
```cd akka-sample-cluster```
```sbt backend:assembly```
```cf push --no-route target/scala-2.11/sample-akka-cluster-backend -p akka-sample-backend.jar -b https://github.com/cloudfoundry/java-buildpack.git```
```cf set-health-check sample-akka-cluster-backend none```
```cf access-allow sample-akka-cluster-backend sample-akka-cluster-backend --port 2551 --protocol tcp```
```cf scale sample-akka-cluster-backend -i 2```

```sbt frontend:assembly```
```cf push sample-akka-cluster-frontend -p target/scala-2.11/akka-sample-frontend.jar -b https://github.com/cloudfoundry/java-buildpack.git```
```cf access-allow sample-akka-cluster-frontend sample-akka-cluster-backend --port 2551 --protocol tcp```

nstall bosh cli
gem install bosh_cli --no-ri --no-rdoc

# install bosh lite:

## install vagrant
https://www.vagrantup.com/downloads.html

## clone bosh lite
git clone https://github.com/cloudfoundry/bosh-lite

## install virtualbox
https://www.virtualbox.org/wiki/Downloads

## virtual box create / fix the ssh bug (https://github.com/mitchellh/vagrant/issues/7610) / restart
cd bosh-lite

vagrant up --provider=virtualbox

vagrant ssh (default password: vagrant)

vagrant@agent-id-bosh-0:~$ ls -la ~/.ssh/authorized_keys
-rw-rw-r-- 1 vagrant vagrant 389 Sep 21 15:27 /home/vagrant/.ssh/authorized_keys
vagrant@agent-id-bosh-0:~$ chmod 0600 /home/vagrant/.ssh/authorized_keys
vagrant@agent-id-bosh-0:~$ ls -la ~/.ssh/authorized_keys
-rw------- 1 vagrant vagrant 389 Sep 21 15:27 /home/vagrant/.ssh/authorized_keys

vagrant suspend
vagrant up

## login into bosh director
> bosh target 192.168.50.4 vagrant
Target set to 'Bosh Lite Director'
Your username: admin
Enter password:
Logged in as 'admin'
Princewills-MacBook-Pro-2:akka-sample-cluster-on-cloudfoundry-2 Admin$

##adding routes
bin/add-route

# install CF (https://github.com/cloudfoundry/bosh-lite/blob/master/README.md#deploy-cloud-foundry)
cd .. # be at the same level as bosh-lite
git clone https://github.com/cloudfoundry/cf-release

./bin/provision_cf

## it will complain about bundler
gem install bundler

./bin/provision_cf

## it will complain spiff
## spiff should be Darwin for Mac (https://github.com/cloudfoundry-incubator/spiff/releases)
unzip spiff_darwin_amd64.zip
mkdir bin
mv spiff binvi ~/.bash_profile # add `pwd`/bin to PATH
source ~/.bash_profile

## here should be success
./bin/provision_cf

# install CF CLI https://github.com/cloudfoundry/cli#downloads
curl -L "https://cli.run.pivotal.io/stable?release=macosx64-binary&source=github" | tar -zx
mv cf bin/


### Comment: check if cluster is running properly
bosh cck cf-warden


## set cf api point
cf api --skip-ssl-validation https://api.bosh-lite.com

## cf login
Princewills-MacBook-Pro-2:akka-sample-cluster-on-cloudfoundry-2 Admin$ cf login
API endpoint: https://api.bosh-lite.com

Email> admin

Password>
Authenticating...
OK



API endpoint:   https://api.bosh-lite.com (API version: 2.62.0)
User:           admin
No org or space targeted, use 'cf target -o ORG -s SPACE'

## create and target org
cf create-org lightbend
cf target -o lightbend

## create space
cf create-space development

## target org and space
cf target -o "lightbend" -s "development"

## deploy non clustered akka app
cf push sample-akka-non-cluster -p akka-sample-cluster/target/scala-2.11/akka-sample-cluster-assembly-0.1-SNAPSHOT.jar -b https://github.com/cloudfoundry/java-buildpack.git

## install go
brew install go

## set env var for go build and go to network plugin folder
export GOPATH=/Users/admin/projects/akka-sample-cluster-on-cloudfoundry-2/netman-release
cd netman-release/src/cli-plugin

# build and install plugin
go build -o /tmp/network-policy-plugin
chmod +x /tmp/network-policy-plugin
cf install-plugin -f /tmp/network-policy-plugin

## from bosh-lite folder
vagrant ssh -c 'sudo modprobe br_netfilter'

## from workspace folder
curl -L -o bosh-lite-stemcell-latest.tgz https://bosh.io/d/stemcells/bosh-warden-boshlite-ubuntu-trusty-go_agent
bosh upload stemcell bosh-lite-stemcell-latest.tgz


git clone --recursive https://github.com/cloudfoundry/diego-release
git clone --recursive https://github.com/cloudfoundry/cf-release
git clone --recursive https://github.com/cloudfoundry-incubator/netman-release

git clone --recursive https://github.com/cloudfoundry/garden-runc-release
cd garden-runc-release
git checkout develop
git submodule update --init --recursive

bosh target vagrant && bosh create release && bosh upload release

vagrant up

## set alias to lite so scripts will work
bosh target 192.168.50.4 lite

## set go variables
export GOROOT=/usr/local/opt/go/libexec
export GOPATH=/Users/admin/projects/akka-sample-cluster-on-cloudfoundry-2   ---- ???

pushd ~/workspace/diego-release
   git checkout develop
  git submodule update --init --recursive
popd

##  deploy
cd netman-release
./scripts/deploy-to-bosh-lite

???
## somehow the master there was dirty
cd cf-release
git submodule update --init --recursive
???

bosh delete release netman 0.2.0+dev.2

#### Their sample app
cd netman-release/src/example-apps/cats-and-dogs/frontend
cf push frontend
cd netman-release/src/example-apps/cats-and-dogs/backend
cf push backend
cf set-env backend CATS_PORTS "5678,9876"
cf restage backend


# how to install amalgam8 on CF
https://github.com/cloudfoundry-incubator/netman-release/tree/develop/src/example-apps/tick

#cluster
cf push --no-route sample-akka-cluster-backend -p akka-sample-backend.jar -b https://github.com/cloudfoundry/java-buildpack.git
cf set-health-check sample-akka-cluster-backend none

cf access-allow sample-akka-cluster-backend sample-akka-cluster-backend --port 2551 --protocol tcp

cf scale sample-akka-cluster-backend -i 2


cf push sample-akka-cluster-frontend -p akka-sample-frontend.jar -b https://github.com/cloudfoundry/java-buildpack.git
cf access-allow sample-akka-cluster-frontend sample-akka-cluster-backend --port 2551 --protocol tcp

Add the following line in your (.bash|.zsh)rc file :
export GPG_NAME=<your_email>

docker build -t rpmbuilder .

docker stop vestigerpmbuilder && docker rm vestigerpmbuilder
docker run -v $HOME/docker/gnupg:/home/builder/.gnupg -v $HOME/docker/ssh:/home/builder/.ssh -d -p 2224:22 --name vestigerpmbuilder rpmbuilder

Edit .ssh/config :

Host rpmbuilder
Hostname 127.0.0.1
User builder
Port 2224
StrictHostKeyChecking no
UserKnownHostsFile=/dev/null


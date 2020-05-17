docker build -t debbuilder .

docker stop vestigedebbuilder && docker rm vestigedebbuilder
docker run -v $HOME/docker/gnupg:/home/builder/.gnupg -v $HOME/docker/ssh:/home/builder/.ssh -d -p 2223:22 --name vestigedebbuilder debbuilder

Edit .ssh/config :

Host debbuilder
Hostname 127.0.0.1
User builder
Port 2223
StrictHostKeyChecking no
UserKnownHostsFile=/dev/null


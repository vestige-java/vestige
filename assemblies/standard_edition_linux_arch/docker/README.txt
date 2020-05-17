docker build -t archbuilder .

docker stop vestigearchbuilder && docker rm vestigearchbuilder
docker run -v $HOME/docker/gnupg:/home/builder/.gnupg -v $HOME/docker/ssh:/home/builder/.ssh -d -p 2226:22 --name vestigearchbuilder archbuilder

Edit .ssh/config :

Host archbuilder
User builder
Hostname 127.0.0.1
Port 2226
StrictHostKeyChecking no
UserKnownHostsFile=/dev/null

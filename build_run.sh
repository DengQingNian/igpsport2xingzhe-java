mvn clean package
docker stop igpsport2xingzhe
docker rm igpsport2xingzhe
docker rmi igpsport2xingzhe:v1
docker build -t igpsport2xingzhe:v1 .
docker run -d --name igpsport2xingzhe -v /root/apps/igpsport2xingzhe-java/data:/data -p 18877:18877 igpsport2xingzhe:v1
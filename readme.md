## How to run Jaeger server
`docker run -d --name jaeger -p16686:16686 -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp -p5778:5778/tcp jaegertracing/all-in-one:latest`  
Open the Jaeger UI on [http://localhost:16686](http://localhost:16686)

## Local run
Just run app from IDE and open [http://localhost:8080](http://localhost:8080)

## Run in Docker

### How to build
`docker build --build-arg JAR_FILE=build/libs/*.jar -t io.github.mfvanek/spring_test .`

### How to run
`docker run --rm --name springDemoApp -p 8080:8080 io.github.mfvanek/spring_test`  
And open [http://localhost:8080](http://localhost:8080)



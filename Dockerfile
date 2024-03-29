FROM clojure:tools-deps AS BASE

RUN apt-get update && apt-get install -y zip

WORKDIR /concourse-pcf-foundation-resource

RUN curl -LO https://github.com/pivotal-cf/om/releases/download/4.1.0/om-linux-4.1.0

ADD scripts scripts
ADD src src
ADD resources resources
ADD test test
ADD deps.edn .

RUN ./scripts/test.sh
RUN ./scripts/compile.sh

FROM openjdk:11-jre-slim

COPY --from=BASE /concourse-pcf-foundation-resource/om-linux-4.1.0 /usr/local/bin/om
COPY --from=BASE /concourse-pcf-foundation-resource/target/concourse-pcf-foundation-resource.jar /

RUN chmod +x /usr/local/bin/om

ADD opt-resource /opt/resource

CMD ["/opt/resource/run"]

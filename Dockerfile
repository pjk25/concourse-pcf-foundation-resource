FROM oracle/graalvm-ce:19.0.0 AS BASE

RUN gu install native-image

RUN curl -O https://download.clojure.org/install/linux-install-1.10.0.442.sh
RUN chmod +x linux-install-1.10.0.442.sh
RUN ./linux-install-1.10.0.442.sh

RUN curl -LO https://github.com/pivotal-cf/om/releases/download/1.0.0/om-linux

ADD scripts scripts
ADD src src
ADD resources resources
ADD test test
ADD deps.edn .

RUN ./scripts/test.sh
RUN ./scripts/compile.sh

FROM alpine

RUN apk add --update ca-certificates

COPY --from=BASE /om-linux /usr/local/bin/om
COPY --from=BASE /concourse-pcf-foundation-resource /usr/local/bin

RUN chmod +x /usr/local/bin/om

ADD opt-resource /opt/resource

CMD ["concourse-pcf-foundation-resource"]

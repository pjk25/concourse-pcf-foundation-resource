FROM oracle/graalvm-ce:1.0.0-rc15 AS BASE

RUN curl -O https://download.clojure.org/install/linux-install-1.10.0.442.sh
RUN chmod +x linux-install-1.10.0.442.sh
RUN ./linux-install-1.10.0.442.sh

ENV GRAALVM_HOME /opt/graalvm-ce-1.0.0-rc15/

ADD scripts scripts
ADD src src
ADD test test
ADD deps.edn .

RUN ./scripts/test.sh
RUN ./scripts/compile.sh

FROM scratch
COPY --from=BASE /concourse-pcf-foundation-resource /

CMD ["/concourse-pcf-foundation-resource"]
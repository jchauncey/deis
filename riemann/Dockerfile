FROM frolvlad/alpine-oraclejdk8

RUN apk -U add curl bash
WORKDIR /home/riemann

RUN curl -o riemann.tar.bz2 https://aphyr.com/riemann/riemann-0.2.9.tar.bz2 && \
    tar -xj -f riemann.tar.bz2

# install etcdctl
RUN curl -sSL -o /usr/local/bin/etcdctl https://s3-us-west-2.amazonaws.com/get-deis/etcdctl-v0.4.9 \
    && chmod +x /usr/local/bin/etcdctl

# install confd
RUN curl -sSL -o /usr/local/bin/confd https://github.com/kelseyhightower/confd/releases/download/v0.10.0/confd-0.10.0-linux-amd64 \
    && chmod +x /usr/local/bin/confd

RUN adduser -D riemann
USER riemann

ADD riemann.config /home/riemann/
ADD /config /home/riemann/config
ADD bin/ /home/riemann/bin

## 5555 - Riemann TCP 
## 5556 - Riemann UDP
EXPOSE 5555/tcp
EXPOSE 5556/udp

CMD ["/home/riemann/bin/boot"]


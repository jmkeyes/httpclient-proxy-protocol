Apache HttpClient PROXY Protocol Shim
=====================================

Provides a `ConnectionSocketFactory` that injects a PROXY protocol header (v1) when opening a connection.

Getting Started
----------------

Add the dependency:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.jmkeyes</groupId>
        <artifactId>httpclient-proxy-protocol</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Register the `ProxyProtocolConnectionSocketFactory`:

```java
Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", new ProxyProtocolConnectionSocketFactory(new PlainConnectionSocketFactory())
        .register("https", new ProxyProtocolConnectionSocketFactory(new SSLConnectionSocketFactory(sslContext)));
        .build();

PoolingHttpClientConnectionManager connectionManager = new PoolingHttpCLientConnectionManager(registry);

HttpClient client = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .build();
```

Now whenever `client` creates a new connection it will send the PROXY header automatically.

Contributing
------------

  1. Clone this repository.
  2. Create your branch: `git checkout -b feature/branch`
  3. Commit your changes: `git commit -am "I am developer."`
  4. Push your changes: `git push origin feature/branch`
  5. Create a PR of your branch against the `master` branch.

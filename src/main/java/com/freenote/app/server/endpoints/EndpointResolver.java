package com.freenote.app.server.endpoints;

@FunctionalInterface
public interface EndpointResolver {
    URIEndpointHandler resolve(String path);
}

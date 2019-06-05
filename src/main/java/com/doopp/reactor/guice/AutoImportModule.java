package com.doopp.reactor.guice;

import com.doopp.reactor.guice.annotation.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

class AutoImportModule extends AbstractModule {

    @Override
    public void configure() {
        for(String className : ReactorGuiceServer.classNames) {
            this.binder(className);
        }
    }

    private <T> void binder(String className) {
        try {
            Class<T> clazz = (Class<T>) Class.forName(className);
            if (clazz.isAnnotationPresent(Service.class) && clazz.getInterfaces().length>=1) {
                Class<T> serviceInterface = (Class<T>) clazz.getInterfaces()[0];
                bind(serviceInterface).to(clazz).in(Scopes.SINGLETON);
            }
        }
        catch(Exception ignored) {}
    }
}

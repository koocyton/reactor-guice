package com.doopp.reactor.guice.test.entity;

import com.doopp.reactor.guice.view.ModelMap;
import reactor.netty.http.server.HttpServerRequest;

import javax.ws.rs.FormParam;

public class User {

    private Long id;

    private String account;

    private String password;

    private String name;

    @FormParam("id")
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @FormParam("name")
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @FormParam("account")
    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    @FormParam("password")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}

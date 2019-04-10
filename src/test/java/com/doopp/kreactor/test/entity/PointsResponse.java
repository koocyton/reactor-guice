package com.doopp.kreactor.test.entity;

import java.util.List;

public class PointsResponse {

    private String status;
    private String count;
    private String info;
    private String infocode;
    private List<Point> pois;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfocode() {
        return infocode;
    }

    public void setInfocode(String infocode) {
        this.infocode = infocode;
    }

    public List<Point> getPois() {
        return pois;
    }

    public void setPois(List<Point> pois) {
        this.pois = pois;
    }
}

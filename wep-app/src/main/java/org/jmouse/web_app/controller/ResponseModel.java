package org.jmouse.web_app.controller;

public class ResponseModel {

    private Object data;

    public ResponseModel(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}

package ecs.exception;

/**
 * 自定义异常
 * @author linjb
 * @daten 2020-08-16
 */

public class ServerException extends Exception {

    private Integer code;
    private String message;

    public ServerException(String message){
        super(message);
        this.code = -1;
    }

    public ServerException(Integer code, String message){
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

}

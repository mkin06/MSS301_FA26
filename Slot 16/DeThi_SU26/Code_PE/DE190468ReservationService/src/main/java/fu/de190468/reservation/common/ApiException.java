package fu.de190468.reservation.common;

public class ApiException extends RuntimeException {
  private final int http;
  private final int status;

  public ApiException(int http, int status, String message) {
    super(message);
    this.http = http;
    this.status = status;
  }

  public int getHttp() {
    return http;
  }

  public int getStatus() {
    return status;
  }

  public static ApiException invalid(String message) {
    return new ApiException(406, 2, message);
  }

  public static ApiException missing(String message) {
    return new ApiException(404, 4, message);
  }
}

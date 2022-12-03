package com.lyft.data.gateway.ha;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestGatewayHaSingleBackend {
  public static final String EXPECTED_RESPONSE = "{\"id\":\"testId\"}";
  int backendPort = 20000 + (int) (Math.random() * 1000);
  int routerPort = 21000 + (int) (Math.random() * 1000);

  private WireMockServer backend =
      new WireMockServer(WireMockConfiguration.options().port(backendPort));
  private final OkHttpClient httpClient = new OkHttpClient();

//  @BeforeClass(alwaysRun = true)
  public void setup() throws Exception {
    HaGatewayTestUtils.prepareMockBackend(backend, "/v1/statement", EXPECTED_RESPONSE);

    // seed database
    HaGatewayTestUtils.TestConfig testConfig =
        HaGatewayTestUtils.buildGatewayConfigAndSeedDb(routerPort);
    // Start Gateway
    String[] args = {"server", testConfig.getConfigFilePath()};
    HaGatewayLauncher.main(args);
    // Now populate the backend
    HaGatewayTestUtils.setUpBackend(
        "presto1", "http://localhost:" + backendPort, "externalUrl", true, "adhoc", routerPort);
  }

  @Test
  public void testRequestDelivery() throws Exception {
    RequestBody requestBody =
        RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "SELECT 1");
    Request request =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/v1/statement")
            .post(requestBody)
            .build();
    Response response = httpClient.newCall(request).execute();
    Assert.assertEquals(EXPECTED_RESPONSE, response.body().string());
  }

  @Test
  public void testLoginTrino() {
    String url = "http://localhost:9090/ui/login";
    Map auth = new HashMap<>();
    auth.put("username", "admin_rw");
    HttpResponse response = HttpRequest.post(url)
            .form(auth)//表单内容
            .timeout(20000)
            .execute();
    Map<String, List<String>> headers = response.headers();
    String str = headers.get("Set-Cookie").get(0);
    String token = str.split(";")[0];
    System.out.println(token);
    Assert.assertTrue(ObjectUtil.isNotEmpty(token));
    response = HttpRequest.get("http://localhost:9090/ui/api/stats")
            .cookie(token)
            .timeout(2000)
            .execute();
    String body = response.body();
    System.out.println(body);
  }

  @AfterClass(alwaysRun = true)
  public void cleanup() {
    backend.stop();
  }
}
